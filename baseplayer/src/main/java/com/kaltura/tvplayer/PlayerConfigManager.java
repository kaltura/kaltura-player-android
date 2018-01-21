package com.kaltura.tvplayer;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.gson.JsonObject;
import com.kaltura.netkit.connect.executor.APIOkRequestsExecutor;
import com.kaltura.netkit.connect.response.ResponseElement;
import com.kaltura.netkit.utils.ErrorElement;
import com.kaltura.netkit.utils.GsonParser;
import com.kaltura.netkit.utils.OnRequestCompletion;
import com.kaltura.playkit.api.ovp.OvpConfigs;
import com.kaltura.playkit.api.ovp.OvpRequestBuilder;

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

    public static void initialize(Context context) {
        dataDir = new File(context.getFilesDir(), "KalturaPlayer/PlayerConfigs");
        dataDir.mkdirs();
        
        
        
    }

    public static void retrieve(int id, int partnerId, String ks, String serverUrl, final OnPlayerConfigLoaded onPlayerConfigLoaded) {
        // Load from cache
        final CachedConfig cachedConfig = loadFromCache(id);
        
        if (serverUrl == null) {
            serverUrl = KalturaPlayer.DEFAULT_OVP_SERVER_URL;
        }
        
        if (cachedConfig == null) {
            refreshCache(id, partnerId, ks, serverUrl, null, onPlayerConfigLoaded);
            return;
        }

        final int freshness = cachedConfig.freshness;

        if (freshness < SOFT_EXPIRATION_SEC) {
            // Just return the cache
            configLoaded(onPlayerConfigLoaded, id, cachedConfig);
            return;
        }
        
        if (freshness < HARD_EXPIRATION_SEC) {
            // Refresh the cache, but return the cache immediately
            refreshCache(id, partnerId, ks, serverUrl, cachedConfig, null);
            configLoaded(onPlayerConfigLoaded, id, cachedConfig);
            return;
        }
        
        refreshCache(id, partnerId, ks, serverUrl, cachedConfig, onPlayerConfigLoaded);
    }

    private static void configLoaded(@Nullable OnPlayerConfigLoaded onPlayerConfigLoaded, int id, CachedConfig cachedConfig) {
        if (onPlayerConfigLoaded != null) {
            onPlayerConfigLoaded.onConfigLoadComplete(id, GsonParser.toJson(cachedConfig.json).getAsJsonObject(), null, cachedConfig.freshness);
        }
    }

    private static void refreshCache(final int id, int partnerId, String ks, String serverUrl, final CachedConfig cachedConfig, final OnPlayerConfigLoaded onPlayerConfigLoaded) {
        load(partnerId, id, ks, serverUrl, new InternalCallback() {
            @Override
            public void finished(String json, ErrorElement error) {
                if (error == null) {
                    // No error 
                    saveToCache(id, json);
                    configLoaded(onPlayerConfigLoaded, id, new CachedConfig(0, json));
                } else {
                    if (cachedConfig != null) {
                        Log.e(TAG, "Failed to load new config from network -- returning old cache");
                        configLoaded(onPlayerConfigLoaded, id, cachedConfig);
                    } else {
                        Log.e(TAG, "Failed to load config from network, no cache");
                        onPlayerConfigLoaded.onConfigLoadComplete(id, null, error, -1);
                    }
                }
            }
        });
    }


    private static void load(int partnerId, final int configId, String ks, String serverUrl, final InternalCallback callback) {
        final APIOkRequestsExecutor requestsExecutor = APIOkRequestsExecutor.getSingleton();

        String apiServerUrl = serverUrl + OvpConfigs.ApiPrefix;

        OvpRequestBuilder request = UIConfService.uiConfById(apiServerUrl, partnerId, configId, ks).completion(new OnRequestCompletion() {
            @Override
            public void onComplete(final ResponseElement response) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.finished(response.getResponse(), response.getError());
                    }
                });
            }
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
        void onConfigLoadComplete(int id, JsonObject uiConf, ErrorElement error, int freshness);
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
