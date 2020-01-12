package com.kaltura.tvplayer.playlist;

import com.kaltura.playkit.PKMediaEntry;

class PKPlaylistMediaEntry {
    private Integer index;
    private PKMediaEntry pkMediaEntry;

    public PKPlaylistMediaEntry(Integer index, PKMediaEntry pkMediaEntry) {
        this.index = index;
        this.pkMediaEntry = pkMediaEntry;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public PKMediaEntry getPkMediaEntry() {
        return pkMediaEntry;
    }

    public void setPkMediaEntry(PKMediaEntry pkMediaEntry) {
        this.pkMediaEntry = pkMediaEntry;
    }
}
