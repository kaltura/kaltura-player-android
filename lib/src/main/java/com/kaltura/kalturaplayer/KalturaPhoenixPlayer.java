package com.kaltura.kalturaplayer;

import android.content.Context;
import android.support.annotation.NonNull;

import com.kaltura.netkit.connect.response.ResultElement;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKPluginConfigs;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.api.phoenix.APIDefines;
import com.kaltura.playkit.mediaproviders.base.OnMediaLoadCompletion;
import com.kaltura.playkit.mediaproviders.ott.PhoenixMediaProvider;
import com.kaltura.playkit.plugins.kava.KavaAnalyticsConfig;
import com.kaltura.playkit.plugins.kava.KavaAnalyticsPlugin;
import com.kaltura.playkit.plugins.ott.PhoenixAnalyticsConfig;
import com.kaltura.playkit.plugins.ott.PhoenixAnalyticsPlugin;

public class KalturaPhoenixPlayer extends KalturaPlayer <KalturaPhoenixPlayer.MediaOptions> {

    private static final PKLog log = PKLog.get("KalturaPhoenixPlayer");
    private static boolean pluginsRegistered;

    public KalturaPhoenixPlayer(Context context, int partnerId) {
        this(context, partnerId, null);
    }

    public KalturaPhoenixPlayer(Context context, int partnerId, InitOptions initOptions) {
        super(context, partnerId, initOptions);
    }

    @Override
    String getDefaultServerUrl() {
        return null;
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
        player.updatePluginConfig(PhoenixAnalyticsPlugin.factory.getName(), getPhoenixAnalyticsConfig());
//        player.updatePluginConfig(KavaAnalyticsPlugin.factory.getName(), getKavaAnalyticsConfig());
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
    public void loadMedia(MediaOptions mediaOptions, final OnEntryLoadListener listener) {
        final PhoenixMediaProvider provider = new PhoenixMediaProvider()
                .setAssetId(mediaOptions.assetId)
                .setSessionProvider(sessionProvider);

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

    @Override
    public void setMedia(PKMediaEntry entry, MediaOptions mediaOptions) {
        setStartPosition(mediaOptions.startPosition);
        setMedia(entry);
    }

    public static class MediaOptions {
        String assetId;
        APIDefines.KalturaAssetType assetType;
        APIDefines.PlaybackContextType contextType;
        String[] formats;
        String[] fileIds;
        String ks;
        double startPosition;

        public MediaOptions setAssetId(String assetId) {
            this.assetId = assetId;
            return this;
        }

        public MediaOptions setAssetType(APIDefines.KalturaAssetType assetType) {
            this.assetType = assetType;
            return this;
        }

        public MediaOptions setContextType(APIDefines.PlaybackContextType contextType) {
            this.contextType = contextType;
            return this;
        }

        public MediaOptions setFormats(String[] formats) {
            this.formats = formats;
            return this;
        }

        public MediaOptions setFileIds(String[] fileIds) {
            this.fileIds = fileIds;
            return this;
        }

        public MediaOptions setStartPosition(double startPosition) {
            this.startPosition = startPosition;
            return this;
        }

        public MediaOptions setKS(String ks) {
            this.ks = ks;
            return this;
        }
    }
}
