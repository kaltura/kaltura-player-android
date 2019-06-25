package com.kaltura.tvplayer;

import android.support.annotation.NonNull;

import com.google.gson.JsonObject;

import com.kaltura.playkit.providers.api.phoenix.PhoenixRequestBuilder;
import com.kaltura.playkit.providers.api.phoenix.services.PhoenixService;


public class PhoenixConfigurations extends PhoenixService {

    public static final String KALTURA_PLAYER = "com.kaltura.player";
    public static final String UDID = "kaltura-player-android/4.0.0";

    public static PhoenixRequestBuilder configByPartnerId(@NonNull String baseUrl, int partnerId) {
        JsonObject params = new JsonObject();
        params.addProperty("applicationName", KALTURA_PLAYER);
        params.addProperty("clientVersion", "4");
        params.addProperty("partnerId", partnerId);
        params.addProperty("platform", "Android");
        params.addProperty("tag", "tag");
        params.addProperty("udid", UDID);

        return new PhoenixRequestBuilder()
                .service("Configurations")
                .action("serveByDevice")
                .method("POST")
                .url(baseUrl)
                .params(params);
    }
}
