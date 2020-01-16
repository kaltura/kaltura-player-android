package com.kaltura.tvplayer.playlist;

import com.kaltura.playkit.PKMediaEntry;

public class PKPlaylistMediaEntry {
    private int mediaIndex;
    private PKMediaEntry pkMediaEntry;

    public PKPlaylistMediaEntry(int mediaIndex, PKMediaEntry pkMediaEntry) {
        this.mediaIndex = mediaIndex;
        this.pkMediaEntry = pkMediaEntry;
    }

    public int getMediaIndex() {
        return mediaIndex;
    }

    public void setMediaIndex(int mediaIndex) {
        this.mediaIndex = mediaIndex;
    }

    public PKMediaEntry getPkMediaEntry() {
        return pkMediaEntry;
    }

    public void setPkMediaEntry(PKMediaEntry pkMediaEntry) {
        this.pkMediaEntry = pkMediaEntry;
    }
}
