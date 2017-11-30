package com.kaltura.kalturaplayer;

import com.kaltura.playkit.api.phoenix.PhoenixRequestBuilder;
import com.kaltura.playkit.api.phoenix.services.OttUserService;

public class KalturaPhoenixAnonymousSession extends KalturaAnonymousSession {

    public static void start(int partnerId, String udid, String serverUrl, final KSResultCallback callback) {
        final PhoenixRequestBuilder login = OttUserService.anonymousLogin(serverUrl, partnerId, udid);
        send(login, callback);
    }

    public static void start(int partnerId, String udid, KSResultCallback callback) {
        start(partnerId, udid, KalturaPhoenixPlayer.DEFAULT_SERVER_URL, callback);
    }
}


