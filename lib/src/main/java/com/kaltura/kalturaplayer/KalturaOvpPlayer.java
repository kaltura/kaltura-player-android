package com.kaltura.kalturaplayer;

import android.content.Context;
import android.support.annotation.NonNull;

import com.kaltura.netkit.connect.response.ResultElement;
import com.kaltura.playkit.MediaEntryProvider;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKPluginConfigs;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.api.ovp.SimpleOvpSessionProvider;
import com.kaltura.playkit.mediaproviders.base.OnMediaLoadCompletion;
import com.kaltura.playkit.mediaproviders.ovp.KalturaOvpMediaProvider;
import com.kaltura.playkit.plugins.kava.KavaAnalyticsConfig;
import com.kaltura.playkit.plugins.kava.KavaAnalyticsPlugin;

public class KalturaOvpPlayer extends KalturaPlayer {

    private static final String DEFAULT_SERVER_URL = "https://cdnapisec.kaltura.com/";
    private static final PKLog log = PKLog.get("KalturaPlayer");
    private static boolean pluginsRegistered;

    private SimpleOvpSessionProvider sessionProvider;

    public KalturaOvpPlayer(Context context, int partnerId, String ks, PKPluginConfigs pluginConfigs, Options options) {
        super(context, partnerId, ks, pluginConfigs, options);
    }

    public KalturaOvpPlayer(Context context, int partnerId, String ks, PKPluginConfigs pluginConfigs) {
        this(context, partnerId, ks, pluginConfigs, null);
    }

    public KalturaOvpPlayer(Context context, int partnerId, String ks) {
        this(context, partnerId, ks, null);
    }

    @Override
    String getDefaultServerUrl() {
        return DEFAULT_SERVER_URL;
    }

    @Override
    protected void registerPlugins(Context context) {
        // Plugin registration is static and only done once, but requires a Context.
        if (!KalturaOvpPlayer.pluginsRegistered) {
            PlayKitManager.registerPlugins(context, KavaAnalyticsPlugin.factory);
            KalturaOvpPlayer.pluginsRegistered = true;
        }
    }

    @Override
    protected void updateKs(String ks) {
        if (sessionProvider != null) {
            sessionProvider.setKs(ks);
        }

        // Update Kava
        player.updatePluginConfig(KavaAnalyticsPlugin.factory.getName(), getKavaAnalyticsConfig());
    }

    private KavaAnalyticsConfig getKavaAnalyticsConfig() {
        return new KavaAnalyticsConfig()
                .setKs(ks).setPartnerId(partnerId).setReferrer(referrer);
    }

    @Override
    protected void addKalturaPluginConfigs(PKPluginConfigs combined) {
        KavaAnalyticsConfig kavaConfig = getKavaAnalyticsConfig();

        combined.setPluginConfig(KavaAnalyticsPlugin.factory.getName(), kavaConfig);
    }

    @Override
    protected void initializeBackendComponents() {
        if (!useStaticMediaProvider) {
            sessionProvider = new SimpleOvpSessionProvider(this.serverUrl, partnerId, ks);
        }
    }

    /**
     * Load entry using the media provider and call the listener.
     * If {@link #autoPrepare} is true, send the loaded media to the player.
     */
     
    public void loadMedia(@NonNull String entryId, @NonNull final OnEntryLoadListener onEntryLoadListener) {

        MediaEntryProvider provider;
        if (useStaticMediaProvider) {
            provider = StaticMediaEntryBuilder.provider(partnerId, ks, serverUrl, entryId, preferredFormat);
        } else {
            provider = new KalturaOvpMediaProvider()
                    .setSessionProvider(sessionProvider).setEntryId(entryId);
        }

        provider.load(new OnMediaLoadCompletion() {
            @Override
            public void onComplete(ResultElement<PKMediaEntry> response) {
                mediaLoadCompleted(response, onEntryLoadListener);
            }
        });
    }

}
