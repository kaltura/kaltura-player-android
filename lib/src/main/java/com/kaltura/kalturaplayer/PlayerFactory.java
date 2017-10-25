package com.kaltura.kalturaplayer;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.google.gson.JsonObject;
import com.kaltura.netkit.connect.executor.RequestQueue;
import com.kaltura.netkit.connect.response.ResultElement;
import com.kaltura.netkit.utils.ErrorElement;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKPluginConfigs;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.api.ovp.SimpleOvpSessionProvider;
import com.kaltura.playkit.mediaproviders.base.OnMediaLoadCompletion;
import com.kaltura.playkit.mediaproviders.ovp.KalturaOvpMediaProvider;

import java.util.Map;

/**
 * Factory for interacting with Kaltura OVP.
 */

public class PlayerFactory {

    private static final String DEFAULT_SERVER_URL = "https://cdnapisec.kaltura.com/";
    private static final PKLog log = PKLog.get("KalturaPlayerFactory");

    private String referrer;

    private final Context appContext;
    private final String serverUrl;
    private final int partnerId;
    private String ks;

    private SimpleOvpSessionProvider sessionProvider;
    private int uiConfId;
    private JsonObject uiConf;
    private RequestQueue requestsExecutor;
    
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    public PlayerFactory(Context appContext, int partnerId, String ks, String serverUrl) {
        
        this.appContext = appContext;

        if (!serverUrl.endsWith("/")) {
            serverUrl += "/";
        }
        this.serverUrl = serverUrl;

        this.partnerId = partnerId;
        this.ks = ks;

        sessionProvider = new SimpleOvpSessionProvider(serverUrl, partnerId, ks);
    }

    public PlayerFactory(Context appContext, int partnerId, String ks) {
        this(appContext, partnerId, ks, DEFAULT_SERVER_URL);
    }

    public PlayerFactory(int partnerId, Context appContext) {
        this(appContext, partnerId, null);
    }

    public int getPartnerId() {
        return partnerId;
    }

    public PlayerFactory setKs(String ks) {
        this.ks = ks;
        sessionProvider.setKs(ks);
        return this;
    }

    private void configureKalturaPlugins(PKPluginConfigs configs) {
        // Configure Kava
    }

    public Player loadPlayer(Context context) {
        return loadPlayer(context, null);
    }

    public Player loadPlayer(Context context, PKPluginConfigs pluginConfigs) {
        // Return a player preconfigured to use stats plugins and the playManifest adapter.
        
        PKPluginConfigs combined = new PKPluginConfigs();

        configureKalturaPlugins(combined);

        // Copy application-provided configs.
        if (pluginConfigs != null) {
            for (Map.Entry<String, Object> entry : pluginConfigs) {
                combined.setPluginConfig(entry.getKey(), entry.getValue());
            }
        }

        Player player = PlayKitManager.loadPlayer(context, combined);
        PlaymanifestRequestAdapter.install(player, getReferrer());

        return player;
    }

    public void loadEntry(String entryId, final OnMediaEntryLoaded onMediaEntryLoaded) {
        // Load and return an entry. Use the stored partnerId, KS, server.
        new KalturaOvpMediaProvider()
                .setSessionProvider(sessionProvider)
                .setEntryId(entryId)
                .load(new OnMediaLoadCompletion() {
                    @Override
                    public void onComplete(final ResultElement<PKMediaEntry> response) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                onMediaEntryLoaded.entryLoaded(response.getResponse(), response.getError());
                            }
                        });
                    }
                });
    }

    public String getReferrer() {
        if (referrer == null) {
            setReferrer(appContext.getPackageName());
        }
        return referrer;
    }

    public PlayerFactory setReferrer(String referrer) {
        Uri uri = Uri.parse(referrer);
        if (TextUtils.isEmpty(uri.getScheme())) {
            uri = uri.buildUpon().scheme("app").build();
            this.referrer = uri.toString();
        } else {
            this.referrer = referrer;
        }
        
        return this;
    }


    public interface OnMediaEntryLoaded {
        void entryLoaded(PKMediaEntry entry, ErrorElement error);
    }
}


