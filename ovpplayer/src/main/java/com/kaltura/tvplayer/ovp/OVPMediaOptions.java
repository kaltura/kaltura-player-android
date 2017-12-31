package com.kaltura.tvplayer.ovp;

import com.kaltura.tvplayer.MediaOptions;

public class OVPMediaOptions extends MediaOptions {
    String entryId;

    public OVPMediaOptions setEntryId(String entryId) {
        this.entryId = entryId;
        return this;
    }
}

