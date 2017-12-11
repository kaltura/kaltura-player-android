package com.kaltura.kalturaplayer;

import android.os.Handler;
import android.os.Looper;

import com.google.gson.JsonObject;
import com.kaltura.netkit.connect.executor.APIOkRequestsExecutor;
import com.kaltura.netkit.connect.response.ResponseElement;
import com.kaltura.netkit.utils.ErrorElement;
import com.kaltura.netkit.utils.GsonParser;
import com.kaltura.netkit.utils.OnRequestCompletion;
import com.kaltura.playkit.api.ovp.OvpConfigs;
import com.kaltura.playkit.api.ovp.OvpRequestBuilder;

public class PlayerConfigManager {

    private static Handler mainHandler = new Handler(Looper.getMainLooper());

    public static void load(int partnerId, final int configId, final OnPlayerConfigLoaded onPlayerConfigLoaded) {
        load(partnerId, configId, null, KalturaPlayer.DEFAULT_OVP_SERVER_URL, onPlayerConfigLoaded);
    }

    public static void load(int partnerId, final int configId, String ks, String serverUrl, final OnPlayerConfigLoaded onPlayerConfigLoaded) {
        final APIOkRequestsExecutor requestsExecutor = APIOkRequestsExecutor.getSingleton();

        String apiServerUrl = serverUrl + OvpConfigs.ApiPrefix;

        OvpRequestBuilder request = UIConfService.uiConfById(apiServerUrl, partnerId, configId, ks).completion(new OnRequestCompletion() {
            @Override
            public void onComplete(final ResponseElement response) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (response.isSuccess()) {
                            String uiConfString = response.getResponse();
                            JsonObject uiConf = (JsonObject) GsonParser.toJson(uiConfString);
                            onPlayerConfigLoaded.configLoaded(configId, uiConf, null);
                        } else {
                            onPlayerConfigLoaded.configLoaded(configId, null, response.getError());
                        }
                    }
                });
            }
        });
        requestsExecutor.queue(request.build());
    }
    
    public interface OnPlayerConfigLoaded {
        void configLoaded(int id, JsonObject uiConf, ErrorElement error);
    }

}
