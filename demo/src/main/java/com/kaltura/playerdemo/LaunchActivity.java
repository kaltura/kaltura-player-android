package com.kaltura.playerdemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.kaltura.playkit.player.PKHttpClientManager;

public class LaunchActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);
        doConnectionsWarmup();

        findViewById(R.id.btn_basic).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LaunchActivity.this, BasicDemoActivity.class));
            }
        });

        findViewById(R.id.btn_ovp).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LaunchActivity.this, OVPDemoActivity.class));
            }
        });

        findViewById(R.id.btn_ott).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LaunchActivity.this, OTTDemoActivity.class));
            }
        });
    }

    private void doConnectionsWarmup() {
        PKHttpClientManager.setHttpProvider("okhttp");
        PKHttpClientManager.warmUp(
                "https://rest-as.ott.kaltura.com/crossdomain.xml",
                "https://api-preprod.ott.kaltura.com/crossdomain.xml",
                "https://vootvideo.akamaized.net/favicon.ico",
                "https://cdnapisec.kaltura.com/alive.html",
                "https://cfvod.kaltura.com/alive.html"
        );
    }
}
