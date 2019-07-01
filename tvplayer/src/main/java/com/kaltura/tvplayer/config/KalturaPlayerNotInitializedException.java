package com.kaltura.tvplayer.config;

public class KalturaPlayerNotInitializedException extends Exception {

    public KalturaPlayerNotInitializedException(){
        super("KalturaPlayer.initialize was not triggered.");
    }
}

