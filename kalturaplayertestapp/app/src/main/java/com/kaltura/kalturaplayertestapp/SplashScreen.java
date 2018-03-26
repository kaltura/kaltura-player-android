package com.kaltura.kalturaplayertestapp;

import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kaltura.netkit.utils.ErrorElement;
import com.kaltura.playkit.PKLog;
import com.kaltura.tvplayer.PlayerConfigManager;

import org.json.JSONObject;

public class SplashScreen extends Activity {
    private static final PKLog log = PKLog.get("SplashScreen");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        boolean isPlayServicesAvailable = isGooglePlayServicesAvailable();

        if (isPlayServicesAvailable) {
            //PlayerConfigManager.retrieve(41188731, 2215841, null, "https://cdnapisec.kaltura.com", new PlayerConfigManager.OnPlayerConfigLoaded() );

            PlayerConfigManager.initialize(this);
            //PlayerConfigManager.retrieve(41604521, 1734762, null, "https://cdnapisec.kaltura.com", new PlayerConfigManager.OnPlayerConfigLoaded() {
            PlayerConfigManager.retrieve(41742801, 2222401, null, "https://cdnapisec.kaltura.com", new PlayerConfigManager.OnPlayerConfigLoaded() {

                @Override
                public void onConfigLoadComplete(int id, JsonObject config, ErrorElement error, int freshness) {
                    if (error == null) {
                        getAdTagFromUiConfJson(config);
                    }
                    Intent i = new Intent(SplashScreen.this, SignInActivity.class);
                    startActivity(i);
                    finish();
                }
            });
        }
    }

    private String getAdTagFromUiConfJson(JsonObject config) {
        JsonElement configJson = config.get("config");
        String configJsonStr = configJson.getAsString();
        JsonParser parser = new JsonParser();
        JsonObject pluginsJsonObject = parser.parse(configJsonStr).getAsJsonObject();
        String adTagUrl = "";
        if (pluginsJsonObject.has("plugins") && pluginsJsonObject.get("plugins").getAsJsonObject().has("doubleClick")) {
            JsonElement doubleclickPluginElement = pluginsJsonObject.get("plugins").getAsJsonObject().get("doubleClick");
            adTagUrl = doubleclickPluginElement.getAsJsonObject().get("adTagUrl").getAsString();
        }
        if (pluginsJsonObject.get("player").getAsJsonObject().has("doubleClick")) {
            JsonElement doubleclickPluginElement = pluginsJsonObject.get("player").getAsJsonObject().get("plugins").getAsJsonObject().get("ima");
            adTagUrl = doubleclickPluginElement.getAsJsonObject().get("adTagUrl").getAsString();
        }


        //String companions = doubleclickPluginElement.getAsJsonObject().get("htmlCompanions").getAsString();
        return adTagUrl;
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
