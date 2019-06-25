package com.kaltura.kalturaplayertestapp;

import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.gson.Gson;
import com.kaltura.playkit.PKLog;
import com.kaltura.tvplayer.config.PhoenixConfigurationsResponse;
import com.kaltura.tvplayer.PlayerConfigManager;

public class SplashScreen extends Activity {
    private static final PKLog log = PKLog.get("SplashScreen");
    private Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        boolean isPlayServicesAvailable = isGooglePlayServicesAvailable();

        if (isPlayServicesAvailable) {

            //PlayerConfigManager.retrieve(41188731, 2215841, null, "https://cdnapisec.kaltura.com", new PlayerConfigManager.OnPlayerConfigLoaded() );
            //PlayerConfigManager.retrieve(41604521, 1734762, null, "https://cdnapisec.kaltura.com", new PlayerConfigManager.OnPlayerConfigLoaded() {
            //PlayerConfigManager.retrieve(2222401, "https://cdnapisec.kaltura.com", (partnerId, asJsonObject, error, freshness) -> {
             PlayerConfigManager.retrieve(this, 3009, "https://rest-us.ott.kaltura.com/v4_5/api_v3/", (partnerId, config, error, freshness) -> {
                    //PhoenixConfigurationsResponse phoenixConfigurationsResponse = gson.fromJson(asJsonObject, PhoenixConfigurationsResponse.class);
                    Intent i = new Intent(SplashScreen.this, SignInActivity.class);
                    startActivity(i);
                    finish();
            });
        }
    }

    public boolean isGooglePlayServicesAvailable() {
        final int googlePlayServicesCheck = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(SplashScreen.this);
        switch (googlePlayServicesCheck) {
            case ConnectionResult.SUCCESS:
                return true;
            case ConnectionResult.SERVICE_DISABLED:
            case ConnectionResult.SERVICE_INVALID:
            case ConnectionResult.SERVICE_MISSING:
            case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
                Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(SplashScreen.this, googlePlayServicesCheck, 0);
                dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        try {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + "com.google.android.gms"));
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            SplashScreen.this.startActivity(intent);
                            SplashScreen.this.finish();
                        } catch (ActivityNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                });
                dialog.show();

        }
        return false;
    }
}
