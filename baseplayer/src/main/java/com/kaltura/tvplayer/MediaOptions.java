package com.kaltura.tvplayer;


import com.kaltura.netkit.utils.ErrorElement;
import com.kaltura.playkit.PKMediaEntry;

public abstract class MediaOptions {
    public String ks;
    double startPosition;

    public MediaOptions setKS(String ks) {
        this.ks = ks;
        return this;
    }

    public MediaOptions setStartPosition(double startPosition) {
        this.startPosition = startPosition;
        return this;
    }

    public abstract void loadMedia(String serverUrl, int partnerId, OnEntryLoadListener listener);

    public interface OnEntryLoadListener {
        void onEntryLoadComplete(PKMediaEntry entry, ErrorElement error);
    }
}

