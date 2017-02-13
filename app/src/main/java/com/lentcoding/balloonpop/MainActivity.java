package com.lentcoding.balloonpop;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().setBackgroundDrawableResource(R.drawable.modern_background);
    }

    public void onClick(View view) {
        Intent i = new Intent(MainActivity.this, GameActivity.class);
        i.putExtra("mute", false);
        startActivity(i);
    }

    public void onClickMute(View view) {
        Intent i = new Intent(MainActivity.this, GameActivity.class);
        i.putExtra("mute", true);
        startActivity(i);
    }
}
