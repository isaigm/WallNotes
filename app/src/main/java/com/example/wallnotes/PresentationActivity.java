package com.example.wallnotes;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.ViewFlipper;

import java.util.concurrent.atomic.AtomicInteger;

public class PresentationActivity extends AppCompatActivity {
    private ViewFlipper viewFlipper;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_presentation);
        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        viewFlipper = findViewById(R.id.viewFlipper);
        RadioButton rb1 = findViewById(R.id.rb1);
        RadioButton rb2 = findViewById(R.id.rb2);
        RadioButton rb3 = findViewById(R.id.rb3);
        Button start = findViewById(R.id.start);
        rb1.setEnabled(false);
        rb2.setEnabled(false);
        rb3.setEnabled(false);
        rb1.setChecked(true);
        AtomicInteger count = new AtomicInteger(0);
        start.setOnClickListener(l -> {
            if(count.get() < 2){
                if(count.get() == 0){
                    viewFlipper.setDisplayedChild(viewFlipper.indexOfChild(findViewById(R.id.vista2)));
                    rb1.setChecked(false);
                    rb2.setChecked(true);
                }else{
                    viewFlipper.setDisplayedChild(viewFlipper.indexOfChild(findViewById(R.id.vista3)));
                    rb2.setChecked(false);
                    rb3.setChecked(true);
                }
                count.getAndIncrement();
            }else{
                SharedPreferences details = getSharedPreferences("details", MODE_PRIVATE);
                SharedPreferences.Editor editor = details.edit();
                editor.putBoolean("first_time", true);
                editor.apply();
                startActivity(new Intent(PresentationActivity.this, MainActivity.class));
                finish();
            }
        });
    }
}