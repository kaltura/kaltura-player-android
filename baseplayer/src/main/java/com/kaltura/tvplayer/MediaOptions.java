package com.kaltura.tvplayer;


public class MediaOptions {
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
}

