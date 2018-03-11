package com.example.kelvin.campuspathways;

import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

/**
 * Created by Kelvin on 3/11/2018.
 * <p>
 * Queries database to get path information
 * Extracts start and end point of each path
 * Groups points together to create nodes
 */

public class DatabaseConnectionCreateNodes extends AsyncTask<Void, Void, Void> {

    //Max distance for points to be in same node, in meters
    private final double maxNodeDistance = 30.0;
    //List of paths
    private ArrayList<LatLng> points;
    //List of node groupings
    private ArrayList<ArrayList> nodes;

    //Constructor
    DatabaseConnectionCreateNodes() {

        //Initialize objects
        points = new ArrayList<>();
        nodes = new ArrayList<>();

    }

    @Override
    protected Void doInBackground(Void... voids) {

        //Connect to database to get path information
        try {

            //Connection information
            String dns = "on-campus-navigation.caqb3uzoiuo3.us-east-1.rds.amazonaws.com";
            String aClass = "net.sourceforge.jtds.jdbc.Driver";
            Class.forName(aClass).newInstance();

            //Connect to database
            Connection dbConnection = DriverManager.getConnection("jdbc:jtds:sqlserver://" + dns +
                    "/Campus-Navigation;user=Android;password=password");

            //Create query
            String query = "SELECT * FROM Pathways";

            //Execute query; In this case Selection
            Statement statement = dbConnection.createStatement();
            ResultSet resultSet = statement.executeQuery(query);

            //Iterate through result and put in ArrayList
            while (resultSet.next()) {

                //Get path JSON
                String pathJSONString = resultSet.getString("User_Path");
                JSONArray pathJSON = new JSONArray(pathJSONString);

                //Get JSON of start and end points
                JSONObject startPointJSON = pathJSON.getJSONObject(0);
                JSONObject endPointJSON = pathJSON.getJSONObject(pathJSON.length() - 1);

                //Get LatLng of start and end points
                LatLng startPoint =
                        new LatLng(startPointJSON.getDouble("Latitude"), startPointJSON.getDouble("Longitude"));
                LatLng endPoint =
                        new LatLng(endPointJSON.getDouble("Latitude"), endPointJSON.getDouble("Longitude"));

                //Insert points into ArrayList
                points.add(startPoint);
                points.add(endPoint);

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
    protected void onPostExecute(Void aVoid) {

        super.onPostExecute(aVoid);

        //Create the nodes
        createNodes();

    }

    //Recursive method to create node groupings
    private void createNodes() {

        //Base case; End
        if (points.isEmpty()) return;

        //ArrayList of points in node
        ArrayList<LatLng> node = new ArrayList<>();

        //Get first point in ArrayList
        LatLng startPoint = points.get(0);
        node.add(startPoint);

        //Second base case; Save last point and return
        if (points.size() == 1) {
            nodes.add(node);
            return;
        }

        //Get distance to every other point in ArrayList

        //Iterate though points in Array List to get distances
        for (int i = 1; i < points.size(); i++) {

            //Calculate distance between first point and current point
            LatLng point = points.get(i);
            double distance = SphericalUtil.computeDistanceBetween(startPoint, point);

            //Add to node List if close enough
            if (distance <= maxNodeDistance) {
                node.add(point);
            }

        }

        //Add node to list of nodes
        nodes.add(node);

        //Remove points in node from overall list
        points.removeAll(node);

        //Recurse
        createNodes();

    }

}
