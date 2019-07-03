package com.kaltura.playerdemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.kaltura.playkit.player.PKHttpClientManager;
import com.kaltura.tvplayer.KalturaPlayer;

public class LaunchActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);
        KalturaPlayer.initializeOTT(this, 3009, "https://rest-us.ott.kaltura.com/v4_5/");
        KalturaPlayer.initializeOVP(this, 2215841, "https://cdnapisec.kaltura.com/");
        KalturaPlayer.initializeOVP(this, 2222401, "https://cdnapisec.kaltura.com/");
        KalturaPlayer.initializeOVP(this, 1091, "http://qa-apache-php7.dev.kaltura.com/");
        doConnectionsWarmup();

        findViewById(R.id.btn_basic).setOnClickListener(v -> startActivity(new Intent(LaunchActivity.this, BasicDemoActivity.class)));

        findViewById(R.id.btn_ovp).setOnClickListener(v -> startActivity(new Intent(LaunchActivity.this, OVPDemoActivity.class)));

        findViewById(R.id.btn_ott).setOnClickListener(v -> startActivity(new Intent(LaunchActivity.this, OTTDemoActivity.class)));
    }

    private void doConnectionsWarmup() {
        PKHttpClientManager.setHttpProvider("okhttp");
        PKHttpClientManager.warmUp(
                 "https://https://rest-us.ott.kaltura.com/crossdomain.xml",
                "https://rest-as.ott.kaltura.com/crossdomain.xml",
                "https://api-preprod.ott.kaltura.com/crossdomain.xml",
                "https://cdnapisec.kaltura.com/alive.html",
                "https://cfvod.kaltura.com/alive.html"
        );
    }
}
