package com.example.kelvin.campuspathways;

import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

/**
 * Created by Kelvin on 2/25/2018.
 * Used to asynchronously select data from the database
 */

public class DatabaseConnectionSelect extends AsyncTask<String, Void, String> {

    private String query;//Query to be performed
    private GoogleMap googleMap;//Map to be used for drawing of paths

    private ArrayList<String> paths;//List of paths to be drawn

    //Only constructor
    DatabaseConnectionSelect(String query, GoogleMap gMap) {
        this.query = query;
        this.googleMap = gMap;

        paths = new ArrayList<>();
    }

    //Connect to database and perform query
    @Override
    protected String doInBackground(String... strings) {

        try {

            //Connection information
            String dns = "on-campus-navigation.caqb3uzoiuo3.us-east-1.rds.amazonaws.com";
            String aClass = "net.sourceforge.jtds.jdbc.Driver";
            Class.forName(aClass).newInstance();

            //Connect to database
            Connection dbConnection = DriverManager.getConnection("jdbc:jtds:sqlserver://" + dns +
                    "/Campus-Navigation;user=Android;password=password");

            //Execute query; In this case Selection
            Statement statement = dbConnection.createStatement();
            ResultSet resultSet = statement.executeQuery(query);

            //Iterate through result and put in ArrayList
            while (resultSet.next()) {
                //Assume that there are 2 columns
                //Column 1: ID && Column 2: Path JSON
                paths.add(resultSet.getString(2));
            }

            //Close connection to database
            dbConnection.close();

        } catch (Exception e) {
            Log.w("Error", "" + e.getMessage());
            return null;
        }

        return null;
    }

    @Override
    protected void onPostExecute(String result) {

        //Iterate through List of JSON arrays
        //Each JSON array is 1 pathway
        try {
            for (int i = 0; i < paths.size(); i++) {

                //Decode JSON array into polyline
                JSONArray pathJSON = new JSONArray(paths.get(i));

                ArrayList<LatLng> points = new ArrayList<>();
                //Get JSON array into list of points
                for (int j = 0; j < pathJSON.length(); j++) {
                    //Get data from JSON object
                    JSONObject point = pathJSON.getJSONObject(j);
                    double lat = point.getDouble("Latitude");
                    double lng = point.getDouble("Longitude");

                    //Make point from JSON data and add to list
                    points.add(new LatLng(lat, lng));
                }

                //Draw pathways in different colors
                if (i % 2 == 0)
                    googleMap.addPolyline(new PolylineOptions().addAll(points).width(10).color(Color.RED));
                else
                    googleMap.addPolyline(new PolylineOptions().addAll(points).width(10).color(Color.BLUE));

                LatLng pos = points.get(points.size() - 1);
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 20.0f));
                googleMap.addMarker(new MarkerOptions().position(pos).title("Pathway #" + (i + 1)));

            }

        } catch (Exception e) {
            Log.w("Error", "" + e.getMessage());
        }

    }
}
