package com.kaltura.kalturaplayer;

import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kaltura.netkit.connect.executor.APIOkRequestsExecutor;
import com.kaltura.netkit.connect.request.RequestBuilder;
import com.kaltura.netkit.connect.response.ResponseElement;
import com.kaltura.netkit.utils.GsonParser;
import com.kaltura.netkit.utils.OnRequestCompletion;
import com.kaltura.playkit.api.phoenix.PhoenixParser;
import com.kaltura.playkit.api.phoenix.PhoenixRequestBuilder;
import com.kaltura.playkit.api.phoenix.model.KalturaLoginSession;

class KalturaAnonymousSession {

    static void send(final RequestBuilder login, final KSResultCallback callback) {
        login.completion(new OnRequestCompletion() {
            @Override
            public void onComplete(final ResponseElement response) {
                if (!response.isSuccess()) {
                    callback.complete(null, response.getError());
                } else {
                    final String json = response.getResponse();
                    if (login instanceof PhoenixRequestBuilder) {
                        KalturaLoginSession result = PhoenixParser.parse(json);
                        callback.complete(result.getKs(), null);
                    } else {
                        // FIXME: 30/11/2017 HACK because the fields of KalturaStartWidgetSessionResponse are private.
                        final JsonElement jsonElement = GsonParser.toJson(json);
                        final String ks = ((JsonObject) jsonElement).get("ks").getAsString();
                        Log.d("JSON", json);
                        callback.complete(ks, null);
                    }
                }
            }
        });

        APIOkRequestsExecutor.getSingleton().queue(login.build());
    }
}
