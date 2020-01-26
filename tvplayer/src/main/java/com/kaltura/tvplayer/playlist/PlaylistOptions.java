package com.kaltura.tvplayer.playlist;

public abstract class PlaylistOptions {
    public boolean loopEnabled;
    public boolean shuffleEnabled;
    public boolean autoContinue = true;
    public int startIndex = 0;
    public CountDownOptions countDownOptions = new CountDownOptions();
}

