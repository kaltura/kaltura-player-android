package com.kaltura.tvplayer;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.kaltura.playkit.PKLog;

public class KalturaOvpPlayer extends KalturaPlayer {

    private static final PKLog log = PKLog.get("KalturaOvpPlayer");

    private KalturaOvpPlayer(Context context, PlayerInitOptions initOptions) {
        super(context, Type.ovp, initOptions);
    }

    public static KalturaOvpPlayer create(@NonNull Context context, @NonNull PlayerInitOptions initOptions) {
        if (playerConfigRetrieved && initOptions != null && initOptions.partnerId != null) {
            initOptions.setTVPlayerParams(PlayerConfigManager.retrieve(Type.ovp, initOptions.partnerId));
        }
        return new KalturaOvpPlayer(context, initOptions);
    }

    public static void initialize(@NonNull Context context, int partnerId, @NonNull String serverUrl) {
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
