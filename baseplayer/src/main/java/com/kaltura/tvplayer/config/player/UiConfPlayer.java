package com.kaltura.tvplayer.config.player;


import java.util.List;

// TODO: use public fields, no getters/setters

public class UiConfPlayer {

    private String audioLanguage;
    private String textLanguage;
    private Boolean useNativeTextTrack;
    private Integer volume;
    private String preload;
    private Boolean autoplay;
    private Boolean allowMutedAutoPlay;
    private Boolean muted;
    private Integer startTime;
    private List<StreamType> streamPriority;

    public String getAudioLanguage() {
        return audioLanguage;
    }

    public String getTextLanguage() {
        return textLanguage;
    }

    public Boolean getUseNativeTextTrack() {
        return useNativeTextTrack;
    }

    public Integer getVolume() {
        return volume;
    }

    public String getPreload() {
        return preload;
    }

    public Boolean getAutoplay() {
        return autoplay;
    }

    public Boolean getAllowMutedAutoPlay() {
        return allowMutedAutoPlay;
    }

    public Boolean getMuted() {
        return muted;
    }

    public Integer getStartTime() {
        return startTime;
    }

    public List<StreamType> getStreamPriority() {
        return streamPriority;
    }
}
