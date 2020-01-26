package com.kaltura.tvplayer.playlist;

import com.kaltura.playkit.PKMediaEntry;

public class BasicMediaOptions {
    private int mediaIndex;
    private PKMediaEntry pkMediaEntry;
    private CountDownOptions countDownOptions;

    public BasicMediaOptions(int mediaIndex, PKMediaEntry pkMediaEntry) {
        this.mediaIndex = mediaIndex;
        this.pkMediaEntry = pkMediaEntry;
        this.countDownOptions = new CountDownOptions();
    }

    public BasicMediaOptions(int mediaIndex, PKMediaEntry pkMediaEntry, CountDownOptions countDownOptions) {
        this.mediaIndex = mediaIndex;
        this.pkMediaEntry = pkMediaEntry;
        this.countDownOptions = countDownOptions;
    }

    public int getMediaIndex() {
        return mediaIndex;
    }

    public void setMediaIndex(int mediaIndex) {
        this.mediaIndex = mediaIndex;
    }

    public PKMediaEntry getPKMediaEntry() {
        return pkMediaEntry;
    }

    public CountDownOptions getCountDownOptions() {
        return countDownOptions;
    }
}
