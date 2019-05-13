package com.kaltura.tvplayer.config.player;


import java.util.List;

public class UiConfPlayer {

    public String audioLanguage;
    public String textLanguage;
    public Boolean useNativeTextTrack;
    public Integer volume;
    public String preload;
    public Boolean autoplay;
    public Boolean allowMutedAutoPlay;
    public Boolean muted;
    public Integer startTime;
    public List<StreamType> streamPriority;

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
