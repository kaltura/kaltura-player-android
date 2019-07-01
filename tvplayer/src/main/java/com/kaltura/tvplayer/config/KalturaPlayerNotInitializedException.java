package com.kaltura.tvplayer.config;

public class KalturaPlayerNotInitializedException extends Exception {

    private static final String KALTURA_PLAYER_INIT_EXCEPTION = "KalturaPlayer.initialize() was not called or hasn't finished.";

    public KalturaPlayerNotInitializedException(){
        super(KALTURA_PLAYER_INIT_EXCEPTION);
    }
}


