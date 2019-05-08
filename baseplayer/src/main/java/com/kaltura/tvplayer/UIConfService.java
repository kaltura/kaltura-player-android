package com.kaltura.tvplayer;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.JsonObject;
import com.kaltura.playkit.providers.api.ovp.OvpRequestBuilder;
import com.kaltura.playkit.providers.api.ovp.services.OvpService;

// TODO: 2019-05-08 Use GET

public class UIConfService extends OvpService {
    public static OvpRequestBuilder uiConfById(@NonNull String baseUrl, int partnerId, int confId, @Nullable String ks) {
        JsonObject params = new JsonObject();
        params.addProperty("id", confId);
        params.addProperty("partnerId", partnerId);

        if (ks != null) {
            params.addProperty("ks", ks);
        }

        return new OvpRequestBuilder()
                .service("uiConf")
                .action("get")
                .method("POST")
                .url(baseUrl)
                .params(params);
    }
}
