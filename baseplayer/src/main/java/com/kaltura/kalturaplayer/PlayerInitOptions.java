package com.kaltura.kalturaplayer;

import com.google.gson.JsonObject;
import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.playkit.PKPluginConfigs;

public class PlayerInitOptions {
    public int partnerId;
    public String ks;
    public PKPluginConfigs pluginConfigs;
    public Boolean autoPlay;
    public Boolean preload;
    public PKMediaFormat preferredFormat;
    public String serverUrl;
    public String referrer;
    public JsonObject playerConfig;

    public PlayerInitOptions() {
    }

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

    public PlayerInitOptions setAutoPlay(Boolean autoPlay) {
        this.autoPlay = autoPlay;
        return this;
    }

    public PlayerInitOptions setPreload(Boolean preload) {
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

    public PlayerInitOptions setPlayerConfig(JsonObject playerConfig) {
        this.playerConfig = playerConfig;
        return this;
    }
}
