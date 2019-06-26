package com.kaltura.tvplayer;

import com.google.gson.JsonObject;
import com.kaltura.playkit.providers.api.ovp.OvpConfigs;
import com.kaltura.playkit.providers.api.ovp.OvpRequestBuilder;
import com.kaltura.playkit.providers.api.ovp.services.OvpService;

public class OVPConfigurations extends OvpService {
    public static OvpRequestBuilder configByPartnerId(String baseUrl, int partnerId) {
        JsonObject params = new JsonObject();
        params.addProperty("id", partnerId);

        return new OvpRequestBuilder()
                .service("partner")
                .action("getPublicInfo")
                .method("POST")
                .url(baseUrl + "/" + OvpConfigs.ApiPrefix)
                .params(params);
    }
}
