package com.kaltura.kalturaplayer;

import android.content.Context;

import com.kaltura.netkit.connect.response.ResultElement;
import com.kaltura.playkit.MediaEntryProvider;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKPluginConfigs;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.mediaproviders.base.OnMediaLoadCompletion;
import com.kaltura.playkit.mediaproviders.ovp.KalturaOvpMediaProvider;
import com.kaltura.playkit.plugins.kava.KavaAnalyticsConfig;
import com.kaltura.playkit.plugins.kava.KavaAnalyticsPlugin;

public class KalturaOvpPlayer extends KalturaPlayer <KalturaOvpPlayer.MediaOptions> {

    public static final String DEFAULT_SERVER_URL = "https://cdnapisec.kaltura.com/";
    private static final PKLog log = PKLog.get("KalturaOvpPlayer");
    private static boolean pluginsRegistered;

    public KalturaOvpPlayer(Context context, int partnerId, InitOptions initOptions) {
        super(context, partnerId, initOptions);
    }

    public KalturaOvpPlayer(Context context, int partnerId) {
        this(context, partnerId, null);
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
    protected void updateKS(String ks) {
        // Update Kava
        player.updatePluginConfig(KavaAnalyticsPlugin.factory.getName(), getKavaAnalyticsConfig(ks));
    }

    private KavaAnalyticsConfig getKavaAnalyticsConfig(String ks) {
        return new KavaAnalyticsConfig()
                .setKs(ks).setPartnerId(getPartnerId()).setReferrer(referrer);
    }

    @Override
    protected void addKalturaPluginConfigs(PKPluginConfigs combined) {
        KavaAnalyticsConfig kavaConfig = getKavaAnalyticsConfig(null);

        // FIXME temporarily disabled Kava
//        combined.setPluginConfig(KavaAnalyticsPlugin.factory.getName(), kavaConfig);
    }

    @Override
    public void loadMedia(MediaOptions mediaOptions, final OnEntryLoadListener listener) {
        MediaEntryProvider provider = new KalturaOvpMediaProvider()
                .setSessionProvider(sessionProvider).setEntryId(mediaOptions.entryId);

        provider.load(new OnMediaLoadCompletion() {
            @Override
            public void onComplete(ResultElement<PKMediaEntry> response) {
                mediaLoadCompleted(response, listener);
            }
        });
    }

    @Override
    public void setMedia(PKMediaEntry entry, MediaOptions mediaOptions) {
        setStartPosition(mediaOptions.startPosition);
        setKS(mediaOptions.ks);
        
        setMedia(entry);
    }

    public static class MediaOptions {
        String entryId;
        String ks;
        double startPosition;

        public MediaOptions setEntryId(String entryId) {
            this.entryId = entryId;
            return this;
        }

        public MediaOptions setKS(String ks) {
            this.ks = ks;
            return this;
        }

        public MediaOptions setStartPosition(double startPosition) {
            this.startPosition = startPosition;
            return this;
        }
    }
}
