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

class KalturaOvpPlayer extends KalturaPlayer {

    private static final PKLog log = PKLog.get("KalturaOvpPlayer");
    private static boolean pluginsRegistered;

    public KalturaOvpPlayer(Context context, PlayerInitOptions initOptions) {
        super(context, initOptions);
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
        pkPlayer.updatePluginConfig(KavaAnalyticsPlugin.factory.getName(), getKavaAnalyticsConfig(ks));
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

    private void loadMedia(OVPMediaOptions mediaOptions, final OnEntryLoadListener listener) {

        if (mediaOptions.ks != null) {
            setKS(mediaOptions.ks);
        }

        MediaEntryProvider provider = new KalturaOvpMediaProvider()
                .setSessionProvider(newSimpleSessionProvider()).setEntryId(mediaOptions.entryId);

        provider.load(new OnMediaLoadCompletion() {
            @Override
            public void onComplete(ResultElement<PKMediaEntry> response) {
                mediaLoadCompleted(response, listener);
            }
        });
    }

    @Override
    public void loadMedia(MediaOptions mediaOptions, OnEntryLoadListener listener) {
        loadMedia(((OVPMediaOptions) mediaOptions), listener);
    }
}
