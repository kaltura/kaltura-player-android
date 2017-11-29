package com.kaltura.kalturaplayer;

import android.content.Context;

import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKPluginConfigs;
import com.kaltura.playkit.api.phoenix.APIDefines;

public class KalturaPhoenixPlayer extends KalturaPlayer <KalturaPhoenixPlayer.MediaOptions> {

    private static final String DEFAULT_SERVER_URL = null; // TODO: default server url
    
    private static final PKLog log = PKLog.get("KalturaPhoenixPlayer");
    private static boolean pluginsRegistered;

    public KalturaPhoenixPlayer(Context context, int partnerId, InitOptions initOptions) {
        super(context, partnerId, initOptions);
    }

    @Override
    String getDefaultServerUrl() {
        return DEFAULT_SERVER_URL;
    }

    @Override
    protected void registerPlugins(Context context) {
        if (!pluginsRegistered) {
            // TODO: register OTT plugins
            
            pluginsRegistered = true;
        }
    }

    @Override
    protected void updateKS(String ks) {
        // TODO: update plugins and provider
    }

    @Override
    protected void addKalturaPluginConfigs(PKPluginConfigs combined) {
        // TODO: add plugins config
    }

    @Override
    public void loadMedia(MediaOptions mediaOptions, OnEntryLoadListener listener) {
        // TODO: Load media using the provider
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
