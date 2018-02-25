package com.example.kelvin.campuspathways;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.maps.android.SphericalUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static java.lang.String.format;

public class DiscoverActivity extends AppCompatActivity implements SensorEventListener {

    /*
    References
    [1] http://www.codingforandroid.com/2011/01/using-orientation-sensors-simple.html
    [2] https://www.built.io/blog/applying-low-pass-filter-to-android-sensor-s-readings
    */

    Context thisContext;
    long currentTime, nextTime;
    //UI Elements
    private SeekBar seekBarHeight;
    private TextView tvHeight, tvDiscoverStatus;
    private Button btPathControl, btDisplayPaths;
    //Sensors
    private FusedLocationProviderClient fusedLocationProviderClient;//Gets starting location
    private SensorManager sensorManager;
    private Sensor stepSensor;
    //Gravity and rotation info; Used for calculating orientation
    private Sensor accelSensor, magnetSensor, gyroSensor;
    private float[] lastAccel = new float[3];
    private float[] lastMagnet = new float[3];
    private boolean accelSet = false, magnetSet = false;
    private float[] rotation = new float[9];
    private float[] orientation = new float[3];
    private float currentAngle = 0f;
    private double zGyro;
    private double zGyroTotal;
    private boolean getCompass = false;
    private double currentDirection;
    private int sensorChanged;
    private GeomagneticField geomagneticField;
    //List of points in user path
    private ArrayList<TimedLocation> userPath;

    private int userHeightInches = 48;//User height; Used in step length calculation
    private boolean tracking = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discover);
        thisContext = this;

        initObjects();

    }

    //Initialize elements
    public void initObjects() {

        //Path buffer
        userPath = new ArrayList<>();

        //UI Elements
        seekBarHeight = findViewById(R.id.seekbarUserHeight);
        tvHeight = findViewById(R.id.tvHeight);
        btPathControl = findViewById(R.id.btPathControl);
        btDisplayPaths = findViewById(R.id.btDisplayPathFromDiscover);
        tvDiscoverStatus = findViewById(R.id.tvDiscoverStatus);

        //Sensors
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        assert sensorManager != null;//Assume phone has necessary sensors
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        magnetSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        //Set up location client
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        //Event listener for height slider
        seekBarHeight.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

                int in = 48 + i;
                userHeightInches = in;//Update global var

                @SuppressLint("DefaultLocale") String ss = "Your Height: " + in + " in / " + in / 12 + " ft " + in % 12 +
                        " in / " + format("%2.2f", (in * 2.54)) + " cm";

                tvHeight.setText(ss);//Update text view

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //Do nothing
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //Do nothing
            }
        });

        //Start and stop tracking
        btPathControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //Not currently tracking; Start
                if (btPathControl.getText().toString().startsWith("Start")) {

                    if (ActivityCompat.checkSelfPermission(thisContext, Manifest.permission.ACCESS_FINE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {

                        boolean perms = getPermissions();

                        if (!perms) {
                            Toast.makeText(thisContext, "Error. Location access not granted", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    fusedLocationProviderClient.getLastLocation().addOnSuccessListener(DiscoverActivity.this,
                            new OnSuccessListener<Location>() {

                                @Override
                                public void onSuccess(Location location) {
                                    if (location != null) {

                                        //Get starting location
                                        LatLng start = new LatLng(location.getLatitude(), location.getLongitude());

                                        //Add starting location to List
                                        userPath.add(new TimedLocation(start));

                                        //Get declination for finding true north
                                        geomagneticField = new GeomagneticField((float) location.getLatitude(),
                                                (float) location.getLatitude(), (float) location.getAltitude(),
                                                System.currentTimeMillis());

                                    } else {
                                        Toast.makeText(thisContext,
                                                "Error. Location not found. Make sure Location is enabled on your phone",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                }

                            });

                    //Update button text and boolean
                    btPathControl.setText(R.string.stopPathDisc);
                    tracking = true;
                }

                //Currently tracking; Stop
                else{

                    //Serialize list of points
                    ArrayList<JSONObject> pathTemp = new ArrayList<>();

                    for (int i = 0; i < userPath.size(); i += 2) {
                        try {
                            JSONObject temp = new JSONObject();
                            temp.put("Latitude", userPath.get(i).getLocation().latitude);
                            temp.put("Longitude", userPath.get(i).getLocation().longitude);
                            temp.put("Time", userPath.get(i).getTimestamp());

                            pathTemp.add(temp);

                        } catch (JSONException e) {
                            //Exit on JSON error
                            e.printStackTrace();
                            Toast.makeText(thisContext, e.getMessage(), Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                    //Convert ArrayList into JSON Array
                    JSONArray pathJSON = new JSONArray(pathTemp);

                    //Build query
                    String st = "'" + pathJSON.toString() + "'";

                    String query = "INSERT INTO My_Test_Table (User_Path)" +
                            " VALUES (" + st + ");";

                    //Send to database
                    new DatabaseConnectionInsert(query).execute();

                    //Reset buffer
                    userPath.clear();

                    //Notify user
                    String ss = "Path sent to server";
                    Toast.makeText(thisContext, ss, Toast.LENGTH_SHORT).show();

                    //Update button text and boolean
                    btPathControl.setText(R.string.startPathDisc);
                    tracking = false;

                }

            }
        });

        //Change to pathway display
        btDisplayPaths.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(thisContext, DisplayActivity.class);
                startActivity(intent);
            }
        });

    }

    //Filters sensor data to improve accuracy
    //Based on code from [2]
    protected float[] filter(float[] in, float[] out) {

        final float ALPHA = (float) 0.25;//Filtering constant

        if (out == null) return in;

        for (int i = 0; i < in.length; i++) {
            out[i] = out[i] + (ALPHA * (in[i] - out[i]));
        }

        return out;

    }

    //Asks User for runtime permission to access location
    public boolean getPermissions() {

        //Check if permission granted
        if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            //If not already granted, prompt user for them
            ActivityCompat.requestPermissions(this, new String[]{ACCESS_FINE_LOCATION},
                    PackageManager.PERMISSION_GRANTED);

            return false;

        }

        //If permission already granted
        else {
            return true;
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onSensorChanged(SensorEvent event) {

        //First check if tracking mode is enabled; Return if not
        if (!tracking) return;

        //This will help up get direction the phone is pointing
        sensorChanged++;    //This will keep track of how many times the sensor has changed
        //Accel sensor
        if (event.sensor == accelSensor) {
            lastAccel = filter(event.values.clone(), lastAccel);
            accelSet = true;
        }

        //Magnet sensor
        else if (event.sensor == magnetSensor) {
            lastMagnet = filter(event.values.clone(), lastMagnet);
            magnetSet = true;
        }

        //The phone will have 30 tries to find the direction the phone is pointing
        if (sensorChanged < 30 || getCompass) {
            if (accelSet && magnetSet && geomagneticField != null && getCompass) {
                for (int i = 0; i < 5; i++) {
                    SensorManager.getRotationMatrix(rotation, null, lastAccel, lastMagnet);
                    SensorManager.getOrientation(rotation, orientation);

                    float azimuthRadians = orientation[0];
                    currentAngle = ((float) (Math.toDegrees(azimuthRadians) + 360) % 360) - geomagneticField.getDeclination();
                    currentDirection = currentAngle;

                    Log.d("direction", "init: " + currentDirection); //For debugging purposes
                }
                getCompass = false;
            } else if (accelSet && magnetSet && geomagneticField != null) {
                SensorManager.getRotationMatrix(rotation, null, lastAccel, lastMagnet);
                SensorManager.getOrientation(rotation, orientation);

                float azimuthRadians = orientation[0];
                currentAngle = ((float) (Math.toDegrees(azimuthRadians) + 360) % 360) - geomagneticField.getDeclination();
                currentDirection = currentAngle;
            }

        }

        //double readings = event.values[2];

        if (event.sensor == gyroSensor) {
            if (event.values[2] <= -0.06 || event.values[2] >= 0.06) {  //Filter out these results
                zGyro = (event.values[2] / 5.89111) * -(180.0 / Math.PI); //The smaller the number, the more sensitive the results are
                currentDirection += zGyro;
                zGyroTotal += zGyro;    //Stores how much movement the gyroscopes have detected
                if ((45 % zGyroTotal) == 45) {  //If the gyroscopes have moved more than 45 degrees
                    zGyroTotal = 0;             //We check the direction using the magnet meter
                    getCompass = true;          //Allows us to check the magnet meter
                    //Log.d("Print", "onSensorChanged: zGyro: " + zGyroTotal + " zGyroMod: " + (60 % zGyroTotal));  //For testing purposes
                }

            }
        }

        //If event is a step
        if (event.sensor == stepSensor && userPath.size() >= 1) {

            //Calculate current step length, in meters
            double stepLength = userHeightInches * 0.0254;

            LatLng lastLocation = userPath.get(userPath.size() - 1).getLocation();

            //Calculate new LatLng
            LatLng currentPos = SphericalUtil.computeOffset(lastLocation, stepLength, currentDirection);

            tvDiscoverStatus.setText("Lat: " + currentPos.latitude + ", Lng: " + currentPos.longitude);

            userPath.add(new TimedLocation(currentPos));

        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        //Do nothing
    }

    protected void onResume() {
        super.onResume();
        //Register sensor listeners
        sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, magnetSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_NORMAL);

        //Reset tracking boolean
        tracking = !btPathControl.getText().toString().startsWith("Start");

    }

    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this, stepSensor);
        sensorManager.unregisterListener(this, accelSensor);
        sensorManager.unregisterListener(this, magnetSensor);
        sensorManager.unregisterListener(this, gyroSensor);
    }


}
