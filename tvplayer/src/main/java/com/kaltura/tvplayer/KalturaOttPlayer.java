package com.kaltura.tvplayer;

import android.content.Context;
import androidx.annotation.NonNull;
import com.kaltura.playkit.PKLog;

public class KalturaOttPlayer extends KalturaPlayer {

    private static final PKLog log = PKLog.get("KalturaOttPlayer");

    private KalturaOttPlayer(Context context, PlayerInitOptions initOptions) {
        super(context, Type.ott, initOptions);
    }

    public static KalturaOttPlayer create(Context context, PlayerInitOptions initOptions) {
        if (playerConfigRetrieved) {
            initOptions.setTVPlayerParams(PlayerConfigManager.retrieve(Type.ott, initOptions.partnerId));
        }
        return new KalturaOttPlayer(context, initOptions);
    }

    public static void initialize(Context context, int partnerId, @NonNull String serverUrl) {
        KalturaPlayer.initializeDrm(context);
        PlayerConfigManager.retrieve(context, Type.ott, partnerId, serverUrl, (config, error, freshness) -> {
            if (error != null) {
                log.e("initialize KalturaPlayerType failed");
            } else {
                KalturaPlayer.playerConfigRetrieved = true;
            }
        });
    }
}
