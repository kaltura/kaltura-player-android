package com.kaltura.tvplayer;

import android.content.Context;

import androidx.annotation.Nullable;

import com.kaltura.playkit.PKLog;

public class KalturaOvpPlayer extends KalturaPlayer {

    private static final PKLog log = PKLog.get("KalturaOvpPlayer");

    private KalturaOvpPlayer(Context context, PlayerInitOptions initOptions) {
        super(context, Type.ovp, initOptions);
    }

    public static KalturaOvpPlayer create(Context context, PlayerInitOptions initOptions) {
        if (playerConfigRetrieved) {
            initOptions.setTVPlayerParams(PlayerConfigManager.retrieve(Type.ovp, initOptions.partnerId));
        }
        return new KalturaOvpPlayer(context, initOptions);
    }

    public static void initialize(Context context, int partnerId, @Nullable String serverUrl) {
        KalturaPlayer.initializeDrm(context);
        PlayerConfigManager.retrieve(context, Type.ovp, partnerId, serverUrl, (config, error, freshness) -> {
            if (error != null) {
                log.e("initialize KalturaPlayerType failed");
            } else {
                KalturaPlayer.playerConfigRetrieved = true;
            }
        });
    }
}
