package com.example.kelvin.campuspathways;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

public class NodeSelectionActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Button btDisplay, btDiscover;
    private Context thisContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_node_selection);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        thisContext = this;
        init();
    }

    //Initialize objects
    private void init() {
        btDiscover = findViewById(R.id.btDiscoverFromNodes);
        btDisplay = findViewById(R.id.btDisplayFromNodes);

        btDisplay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(thisContext, DisplayActivity.class);
                startActivity(intent);
            }
        });

        btDiscover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(thisContext, DiscoverActivity.class);
                startActivity(intent);
            }
        });

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        new DatabaseConnectionCreateNodes(mMap).execute();

    }


}
