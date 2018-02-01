package com.kaltura.kalturaplayertestapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kaltura.netkit.utils.ErrorElement;
import com.kaltura.tvplayer.PlayerConfigManager;

import org.json.JSONObject;

public class SplashScreen extends Activity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        //PlayerConfigManager.retrieve(41188731, 2215841, null, "https://cdnapisec.kaltura.com", new PlayerConfigManager.OnPlayerConfigLoaded() );

        PlayerConfigManager.initialize(this);
        PlayerConfigManager.retrieve(41604521, 1734762, null, "https://cdnapisec.kaltura.com", new PlayerConfigManager.OnPlayerConfigLoaded() {
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

    private String getAdTagFromUiConfJson(JsonObject config) {
        JsonElement configJson = config.get("config");
        String configJsonStr = configJson.getAsString();
        JsonParser parser = new JsonParser();
        JsonObject pluginsJsonObject = parser.parse(configJsonStr).getAsJsonObject();
        JsonElement doubleclickPluginElement = pluginsJsonObject.get("plugins").getAsJsonObject().get("doubleClick");

        String adTagUrl = doubleclickPluginElement.getAsJsonObject().get("adTagUrl").getAsString();
        //String companions = doubleclickPluginElement.getAsJsonObject().get("htmlCompanions").getAsString();
        return adTagUrl;
    }

}
