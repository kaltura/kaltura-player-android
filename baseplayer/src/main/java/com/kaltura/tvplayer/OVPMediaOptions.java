package com.kaltura.tvplayer;

public class OVPMediaOptions extends MediaOptions {
    String entryId;

    public OVPMediaOptions setEntryId(String entryId) {
        this.entryId = entryId;
        return this;
    }

    @Override
    public OVPMediaOptions setKS(String ks) {
        super.setKS(ks);
        return this;
    }

    @Override
    public OVPMediaOptions setStartPosition(double startPosition) {
        super.setStartPosition(startPosition);
        return this;
    }
}

