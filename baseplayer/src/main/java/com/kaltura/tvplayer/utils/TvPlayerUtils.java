package com.kaltura.tvplayer.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kaltura.playkit.plugins.kava.KavaAnalyticsConfig;
import com.kaltura.tvplayer.config.ima.UiConfIMAConfig;
import com.kaltura.tvplayer.config.youbora.UiConfYouboraConfig;

import static com.kaltura.tvplayer.PlayerInitOptions.CONFIG;
import static com.kaltura.tvplayer.PlayerInitOptions.OPTIONS;
import static com.kaltura.tvplayer.PlayerInitOptions.PLAYER;
import static com.kaltura.tvplayer.PlayerInitOptions.PLUGINS;

public class TvPlayerUtils {
    public static JsonObject getPluginsConfig(JsonObject uiconfJsonObject) {
        GsonReader uiConf = GsonReader.withObject(uiconfJsonObject);
        return (uiConf != null && uiConf.getObject(CONFIG) != null && uiConf.getObject(CONFIG).getAsJsonObject(PLAYER) != null) ?
                uiConf.getObject(CONFIG).getAsJsonObject(PLAYER).getAsJsonObject(PLUGINS) :
                new JsonObject();
    }

    public static JsonObject getPluginsJsonObject(JsonObject uiconfPluginsJsonObject, String pluginName) {
        if (uiconfPluginsJsonObject != null && uiconfPluginsJsonObject.has(pluginName)) {
            return uiconfPluginsJsonObject.getAsJsonObject(pluginName);
        }
        return null;
    }

    public static UiConfIMAConfig getUiConfIMAConfig(JsonObject uiconfPluginsJsonObject) {
        Gson gson = new Gson();
        JsonObject uiConfIMAJsonObject = TvPlayerUtils.getPluginsJsonObject(uiconfPluginsJsonObject, "ima");
        if (uiConfIMAJsonObject != null) {
            return gson.fromJson(uiConfIMAJsonObject, UiConfIMAConfig.class);

        }
        return null;
    }

    public static UiConfYouboraConfig getUiConfYouboraConfig(JsonObject uiconfPluginsJsonObject) {
        Gson gson = new Gson();
        JsonObject uiConfYouboraJsonObject = TvPlayerUtils.getPluginsJsonObject(uiconfPluginsJsonObject, "youbora");
        if (uiConfYouboraJsonObject != null) {
            return gson.fromJson(uiConfYouboraJsonObject, UiConfYouboraConfig.class);
        }
        return null;
    }

    public static KavaAnalyticsConfig getUiConfKavaConfig(String partnerId, JsonObject uiconfPluginsJsonObject) {
        Gson gson = new Gson();
        JsonObject uiConfKavaJsonObject = TvPlayerUtils.getPluginsJsonObject(uiconfPluginsJsonObject, "kava");
        if (uiConfKavaJsonObject != null) {
            KavaAnalyticsConfig kavaAnalyticsConfig =  gson.fromJson(uiConfKavaJsonObject, KavaAnalyticsConfig.class);
            kavaAnalyticsConfig.setPartnerId(Integer.valueOf(partnerId));
            return kavaAnalyticsConfig;
        }
        return null;
    }
}
