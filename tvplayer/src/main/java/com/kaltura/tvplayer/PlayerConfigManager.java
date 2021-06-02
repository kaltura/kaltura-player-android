package com.kaltura.tvplayer;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.kaltura.netkit.utils.ErrorElement;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.providers.api.ovp.OvpConfigs;
import com.kaltura.playkit.utils.NetworkUtils;
import com.kaltura.playkit.utils.NetworkUtilsCallback;
import com.kaltura.tvplayer.config.PhoenixConfigurationsResponse;
import com.kaltura.tvplayer.config.PhoenixTVPlayerParams;
import com.kaltura.tvplayer.config.TVPlayerParams;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class PlayerConfigManager {

    private static final PKLog log = PKLog.get("PlayerConfigManager");

    private static Gson gson = new Gson();
    private static final long SOFT_EXPIRATION_SEC = 72 * 60 * 60; // do not refresh till next 3rd day
    private static final long HARD_EXPIRATION_SEC = 148 * 60 * 60; // between 72 and 148 hours get it from cache and refresh o/w get it from network
    public static final String KALTURA_PLAYER = "com.kaltura.player";
    public static final String UDID = "kaltura-player-android/4.0.0";
    private static Handler mainHandler = new Handler(Looper.getMainLooper());
    private static File dataDir;

    public static void retrieve(Context context, KalturaPlayer.Type playerType, int partnerId, String serverUrl, final OnPlayerConfigLoaded onPlayerConfigLoaded) {
        //playerType = tvPlayerType;
        if (dataDir == null) {
            dataDir = new File(context.getFilesDir(), "KalturaPlayer/PlayerConfigs");
            dataDir.mkdirs();
        }

        // Load from cache
        final CachedConfig cachedConfig = loadFromCache(partnerId);
        serverUrl = KalturaPlayer.safeServerUrl(playerType, serverUrl, KalturaPlayer.Type.ovp.equals(playerType) ? KalturaPlayer.DEFAULT_OVP_SERVER_URL : null);

        if (cachedConfig == null) {
            refreshCache(context, playerType, partnerId, serverUrl, null, onPlayerConfigLoaded);
            return;
        }

        final int freshness = cachedConfig.freshness;

        if (freshness < SOFT_EXPIRATION_SEC) {
            // Just return the cache
            configLoaded(onPlayerConfigLoaded, playerType, partnerId, serverUrl, cachedConfig);
            return;
        }

        if (freshness < HARD_EXPIRATION_SEC) {
            // Refresh the cache, but return the cache immediately
            refreshCache(context, playerType, partnerId, serverUrl, cachedConfig, null);
            configLoaded(onPlayerConfigLoaded, playerType, partnerId, serverUrl, cachedConfig);
            return;
        }

        refreshCache(context, playerType, partnerId, serverUrl, cachedConfig, onPlayerConfigLoaded);
    }

    static TVPlayerParams retrieve(KalturaPlayer.Type tvPlayerType, int partnerId) {
        final CachedConfig cachedConfig = loadFromCache(partnerId);
        if (cachedConfig != null) {
            if (KalturaPlayer.Type.ovp.equals(tvPlayerType)) {
                return gson.fromJson(cachedConfig.json, TVPlayerParams.class);
            } else  if (KalturaPlayer.Type.ott.equals(tvPlayerType)) {
                return gson.fromJson(cachedConfig.json, PhoenixTVPlayerParams.class);
            }
        }
        return null;
    }

    private static void configLoaded(@Nullable OnPlayerConfigLoaded onPlayerConfigLoaded, KalturaPlayer.Type playerType, int partnerId, String serverUrl, CachedConfig cachedConfig) {
        if (onPlayerConfigLoaded != null) {

            TVPlayerParams playerParams = null;
            if (KalturaPlayer.Type.ovp.equals(playerType)) {
                playerParams = gson.fromJson(cachedConfig.json, TVPlayerParams.class);
            } else  if (KalturaPlayer.Type.ott.equals(playerType)) {
                playerParams = gson.fromJson(cachedConfig.json, PhoenixTVPlayerParams.class);
            }
            onPlayerConfigLoaded.onConfigLoadComplete(playerParams, null, cachedConfig.freshness);
        }
    }

    private static void refreshCache(Context context, KalturaPlayer.Type playerType, int partnerId, String serverUrl, final CachedConfig cachedConfig, final OnPlayerConfigLoaded onPlayerConfigLoaded) {
        load(context, playerType, partnerId, serverUrl, (json, error) -> {
            if (error == null && json != null) {
                TVPlayerParams playerParams = null;
                if (KalturaPlayer.Type.ovp.equals(playerType)) {
                    playerParams = gson.fromJson(json, TVPlayerParams.class);

                } else  if (KalturaPlayer.Type.ott.equals(playerType)) {
                    PhoenixConfigurationsResponse phoenixConfigurationsResponse = gson.fromJson(json, PhoenixConfigurationsResponse.class);
                    playerParams = phoenixConfigurationsResponse.params;
                }
                playerParams.serviceUrl = serverUrl;
                playerParams.partnerId = partnerId;


                String updatedConfig = gson.toJson(playerParams);
                // No error
                saveToCache(partnerId, updatedConfig);
                configLoaded(onPlayerConfigLoaded, playerType, partnerId, serverUrl, new CachedConfig(0, updatedConfig));
            } else {
                if (cachedConfig != null) {
                    log.e("Failed to load new config from network -- returning old cache partnerId = " + partnerId);
                    configLoaded(onPlayerConfigLoaded, playerType, partnerId, serverUrl, cachedConfig);
                } else {
                    log.e("Failed to load config from network, no cache partnerId = " + partnerId);
                    onPlayerConfigLoaded.onConfigLoadComplete(null, ErrorElement.GeneralError, -1);
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

    private static void load(Context context, KalturaPlayer.Type playerType, int partnerId, String serverUrl, final NetworkUtilsCallback callback) {
        mainHandler.post(() -> {
            if (KalturaPlayer.Type.ott.equals(playerType)) {
                NetworkUtils.requestOttConfigByPartnerId(context, serverUrl, partnerId, KALTURA_PLAYER, UDID, callback);
            } else if (KalturaPlayer.Type.ovp.equals(playerType)) {
                NetworkUtils.requestOvpConfigByPartnerId(context, serverUrl, partnerId, OvpConfigs.ApiPrefix, callback);
            }
        });
    }

    private static void saveToCache(int id, String json) {
        //log.d("saveToCache partnerId = " + id + "->" + json);

        final File file = new File(dataDir, id + ".json");

        FileWriter writer = null;
        try {
            writer = new FileWriter(file);
            writer.write(json);
        } catch (IOException e) {
            log.e ("Failed to write config cache " + file, e);
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

    static CachedConfig loadFromCache(int partnerId) {
        if (dataDir == null) {
            return null;
        }
        final File file = new File(dataDir, partnerId + ".json");
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
            log.e("Failed to open config " + partnerId, e);
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
        void onConfigLoadComplete(TVPlayerParams config, ErrorElement error, int freshness);
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
