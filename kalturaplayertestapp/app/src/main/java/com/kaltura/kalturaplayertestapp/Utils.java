package com.kaltura.kalturaplayertestapp;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kaltura.kalturaplayertestapp.converters.UiConf;
import com.kaltura.playkit.PKPluginConfigs;
import com.kaltura.tvplayer.PlayerInitOptions;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Scanner;

/**
 * Created by gilad.nadav on 1/24/18.
 */

public class Utils {

    public static PlayerInitOptions parseInitOptions(int partnerId, JsonObject config) {

        JsonObject uiconfJson = null;
        String mediaProvider = safeString(config, "mediaProvider");
        final PlayerInitOptions options = new PlayerInitOptions(partnerId);
//
//        {
//           "mode": "ovp",
//                "initOptions": {
//           "partnerId": 2215841,
//                    "autoPlay": true,
//                    "ks": null
//        }
//        "items": [
//            {
//                  "name": "Sintel",
//                    "entryId": "1_w9zx2eti"
//            },
//            {
//                  "name": "Sintel - snippet",
//                      "entryId": "1_9bwuo813"
//            }
//          ]
//        }



//        {
//  "mediaProvider": "ovp",
//  "ovpBaseUrl": "https://cdnapisec.kaltura.com",
//  "ovpPartnerId": 243342,
//  "uiConfId": 21099702,
//  "startPosition": 0,
//  "ovpEntriesList": [
//    "0_uka1msg4"
//  ]
//        }


        if ("ovp".equals(mediaProvider)) {
//            final Integer partnerId = safeInteger(json, "partnerId");
//            if (partnerId == null) {
//                throw new IllegalArgumentException("partnerId must not be null");
//            }
            options
                    .setServerUrl(safeString(config, "ovpBaseUrl"))
                    .setAutoPlay(safeBoolean(config, "autoPlay"))
                    .setPreload(safeBoolean(config, "preload"))
                    .setKs(safeString(config, "ks"))
                    .setPluginConfigs(parsePluginConfigs(config.get("plugins")))
                    .setReferrer(safeString(config, "referrer"));

        } else if ("ott".equals(mediaProvider)) {
            //TODO
        }
        return options;
    }

    public static PKPluginConfigs parsePluginConfigs(JsonElement json) {
        PKPluginConfigs configs = new PKPluginConfigs();
        if (json != null && json.isJsonObject()) {
            final JsonObject obj = json.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                final String pluginName = entry.getKey();
                final JsonElement value = entry.getValue();
                configs.setPluginConfig(pluginName, value);
            }
        }
        return configs;
    }

    public static JsonObject safeObject(JsonObject json, String key) {
        final JsonElement jsonElement = json.get(key);
        if (jsonElement != null && jsonElement.isJsonObject()) {
            return jsonElement.getAsJsonObject();
        }
        return null;
    }

    public static String safeString(JsonObject json, String key) {
        final JsonElement jsonElement = json.get(key);
        if (jsonElement != null && jsonElement.isJsonPrimitive()) {
            return jsonElement.getAsString();
        }
        return null;
    }

    public static Boolean safeBoolean(JsonObject json, String key) {
        final JsonElement jsonElement = json.get(key);
        if (jsonElement != null && jsonElement.isJsonPrimitive()) {
            return jsonElement.getAsBoolean();
        }
        return null;
    }

    public static Integer safeInteger(JsonObject json, String key) {
        final JsonElement jsonElement = json.get(key);
        if (jsonElement != null && jsonElement.isJsonPrimitive()) {
            return jsonElement.getAsInt();
        }
        return null;
    }


    public static String getResponseFromHttpUrl(URL url) throws IOException {
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        try {
            InputStream in = urlConnection.getInputStream();

            Scanner scanner = new Scanner(in);
            scanner.useDelimiter("\\A");

            boolean hasInput = scanner.hasNext();
            if (hasInput) {
                return scanner.next();
            } else {
                return null;
            }
        } finally {
            urlConnection.disconnect();
        }
    }
}
