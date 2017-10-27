package com.kaltura.kalturaplayer;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

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
import com.kaltura.playkit.plugins.ovp.KavaAnalyticsConfig;
import com.kaltura.playkit.plugins.ovp.KavaAnalyticsPlugin;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Factory for interacting with Kaltura OVP.
 */

public class KalturaPlayerFactory {

    private static final String DEFAULT_SERVER_URL = "https://cdnapisec.kaltura.com/";
    private static final PKLog log = PKLog.get("KalturaPlayerFactory");

    private String referrer;

    private final Context appContext;
    private final String serverUrl;
    private final int partnerId;
    private String ks;

    private SimpleOvpSessionProvider sessionProvider;
    
    // Saving Players so we can update their plugins. Holding them in a weak map so that they are
    // removed when the application no longer uses them.
    private final WeakHashMap<Player, Boolean> loadedPlayers = new WeakHashMap<>(1);
    
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    public KalturaPlayerFactory(Context appContext, int partnerId, String ks, String serverUrl) {
        
        this.appContext = appContext;

        if (!serverUrl.endsWith("/")) {
            serverUrl += "/";
        }
        this.serverUrl = serverUrl;

        this.partnerId = partnerId;
        this.ks = ks;

        sessionProvider = new SimpleOvpSessionProvider(this.serverUrl, partnerId, ks);
    }

    public KalturaPlayerFactory(Context appContext, int partnerId, String ks) {
        this(appContext, partnerId, ks, DEFAULT_SERVER_URL);
    }

    public KalturaPlayerFactory(int partnerId, Context appContext) {
        this(appContext, partnerId, null);
    }

    public int getPartnerId() {
        return partnerId;
    }

    public KalturaPlayerFactory setKs(String ks) {
        this.ks = ks;
        sessionProvider.setKs(ks);

        // Update players
        for (Player player : loadedPlayers.keySet()) {
            player.updatePluginConfig(KavaAnalyticsPlugin.factory.getName(), getKavaAnalyticsConfig());
        }
        
        return this;
    }

    private void configureKalturaPlugins(PKPluginConfigs configs) {
        // Configure Kava
        PlayKitManager.registerPlugins(appContext, KavaAnalyticsPlugin.factory);


        KavaAnalyticsConfig kavaConfig = getKavaAnalyticsConfig();

        configs.setPluginConfig(KavaAnalyticsPlugin.factory.getName(), kavaConfig);
    }

    private KavaAnalyticsConfig getKavaAnalyticsConfig() {
        return new KavaAnalyticsConfig()
                    .setKs(ks).setPartnerId(partnerId).setReferrer(getReferrer());
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
        loadedPlayers.put(player, Boolean.TRUE);

        PlaymanifestRequestAdapter requestAdapter = PlaymanifestRequestAdapter.install(player, getReferrer());
        

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

    public KalturaPlayerFactory setReferrer(String referrer) {
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


