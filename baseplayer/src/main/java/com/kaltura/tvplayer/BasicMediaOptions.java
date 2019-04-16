package com.kaltura.tvplayer;

import com.kaltura.playkit.PKMediaEntry;

public class BasicMediaOptions  extends MediaOptions{

    @Override
    public BasicMediaOptions setStartPosition(double startPosition) {
        super.setStartPosition(startPosition);
        return this;
    }
}

