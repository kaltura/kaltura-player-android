package com.kaltura.kalturaplayer;

import android.content.Context;

import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKPluginConfigs;

public class KalturaPhoenixPlayer extends KalturaPlayerBase {

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
}
