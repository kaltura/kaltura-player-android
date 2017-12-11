package com.kaltura.ovpplayer;

import com.kaltura.kalturaplayer.MediaOptions;

public class OVPMediaOptions extends MediaOptions {
    String entryId;

    public OVPMediaOptions setEntryId(String entryId) {
        this.entryId = entryId;
        return this;
    }
}

