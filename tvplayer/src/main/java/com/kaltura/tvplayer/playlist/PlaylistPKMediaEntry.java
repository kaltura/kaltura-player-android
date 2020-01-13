package com.kaltura.tvplayer.playlist;

import com.kaltura.playkit.PKMediaEntry;

public class PlaylistPKMediaEntry {
    private PKMediaEntry pkMediaEntry;
    private CountDownOptions countDownOptions;

    public PlaylistPKMediaEntry(PKMediaEntry pkMediaEntry) {
        this.pkMediaEntry = pkMediaEntry;
    }

    public PlaylistPKMediaEntry(PKMediaEntry pkMediaEntry, CountDownOptions countDownOptions) {
        this.pkMediaEntry = pkMediaEntry;
        this.countDownOptions = countDownOptions;
    }

    public PKMediaEntry getPKMediaEntry() {
        return pkMediaEntry;
    }

    public CountDownOptions getCountDownOptions() {
        return countDownOptions;
    }
}
