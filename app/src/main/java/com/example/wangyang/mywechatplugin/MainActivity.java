package com.example.wangyang.mywechatplugin;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

public class MainActivity extends AppCompatActivity {
    private Button open;
    private ImageView imageViewA;
    private ImageView imageViewB;
    public static boolean isA = true;
    public static boolean isB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        open = findViewById(R.id.open);
        imageViewA = findViewById(R.id.lucky_money_A);
        imageViewB = findViewById(R.id.lucky_money_B);
        open.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(intent);
            }
        });
        imageViewA.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isA) {
                    isA = true;
                    isB = false;
                    imageViewA.setImageResource(R.drawable.switch_on);
                    imageViewB.setImageResource(R.drawable.switch_off);
                }
            }
        });
        imageViewB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isB) {
                    isB = true;
                    isA = false;
                    imageViewA.setImageResource(R.drawable.switch_off);
                    imageViewB.setImageResource(R.drawable.switch_on);
                }
            }
        });
    }
}
