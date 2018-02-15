package com.kaltura.tvplayer.config.ima;

/**
 * Created by gilad.nadav on 2/8/18.
 */

public class SdkSettings {

    private int numRedirects = 4;
    private boolean autoPlayAdBreaks = true;
    private boolean debugMode = false;
    private transient String language = "en";

    public int getNumRedirects() {
        return numRedirects;
    }

    public boolean autoPlayAdBreaks() {
        return autoPlayAdBreaks;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public String getLanguage() {
        return language;
    }
}
