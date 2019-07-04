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
