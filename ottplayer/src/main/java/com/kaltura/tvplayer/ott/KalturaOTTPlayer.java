package com.kaltura.tvplayer.ott;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kaltura.netkit.connect.response.ResultElement;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKPluginConfigs;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.mediaproviders.base.OnMediaLoadCompletion;
import com.kaltura.playkit.mediaproviders.ott.PhoenixMediaProvider;
import com.kaltura.playkit.plugins.kava.KavaAnalyticsConfig;
import com.kaltura.playkit.plugins.ott.PhoenixAnalyticsConfig;
import com.kaltura.playkit.plugins.ott.PhoenixAnalyticsPlugin;
import com.kaltura.playkit.plugins.ovp.KalturaLiveStatsConfig;
import com.kaltura.tvplayer.KalturaPlayer;
import com.kaltura.tvplayer.PlayerInitOptions;

import java.util.Map;

public class KalturaOTTPlayer extends KalturaPlayer<OTTMediaOptions> {

    private static final PKLog log = PKLog.get("KalturaOTTPlayer");
    private static boolean pluginsRegistered;

    public static KalturaOTTPlayer create(final Context context, PlayerInitOptions options) {
        return new KalturaOTTPlayer(context, options);
    }

    private KalturaOTTPlayer(Context context, PlayerInitOptions initOptions) {
        super(context, initOptions);

        this.serverUrl = KalturaPlayer.safeServerUrl(initOptions.serverUrl, null);
    }

    @Override
    protected void registerPlugins(Context context) {
        if (!pluginsRegistered) {
            registerCommonPlugins(context);

            PlayKitManager.registerPlugins(context, PhoenixAnalyticsPlugin.factory);

            pluginsRegistered = true;
        }
    }

    @Override
    protected void updateKS(String ks) {
        // update plugins and provider
        //pkPlayer.updatePluginConfig(PhoenixAnalyticsPlugin.factory.getName(), getPhoenixAnalyticsConfig());
        //pkPlayer.updatePluginConfig(KavaAnalyticsPlugin.factory.getName(), getKavaAnalyticsConfig());
    }

    @Override
    protected void addKalturaPluginConfigs(PKPluginConfigs combined) {
        //NOT ADDING PHOENIX IF KS IS NOT VALID
        if (!TextUtils.isEmpty(getKS())) {
            PhoenixAnalyticsConfig phoenixConfig = getPhoenixAnalyticsConfig();
            if (phoenixConfig != null) {
                combined.setPluginConfig(PhoenixAnalyticsPlugin.factory.getName(), phoenixConfig);
            }
        }
    }

    @Override
    protected void updateKalturaPluginConfigs(PKPluginConfigs combined) {
        log.d("OTTPlayer updateKalturaPluginConfigs");
        for (Map.Entry<String, Object> plugin : combined) {
            if (plugin.getValue() instanceof JsonObject) {
                updatePluginConfig(plugin.getKey(), (JsonObject) plugin.getValue());
            } else {
                log.e("OTTPlayer updateKalturaPluginConfigs " + plugin.getKey()  + " is not a JsonObject");
            }
        }
        if (!TextUtils.isEmpty(getKS())) {
            PhoenixAnalyticsConfig phoenixConfig = getPhoenixAnalyticsConfig();
            if (phoenixConfig != null) {
                updatePluginConfig(PhoenixAnalyticsPlugin.factory.getName(), phoenixConfig);
            }
        }
    }

    private KavaAnalyticsConfig getKavaAnalyticsConfig() {
        return new KavaAnalyticsConfig().setPartnerId(getPartnerId()).setReferrer(referrer);
    }

    private KalturaLiveStatsConfig getLiveStatsConfig() {
        final PKMediaEntry mediaEntry = getMediaEntry();
        return new KalturaLiveStatsConfig(getPartnerId(), mediaEntry != null ? mediaEntry.getId() : null);
    }


    @NonNull
    private PhoenixAnalyticsConfig getPhoenixAnalyticsConfig() {
        // Special case: Phoenix plugin
        // Phoenix
        String name = PhoenixAnalyticsPlugin.factory.getName();
        JsonObject phoenixAnalyticObject = null;
        if (getInitOptions().pluginConfigs.hasConfig(name)) {
            phoenixAnalyticObject = (JsonObject) getInitOptions().pluginConfigs.getPluginConfig(name);
        } else {
            phoenixAnalyticObject = phoenixAnalyticDefaults(getPartnerId(), getServerUrl(), getKS(), -1);
        }
        return new Gson().fromJson(phoenixAnalyticObject, PhoenixAnalyticsConfig.class);

    }

    private JsonObject phoenixAnalyticDefaults(int partnerId, String serverUrl, String ks, int timerInterval) {
        return new PhoenixAnalyticsConfig(partnerId, serverUrl, ks, timerInterval).toJson();
    }


    @Override
    public void loadMedia(OTTMediaOptions mediaOptions, final OnEntryLoadListener listener) {

        if (mediaOptions.getKs() != null) {
            setKS(mediaOptions.getKs());
        }
        setPreferrdMediaFormat(mediaOptions.getPreferredMediaFormat());
        setStartPosition(mediaOptions.getStartPosition());

        final PhoenixMediaProvider provider = new PhoenixMediaProvider(getServerUrl(), getPartnerId(), getKS())
                .setAssetId(mediaOptions.assetId);


        if (mediaOptions.fileIds != null) {
            provider.setFileIds(mediaOptions.fileIds);
        }

        if (mediaOptions.contextType != null) {
            provider.setContextType(mediaOptions.contextType);
        }

        if (mediaOptions.assetType != null) {
            provider.setAssetType(mediaOptions.assetType);
        }

        if (mediaOptions.formats != null) {
            provider.setFormats(mediaOptions.formats);
        }

        provider.load(new OnMediaLoadCompletion() {
            @Override
            public void onComplete(ResultElement<PKMediaEntry> response) {
                mediaLoadCompleted(response, listener);
            }
        });
    }
}
