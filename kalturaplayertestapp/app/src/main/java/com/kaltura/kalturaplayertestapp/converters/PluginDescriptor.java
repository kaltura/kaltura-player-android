package com.kaltura.kalturaplayertestapp.converters;


import com.google.gson.JsonObject;

public class PluginDescriptor {
    private String pluginName;
    private JsonObject params;

    public String getPluginName() {
        return pluginName;
    }

    public void setPluginName(String pluginName) {
        this.pluginName = pluginName;
    }

    public JsonObject getParams() {
        return params;
    }

    public void setParams(JsonObject params) {
        this.params = params;
    }
}
