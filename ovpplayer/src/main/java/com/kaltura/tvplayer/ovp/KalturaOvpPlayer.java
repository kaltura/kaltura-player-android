package com.kaltura.tvplayer.ovp;

import android.content.Context;

import com.kaltura.netkit.connect.response.ResultElement;
import com.kaltura.playkit.MediaEntryProvider;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKPluginConfigs;
import com.kaltura.playkit.mediaproviders.base.OnMediaLoadCompletion;
import com.kaltura.playkit.mediaproviders.ovp.KalturaOvpMediaProvider;
import com.kaltura.playkit.plugins.kava.KavaAnalyticsConfig;
import com.kaltura.playkit.plugins.kava.KavaAnalyticsPlugin;
import com.kaltura.tvplayer.KalturaPlayer;
import com.kaltura.tvplayer.PlayerInitOptions;

public class KalturaOvpPlayer extends KalturaPlayer<OVPMediaOptions> {

    private static final PKLog log = PKLog.get("KalturaOvpPlayer");
    private static boolean pluginsRegistered;

    public static KalturaOvpPlayer create(final Context context, PlayerInitOptions options) {
        return new KalturaOvpPlayer(context, options);
    }

    private KalturaOvpPlayer(Context context, PlayerInitOptions initOptions) {
        super(context, initOptions);

        this.serverUrl = KalturaPlayer.safeServerUrl(initOptions.serverUrl, KalturaPlayer.DEFAULT_OVP_SERVER_URL);
    }

    @Override
    protected void registerPlugins(Context context) {
        // Plugin registration is static and only done once, but requires a Context.
        if (!KalturaOvpPlayer.pluginsRegistered) {
            registerCommonPlugins(context);
        }
    }

    @Override
    protected void updateKS(String ks) {
        // Update Kava
        //pkPlayer.updatePluginConfig(KavaAnalyticsPlugin.factory.getName(), getKavaAnalyticsConfig(ks));
    }

    private KavaAnalyticsConfig getKavaAnalyticsConfig(String ks) {
        return new KavaAnalyticsConfig().setKs(ks).setPartnerId(getPartnerId()).setReferrer(referrer);
    }

    @Override
    protected void addKalturaPluginConfigs(PKPluginConfigs combined) {
        //Do nothing special here
    }

    @Override
    public void loadMedia(OVPMediaOptions mediaOptions, final OnEntryLoadListener listener) {

        if (mediaOptions.getKs() != null) {
            setKS(mediaOptions.getKs());
        }
        setPreferrdMediaFormat(mediaOptions.getPreferredMediaFormat());
        setStartPosition(mediaOptions.getStartPosition());

        MediaEntryProvider provider = new KalturaOvpMediaProvider(getServerUrl(), getPartnerId(), getKS())
                .setEntryId(mediaOptions.entryId);

        provider.load(new OnMediaLoadCompletion() {
            @Override
            public void onComplete(ResultElement<PKMediaEntry> response) {
                mediaLoadCompleted(response, listener);
            }
        });
    }

    public interface PlayerReadyCallback {
        void onPlayerReady(KalturaOvpPlayer player);
    }
}
