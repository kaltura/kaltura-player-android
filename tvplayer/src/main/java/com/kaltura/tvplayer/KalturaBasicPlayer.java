package com.kaltura.tvplayer;

import android.content.Context;

import com.kaltura.playkit.PKLog;

public class KalturaBasicPlayer extends KalturaPlayerBase {

    private static final PKLog log = PKLog.get("KalturaBasicPlayer");

    private KalturaBasicPlayer(Context context, PlayerInitOptions initOptions) {
        super(context, Type.basic, initOptions);
    }

    public static KalturaBasicPlayer create(Context context, PlayerInitOptions initOptions) {
        return new KalturaBasicPlayer(context, initOptions);
    }
}