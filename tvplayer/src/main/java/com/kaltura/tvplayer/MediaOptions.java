package com.kaltura.tvplayer;

import com.kaltura.playkit.player.PKExternalSubtitle;
import com.kaltura.playkit.providers.MediaEntryProvider;
import com.kaltura.playkit.providers.ott.PhoenixMediaProvider;
import com.kaltura.playkit.providers.ovp.KalturaOvpMediaProvider;

import java.util.List;

public abstract class MediaOptions {
    public String ks;
    public Long startPosition;
    public List<PKExternalSubtitle> externalSubtitles;

    public abstract MediaEntryProvider buildMediaProvider(String serverUrl, int partnerId, String ks, String referrer);
}

