package com.kaltura.tvplayer;

import androidx.annotation.Nullable;

import com.kaltura.playkit.player.PKExternalSubtitle;
import com.kaltura.playkit.providers.MediaEntryProvider;
import com.kaltura.tvplayer.playlist.CountDownOptions;

import java.util.List;

public abstract class MediaOptions {
    public Long startPosition;
    public List<PKExternalSubtitle> externalSubtitles;
    public String externalVttThumbnailUrl;
    public @Nullable CountDownOptions playlistCountDownOptions;
    
    public abstract MediaEntryProvider buildMediaProvider(String serverUrl, int partnerId);
}

