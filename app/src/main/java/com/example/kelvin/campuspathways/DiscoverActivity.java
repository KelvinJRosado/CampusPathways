package com.example.kelvin.campuspathways;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import static java.lang.String.format;

public class DiscoverActivity extends AppCompatActivity {

    Context thisContext;
    //UI Elements
    private SeekBar seekBarHeight;
    private TextView tvHeight;
    private Button btPathControl, btDisplayPaths;
    private int userHeightInches = 48;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discover);
        thisContext = this;

        init();
    }

    //Initialize elements
    public void init(){
        seekBarHeight = findViewById(R.id.seekbarUserHeight);
        tvHeight = findViewById(R.id.tvHeight);
        btPathControl = findViewById(R.id.btPathControl);
        btDisplayPaths = findViewById(R.id.btDisplayPathFromDiscover);

        //Event listener for height slider
        seekBarHeight.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

                int in = 48 + i;
                userHeightInches = in;//Update global var

                @SuppressLint("DefaultLocale") String ss = "Your Height: " + in + " in / " + in / 12 + " ft " + in % 12 +
                        " in / " + format("%2.2f",(in * 2.54)) + " cm";

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

                //Currently tracking
                if(btPathControl.getText().toString().startsWith("Start")){

                    //Update button text
                    btPathControl.setText(R.string.stopPathDisc);

                }

                //Not tracking
                else{

                    //Update button text
                    btPathControl.setText(R.string.startPathDisc);

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

}
