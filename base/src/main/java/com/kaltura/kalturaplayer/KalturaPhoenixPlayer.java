package com.kaltura.kalturaplayer;

import android.content.Context;
import android.support.annotation.NonNull;

import com.kaltura.netkit.connect.response.ResultElement;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKPluginConfigs;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.mediaproviders.base.OnMediaLoadCompletion;
import com.kaltura.playkit.mediaproviders.ott.PhoenixMediaProvider;
import com.kaltura.playkit.plugins.kava.KavaAnalyticsConfig;
import com.kaltura.playkit.plugins.kava.KavaAnalyticsPlugin;
import com.kaltura.playkit.plugins.ott.PhoenixAnalyticsConfig;
import com.kaltura.playkit.plugins.ott.PhoenixAnalyticsPlugin;

public class KalturaPhoenixPlayer extends KalturaPlayer <TVMediaOptions> {

    private static final PKLog log = PKLog.get("KalturaPhoenixPlayer");
    private static boolean pluginsRegistered;

    public static KalturaPhoenixPlayer create(final Context context, PlayerInitOptions options) {

        final PlayerInitOptions initOptions = options != null ? options : new PlayerInitOptions();

        return new KalturaPhoenixPlayer(context, initOptions);
    }

    private KalturaPhoenixPlayer(Context context, PlayerInitOptions initOptions) {
        super(context, initOptions);
        
        this.serverUrl = safeServerUrl(initOptions.serverUrl, null);
    }

    @Override
    protected void registerPlugins(Context context) {
        if (!pluginsRegistered) {
            PlayKitManager.registerPlugins(context, 
                    PhoenixAnalyticsPlugin.factory,
                    KavaAnalyticsPlugin.factory);
            
            pluginsRegistered = true;
        }
    }

    @Override
    protected void updateKS(String ks) {
        // update plugins and provider
        pkPlayer.updatePluginConfig(PhoenixAnalyticsPlugin.factory.getName(), getPhoenixAnalyticsConfig());
//        pkPlayer.updatePluginConfig(KavaAnalyticsPlugin.factory.getName(), getKavaAnalyticsConfig());
    }

    @Override
    protected void addKalturaPluginConfigs(PKPluginConfigs combined) {
        combined.setPluginConfig(PhoenixAnalyticsPlugin.factory.getName(), getPhoenixAnalyticsConfig());
//        combined.setPluginConfig(KavaAnalyticsPlugin.factory.getName(), getKavaAnalyticsConfig());
    }

    private KavaAnalyticsConfig getKavaAnalyticsConfig() {
        return new KavaAnalyticsConfig().setPartnerId(getPartnerId()).setReferrer(referrer);
    }

    @NonNull
    private PhoenixAnalyticsConfig getPhoenixAnalyticsConfig() {
        return new PhoenixAnalyticsConfig(getPartnerId(), getServerUrl(), getKS(), 30);
    }


    @Override
    public void loadMedia(TVMediaOptions mediaOptions, final OnEntryLoadListener listener) {
        
        if (mediaOptions.ks != null) {
            setKS(mediaOptions.ks);
        }
        
        final PhoenixMediaProvider provider = new PhoenixMediaProvider()
                .setAssetId(mediaOptions.assetId)
                .setSessionProvider(newSimpleSessionProvider());

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
