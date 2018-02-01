package com.kaltura.kalturaplayertestapp.converters;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class YouboraPluginConfig {
    JsonPrimitive accountCode;
    JsonPrimitive username;
    JsonPrimitive haltOnError;
    JsonPrimitive enableSmartAds;
    JsonPrimitive parseHLS;
    JsonPrimitive parseCDNNodeHost;
    JsonPrimitive httpSecure;
    JsonPrimitive transactionCode;
    JsonPrimitive enableAnalytics;

    JsonObject media;
    JsonObject ads;
    JsonObject properties;
    JsonObject extraParams;

    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("accountCode", accountCode);
        jsonObject.add("username", username);
        jsonObject.add("haltOnError", haltOnError);
        jsonObject.add("enableSmartAds", enableSmartAds);

        jsonObject.add("parseHLS", parseHLS);
        jsonObject.add("parseCDNNodeHost", parseCDNNodeHost);
        jsonObject.add("httpSecure", httpSecure);
        jsonObject.add("transactionCode", transactionCode);
        jsonObject.add("enableAnalytics", enableAnalytics);

        jsonObject.add("media", media);
        jsonObject.add("ads", ads);
        jsonObject.add("properties", properties);
        jsonObject.add("extraParams", extraParams);

        return jsonObject;
    }
}