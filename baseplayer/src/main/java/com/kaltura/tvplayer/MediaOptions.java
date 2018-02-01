package com.kaltura.tvplayer;


import com.kaltura.playkit.PKMediaFormat;

public class MediaOptions {
    private  String ks;
    private double startPosition;
    private PKMediaFormat preferredMediaFormat;

    public MediaOptions setKS(String ks) {
        this.ks = ks;
        return this;
    }

    public MediaOptions setStartPosition(double startPosition) {
        this.startPosition = startPosition;
        return this;
    }

    public MediaOptions setPreferredMediaFormat(String preferredMeidaFormat, PKMediaFormat initOptionsPreferredMediaFormat) {
        if (preferredMeidaFormat != null) {
            this.preferredMediaFormat = PKMediaFormat.valueOf(preferredMeidaFormat);
        } else {
            if (initOptionsPreferredMediaFormat == null) {
                this.preferredMediaFormat = PKMediaFormat.dash;
            } else {
                this.preferredMediaFormat = initOptionsPreferredMediaFormat;
            }
        }
        return this;
    }

    public String getKs() {
        return ks;
    }

    public double getStartPosition() {
        return startPosition;
    }

    public PKMediaFormat getPreferredMediaFormat() {
        return preferredMediaFormat;
    }
}

