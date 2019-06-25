package com.kaltura.tvplayer;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.kaltura.netkit.connect.executor.APIOkRequestsExecutor;
import com.kaltura.netkit.utils.ErrorElement;
import com.kaltura.netkit.utils.GsonParser;
import com.kaltura.playkit.providers.api.ovp.OvpConfigs;
import com.kaltura.playkit.providers.api.phoenix.PhoenixRequestBuilder;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class PlayerConfigManager {

    private static final String TAG = "PlayerConfigManager";
    private static final long SOFT_EXPIRATION_SEC = 24 * 60 * 60;
    private static final long HARD_EXPIRATION_SEC = 72 * 60 * 60;
    private static Handler mainHandler = new Handler(Looper.getMainLooper());
    private static File dataDir;

    public static void retrieve(Context context, int partnerId, String serverUrl, final OnPlayerConfigLoaded onPlayerConfigLoaded) {
        if (dataDir == null) {
            dataDir = new File(context.getFilesDir(), "KalturaPlayer/PlayerConfigs");
            dataDir.mkdirs();
        }

        // Load from cache
        final CachedConfig cachedConfig = loadFromCache(partnerId);
        
        if (TextUtils.isEmpty(serverUrl)) {
            serverUrl = KalturaPlayer.DEFAULT_OVP_SERVER_URL;
        } else if (!serverUrl.endsWith("/")) {
            serverUrl += "/";
        }
        
        if (cachedConfig == null) {
            refreshCache(partnerId, serverUrl, null, onPlayerConfigLoaded);
            return;
        }

        final int freshness = cachedConfig.freshness;

        if (freshness < SOFT_EXPIRATION_SEC) {
            // Just return the cache
            configLoaded(onPlayerConfigLoaded, partnerId, cachedConfig);
            return;
        }

        if (freshness < HARD_EXPIRATION_SEC) {
            // Refresh the cache, but return the cache immediately
            refreshCache(partnerId, serverUrl, cachedConfig, null);
            configLoaded(onPlayerConfigLoaded, partnerId, cachedConfig);
            return;
        }
        
        refreshCache(partnerId, serverUrl, cachedConfig, onPlayerConfigLoaded);
    }

    private static void configLoaded(@Nullable OnPlayerConfigLoaded onPlayerConfigLoaded, int partnerId, CachedConfig cachedConfig) {
        if (onPlayerConfigLoaded != null) {
            onPlayerConfigLoaded.onConfigLoadComplete(partnerId, GsonParser.toJson(cachedConfig.json).getAsJsonObject(), null, cachedConfig.freshness);
        }
    }

    private static void refreshCache(int partnerId, String serverUrl, final CachedConfig cachedConfig, final OnPlayerConfigLoaded onPlayerConfigLoaded) {
        load(partnerId, serverUrl, new InternalCallback() {
            @Override
            public void finished(String json, ErrorElement error) {
                if (error == null) {
                    // No error 
                    saveToCache(partnerId, json);
                    configLoaded(onPlayerConfigLoaded, partnerId, new CachedConfig(0, json));
                } else {
                    if (cachedConfig != null) {
                        Log.e(TAG, "Failed to load new config from network -- returning old cache");
                        configLoaded(onPlayerConfigLoaded, partnerId, cachedConfig);
                    } else {
                        Log.e(TAG, "Failed to load config from network, no cache");
                        onPlayerConfigLoaded.onConfigLoadComplete(partnerId, null, error, -1);
                    }
                }
            }
        });
    }

    private static boolean isValidResponse(String responseString, StringBuffer errorMessage) {
        if (TextUtils.isEmpty(responseString)) {
            return false;
        }
        try {
            JsonParser jsonParser = new JsonParser();
            Object jsonResponseObject = jsonParser.parse(responseString);
            if (jsonResponseObject instanceof JsonObject) {
                JsonObject responseJson = (JsonObject) jsonResponseObject;
                if (responseJson != null && responseJson.has("objectType")) {
                    String objectType = responseJson.getAsJsonPrimitive("objectType").getAsString();
                    if (objectType.equals("KalturaAPIException")) {
                        errorMessage.append(responseJson.getAsJsonPrimitive("message").getAsString());
                        return false;
                    }
                }
            }
        } catch (JsonSyntaxException ex) {
            return false;
        }
        return true;
    }

    private static void load(int partnerId, String serverUrl, final InternalCallback callback) {
        final APIOkRequestsExecutor requestsExecutor = APIOkRequestsExecutor.getSingleton();
        //final String apiServerUrl = serverUrl + OvpConfigs.ApiPrefix;
        final String apiServerUrl = serverUrl;

        PhoenixRequestBuilder request = PhoenixConfigurations.configByPartnerId(apiServerUrl, partnerId).completion(response -> {
            mainHandler.post(() -> {
                StringBuffer errorMessage = new StringBuffer();
                int errorCode = -1;
                ErrorElement errorElement = response.getError();
                if (!isValidResponse(response.getResponse(), errorMessage)){
                    errorElement = ErrorElement.fromCode(errorCode, errorMessage.toString());
                }
                callback.finished(response.getResponse(), errorElement);
            });
        });

        requestsExecutor.queue(request.build());
    }

    private static void saveToCache(int id, String json) {
        final File file = new File(dataDir, id + ".json");
        
        FileWriter writer = null;
        try {
            writer = new FileWriter(file);
            writer.write(json);
        } catch (IOException e) {
            Log.e(TAG, "Failed to write config cache " + file, e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static CachedConfig loadFromCache(int id) {
        final File file = new File(dataDir, id + ".json");
        if (!file.exists()) {
            return null;
        }

        final long timestamp;
        final StringBuilder json;
        BufferedReader reader = null;
        try {
            timestamp = file.lastModified();
            reader = new BufferedReader(new FileReader(file));
            json = new StringBuilder();
            char[] buffer = new char[1024];
            int size;
            while ((size = reader.read(buffer)) > 0) {
                json.append(buffer, 0, size);
            }

        } catch (IOException e) {
            Log.e(TAG, "Failed to open config " + id, e);
            return null;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return new CachedConfig(timestamp, json.toString());
    }
    
    public interface OnPlayerConfigLoaded {
        void onConfigLoadComplete(int partnerId, JsonObject config, ErrorElement error, int freshness);
    }
    
    interface InternalCallback {
        void finished(String json, ErrorElement error);
    }

    static class CachedConfig {
        final int freshness;
        final String json;

        CachedConfig(long timestamp, String json) {
            if (timestamp == 0) {
                this.freshness = 0;
            } else {
                this.freshness = (int) (System.currentTimeMillis() - timestamp) / 1000;
            }
            
            this.json = json;
        }
    }
}
