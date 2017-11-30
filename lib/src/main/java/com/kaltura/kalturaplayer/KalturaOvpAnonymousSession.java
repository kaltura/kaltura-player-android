package com.kaltura.kalturaplayer;

import com.kaltura.playkit.api.ovp.OvpConfigs;
import com.kaltura.playkit.api.ovp.OvpRequestBuilder;
import com.kaltura.playkit.api.ovp.services.OvpSessionService;

public class KalturaOvpAnonymousSession extends KalturaAnonymousSession {
    public static void start(int partnerId, final KSResultCallback callback) {
        start(partnerId, KalturaOvpPlayer.DEFAULT_SERVER_URL, callback);
    }

    public static void start(int partnerId, String serverUrl, final KSResultCallback callback) {
        final OvpRequestBuilder login = OvpSessionService.anonymousSession(serverUrl + OvpConfigs.ApiPrefix, partnerId);
        send(login, callback);
    }
}
