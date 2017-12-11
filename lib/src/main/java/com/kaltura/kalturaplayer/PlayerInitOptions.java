package com.kaltura.kalturaplayer;

import com.google.gson.JsonObject;
import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.playkit.PKPluginConfigs;

public class PlayerInitOptions {
    public JsonObject uiConf;
    public int partnerId;
    public String ks;
    public PKPluginConfigs pluginConfigs;
    public boolean autoPlay;
    public boolean preload;
    public PKMediaFormat preferredFormat;
    public String serverUrl;
    public String referrer;

    public PlayerInitOptions setPartnerId(int partnerId) {
        this.partnerId = partnerId;
        return this;
    }

    public PlayerInitOptions setKs(String ks) {
        this.ks = ks;
        return this;
    }

    public PlayerInitOptions setPluginConfigs(PKPluginConfigs pluginConfigs) {
        this.pluginConfigs = pluginConfigs;
        return this;
    }

    public PlayerInitOptions setAutoPlay(boolean autoPlay) {
        this.autoPlay = autoPlay;
        return this;
    }

    public PlayerInitOptions setPreload(boolean preload) {
        this.preload = preload;
        return this;
    }

    public PlayerInitOptions setPreferredFormat(PKMediaFormat preferredFormat) {
        this.preferredFormat = preferredFormat;
        return this;
    }

    public PlayerInitOptions setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
        return this;
    }

    public PlayerInitOptions setReferrer(String referrer) {
        this.referrer = referrer;
        return this;
    }
}
