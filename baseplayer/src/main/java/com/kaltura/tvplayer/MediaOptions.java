package com.kaltura.tvplayer;


import com.kaltura.playkit.PKMediaFormat;

public class MediaOptions {
    private String ks;
    private double startPosition;

    public MediaOptions setKS(String ks) {
        this.ks = ks;
        return this;
    }

    public MediaOptions setStartPosition(double startPosition) {
        this.startPosition = startPosition;
        return this;
    }

    public String getKs() {
        return ks;
    }

    public double getStartPosition() {
        return startPosition;
    }
}

