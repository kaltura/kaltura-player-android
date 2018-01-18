package com.kaltura.tvplayer;

import com.google.gson.JsonObject;
import com.kaltura.playkit.PKPluginConfigs;
import com.kaltura.playkit.utils.GsonReader;

public class PlayerInitOptions {
    public final int partnerId;
    public String ks;

    public final Integer uiConfId;
    public final JsonObject uiConf;
    public final GsonReader uiConfPlugins;

    public PKPluginConfigs pluginConfigs;
    
    public Boolean autoplay = true;
    public Boolean preload = true;
    public String serverUrl;
    public String referrer;
    public String audioLanguage;
    public String textLanguage;

    public PlayerInitOptions(int partnerId, JsonObject uiConf) {
        this.partnerId = partnerId;

        // Fields not found in the UIConf: partnerId*, ks*, serverUrl, referrer
        
        if (uiConf == null) {
            this.uiConfId = null;
            this.uiConf = null;
            this.uiConfPlugins = null;
            return;
        }
        
        this.uiConf = uiConf;
        this.uiConfId = GsonReader.getInteger(uiConf, "id");
        
        this.uiConfPlugins = GsonReader.getReader(uiConf, "plugins");

        // Apply player options
        // Assuming this format: 
        /*
            { 
                "playback": {
                    "audioLanguage": "",
                    "textLanguage": "",
                    "preload": "none"/"auto",
                    "autoplay": false,
                }
            }
         */

        GsonReader reader = GsonReader.getReader(uiConf, "playback");
        if (reader != null) {
            audioLanguage = reader.getString("audioLanguage");
            textLanguage = reader.getString("textLanguage");
            autoplay = reader.getBoolean("autoplay");

            String preload = reader.getString("preload");
            this.preload = "auto".equals(preload);
        }
    }

    public PlayerInitOptions setKs(String ks) {
        this.ks = ks;
        return this;
    }

    public PlayerInitOptions setPluginConfigs(PKPluginConfigs pluginConfigs) {
        if (pluginConfigs != null) {
            this.pluginConfigs = pluginConfigs;
        }
        return this;
    }

    public PlayerInitOptions setAutoplay(Boolean autoplay) {
        if (autoplay != null) {
            this.autoplay = autoplay;
        }
        return this;
    }

    public PlayerInitOptions setPreload(Boolean preload) {
        if (preload != null) {
            this.preload = preload;
        }
        return this;
    }

    public PlayerInitOptions setServerUrl(String serverUrl) {
        if (serverUrl != null) {
            this.serverUrl = serverUrl;
        }
        return this;
    }

    public PlayerInitOptions setReferrer(String referrer) {
        if (referrer != null) {
            this.referrer = referrer;
        }
        return this;
    }

    public PlayerInitOptions setAudioLanguage(String audioLanguage) {
        if (audioLanguage != null) {
            this.audioLanguage = audioLanguage;
        }
        return this;
    }

    public PlayerInitOptions setTextLanguage(String textLanguage) {
        if (textLanguage != null) {
            this.textLanguage = textLanguage;
        }
        return this;
    }
}
