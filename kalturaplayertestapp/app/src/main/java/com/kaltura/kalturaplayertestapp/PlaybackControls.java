package com.kaltura.kalturaplayertestapp;

/**
 * Created by gilad.nadav on 3/18/18.
 */

public interface PlaybackControls {
    void handleContainerClick();
    void showControls(int visability);
    void setContentPlayerState(Enum playerState);
    void setAdPlayerState(Enum playerState);
}
