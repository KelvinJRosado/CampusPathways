package com.example.kelvin.campuspathways;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class DisplayActivity extends FragmentActivity implements OnMapReadyCallback {

    //UI Elements
    Button btDiscover;
    Context thisContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        init();
        thisContext = this;

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {

        //Once map loads, plot all paths
        new DatabaseConnectionSelect("Select * FROM My_Test_Table", googleMap).execute();

    }

    public void init(){
        btDiscover = findViewById(R.id.btDiscoverPathFromDisplay);

        //Change to Discover Activity
        btDiscover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //Check if given location access first; If not, tell user
                if (!getPermissions()) {
                    Toast.makeText(thisContext, "Error. Location access not granted", Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent intent = new Intent(thisContext, DiscoverActivity.class);
                startActivity(intent);
            }
        });

    }

    //Asks User for runtime permission to access location
    //Required for discovery
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

}
