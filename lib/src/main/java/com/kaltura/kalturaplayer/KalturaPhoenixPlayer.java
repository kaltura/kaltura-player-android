package com.kaltura.kalturaplayer;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKPluginConfigs;
import com.kaltura.playkit.api.phoenix.APIDefines;

import java.util.Arrays;
import java.util.List;

public class KalturaPhoenixPlayer extends KalturaPlayer {

    private static final String DEFAULT_SERVER_URL = null; // TODO: default server url
    
    private static final PKLog log = PKLog.get("KalturaPhoenixPlayer");
    private static boolean pluginsRegistered;

    public KalturaPhoenixPlayer(Context context, int partnerId, String ks, PKPluginConfigs pluginConfigs, Options options) {
        super(context, partnerId, ks, pluginConfigs, options);
    }

    public KalturaPhoenixPlayer(Context context, int partnerId, String ks, PKPluginConfigs pluginConfigs) {
        this(context, partnerId, ks, pluginConfigs, null);
    }

    public KalturaPhoenixPlayer(Context context, int partnerId, String ks) {
        this(context, partnerId, ks, null);
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
    protected void updateKs(String ks) {
        // TODO: update plugins and provider
    }

    @Override
    protected void addKalturaPluginConfigs(PKPluginConfigs combined) {
        // TODO: add plugins config
    }

    @Override
    protected void initializeBackendComponents() {
        // TODO: initialize session manager etc
    }

    public void loadMedia(@NonNull String assetId, @Nullable LoadMediaOptions options, @NonNull OnEntryLoadListener onEntryLoadListener) {
        
        // TODO: Load media using the provider
    }

    public static class LoadMediaOptions {
        public APIDefines.KalturaAssetType assetType;
        public APIDefines.PlaybackContextType contextType;
        public List<String> formats;
        public List<String> mediaFileIds;
        
        public LoadMediaOptions setAssetType(APIDefines.KalturaAssetType assetType) {
            this.assetType = assetType;
            return this;
        }

        public LoadMediaOptions setContextType(APIDefines.PlaybackContextType contextType) {
            this.contextType = contextType;
            return this;
        }

        public LoadMediaOptions setFormats(List<String> formats) {
            this.formats = formats;
            return this;
        }

        public LoadMediaOptions setFormats(String... formats) {
            this.formats = Arrays.asList(formats);
            return this;
        }

        public LoadMediaOptions setMediaFileIds(List<String> mediaFileIds) {
            this.mediaFileIds = mediaFileIds;
            return this;
        }

        public LoadMediaOptions setMediaFileIds(String... mediaFileIds) {
            this.mediaFileIds = Arrays.asList(mediaFileIds);
            return this;
        }
    }

}
