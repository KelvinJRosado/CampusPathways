package com.example.kelvin.campuspathways;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class StartActivity extends AppCompatActivity {

    //UI Elements
    Button btDiscover, btDisplay;
    Context thisContext;//Used when switching activities

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        thisContext = this;

        init();
    }

    //Initialize UI elements and event listeners
    public void init(){
        btDisplay = findViewById(R.id.btShowMap);
        btDiscover = findViewById(R.id.btTrackPath);

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

}
