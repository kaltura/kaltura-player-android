package com.kaltura.tvplayer;

import android.content.Context;

import androidx.annotation.Nullable;

import com.kaltura.playkit.PKLog;

public class KalturaOvpPlayer extends KalturaPlayerBase {

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

    public static void initializeOVP(Context context, int partnerId, @Nullable String serverUrl) {

        PlayerConfigManager.retrieve(context, Type.ovp, partnerId, serverUrl, (config, error, freshness) -> {
            if (error != null) {
                log.e("initialize KalturaPlayerType failed");
            } else {
                playerConfigRetrieved = true;
            }
        });
    }
}