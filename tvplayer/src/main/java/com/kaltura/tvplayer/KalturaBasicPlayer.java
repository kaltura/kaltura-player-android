package com.kaltura.tvplayer;

import android.content.Context;

import com.kaltura.playkit.PKLog;

public class KalturaBasicPlayer extends KalturaPlayer {

    private static final PKLog log = PKLog.get("KalturaBasicPlayer");

    private KalturaBasicPlayer(Context context, PlayerInitOptions initOptions) {
        super(context, Type.basic, initOptions);
    }

    public static KalturaBasicPlayer create(Context context, PlayerInitOptions initOptions) {
        KalturaPlayer.initializeDrm(context);
        return new KalturaBasicPlayer(context, initOptions);
    }
}
