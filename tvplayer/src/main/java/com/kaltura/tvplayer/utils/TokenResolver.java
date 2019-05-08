package com.kaltura.tvplayer.utils;

import com.google.gson.JsonObject;

public interface TokenResolver {
    String resolve(String string);
    JsonObject resolve(JsonObject pluginJsonConfig);
}