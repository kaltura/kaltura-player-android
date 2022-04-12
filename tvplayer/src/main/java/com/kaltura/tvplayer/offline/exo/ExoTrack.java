package com.kaltura.tvplayer.offline.exo;

import com.kaltura.dtg.DownloadItem;


public class ExoTrack implements DownloadItem.Track {

    private DownloadItem.TrackType type;
    
    private String uniqueId;
    private String language;
    private long bitrate;
    private int width;
    private int height;
    private String codecs;

    public ExoTrack(DownloadItem.TrackType type, String uniqueId, long bitrate, int width, int height, String codecs, String language) {
        this.type = type;
        this.uniqueId = uniqueId;
        this.bitrate = bitrate;
        this.codecs = codecs;
        this.width = width;
        this.height = height;
        this.language = language;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    @Override
    public DownloadItem.TrackType getType() {
        return null;
    }

    @Override
    public String getLanguage() {
        return language;
    }

    @Override
    public long getBitrate() {
        return bitrate;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public String getCodecs() {
        return codecs != null ? codecs : "avc1";
    }

    @Override
    public String getAudioGroupId() {
        return null;
    }
}
