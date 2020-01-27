package com.kaltura.tvplayer;

import com.kaltura.playkit.player.PKExternalSubtitle;
import com.kaltura.playkit.providers.MediaEntryProvider;
import com.kaltura.tvplayer.playlist.CountDownOptions;

import java.util.List;

public abstract class MediaOptions {
    public String ks;
    public Long startPosition;
    public String referrer;
    public List<PKExternalSubtitle> externalSubtitles;
    public CountDownOptions countDownOptions;

    public abstract MediaEntryProvider buildMediaProvider(String serverUrl, int partnerId, String ks, String referrer);
}

