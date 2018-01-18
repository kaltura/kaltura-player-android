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
import com.kaltura.playkit.plugins.ovp.KalturaLiveStatsConfig;
import com.kaltura.playkit.plugins.ovp.KalturaStatsConfig;
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
        if (!pluginsRegistered) {
            registerCommonPlugins(context);
        }
    }

    @Override
    protected void updateKS(String ks) {
        // Update Kava
        pkPlayer.updatePluginConfig(KavaAnalyticsPlugin.factory.getName(), getDefaultKavaConfig(ks));
    }

    private KavaAnalyticsConfig getDefaultKavaConfig(String ks) {
        // TODO: merge UIConf
        return new KavaAnalyticsConfig()
                .setKs(ks).setPartnerId(getPartnerId()).setReferrer(referrer);
    }

    private KalturaLiveStatsConfig getDefaultLiveStatsConfig() {
        // TODO: merge UIConf
        final PKMediaEntry mediaEntry = getMediaEntry();
        return new KalturaLiveStatsConfig(getPartnerId(), "{{entryId}}");
    }

    private KalturaStatsConfig getDefaultStatsConfig() {
        // TODO: merge UIConf
        final PKMediaEntry mediaEntry = getMediaEntry();
        return new KalturaStatsConfig(getUiConfId(), getPartnerId(), "{{entryId}}", null, 0, true);
    }

    @Override
    protected void addKalturaPluginConfigs(PKPluginConfigs combined) {
        KavaAnalyticsConfig kavaConfig = getDefaultKavaConfig(null);
        combined.setPluginConfig(KavaAnalyticsPlugin.factory.getName(), kavaConfig);
    }

    @Override
    public void loadMedia(OVPMediaOptions mediaOptions, final OnEntryLoadListener listener) {

        if (mediaOptions.ks != null) {
            setKS(mediaOptions.ks);
        }

        MediaEntryProvider provider = new KalturaOvpMediaProvider(getServerUrl(), getPartnerId(), getKS())
                .setEntryId(mediaOptions.entryId);

        provider.load(new OnMediaLoadCompletion() {
            @Override
            public void onComplete(ResultElement<PKMediaEntry> response) {
                mediaLoadCompleted(response, listener);
            }
        });
    }
}
