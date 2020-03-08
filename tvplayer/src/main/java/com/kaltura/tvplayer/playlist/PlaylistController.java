package com.kaltura.tvplayer.playlist;


import androidx.annotation.Nullable;

import com.kaltura.playkit.PKPlaylist;
import com.kaltura.playkit.PKPlaylistMedia;

public interface PlaylistController {

    /**
     * getPlaylist - get current playlist data
     *
     * @return - PKPlaylist
     */
    PKPlaylist getPlaylist();

    /**
     * getPlaylistType - get current playlist type OVP_ID,OVP_LIST,OTT_LIST,BASIC_LIST
     *
     * @return - PKPlaylistType
     */
    PKPlaylistType getPlaylistType();

    /**
     * getCurrentPlaylistMedia - get current playlist media data.
     *
     * @return - PKPlaylistMedia
     */
    PKPlaylistMedia getCurrentPlaylistMedia();

    /**
     * getCurrentMediaIndex for current playing madia.
     *
     * @return - int
     */
    int getCurrentMediaIndex();

    /**
     * GetCurrentCountDownOptions for current madia.
     *
     * @return - CountDownOptions
     */
    CountDownOptions getCurrentCountDownOptions();

    /**
     * DisableCountDown for current madia.
     *
     */
    void disableCountDown();

    /**
     * Preload next item - will do the provider request for specific media to save network calls not relevant for basic player.
     *
     */
    void preloadNext();

    /**
     * PreloadItem by index - will do the provider request for specific media to save network calls not relevant for basic player.
     *
     * @param index - media index in playlist.
     */
    void preloadItem(int index);

    /**
     * PlayItem by index.
     *
     * @param index - media index in playlist.
     */
    void playItem(int index);

    /**
     * PlayItem by index and auto play configuration.
     *
     * @param index - media index in playlist.
     * @param isAutoPlay - isAutoPlay.
     */
    void playItem(int index, boolean isAutoPlay);

    /**
     * playNext - play the next media in the playlist.
     *
     */
    void playNext();

    /**
     * playPrev - play the previous media in the playlist.
     *
     */
    void playPrev();

    /**
     * replay - play the playlist from the first index.
     *
     */
    void replay();

    /**
     * isMediaLoaded - validation if media was fetched/played at least one time so the playback information is available.
     *
     * @param mediaId - media id in playlist.
     * @return - boolean
     */
    boolean isMediaLoaded(String mediaId);

    /**
     * setLoop - configure the controller to play the playlist again
     * when last playlist media is ended
     *
     * @param mode - enabled/disabled.
     */
    void setLoop(boolean mode);

    /**
     * isLoopEnabled - validation if playlist controller is configured to support setLoop mode.
     *
     * @return - boolean
     */
    boolean isLoopEnabled();

    /**
     * setAutoContinue - configure the controller to play the playlist in setAutoContinue mode
     *
     * @param mode - enabled/disabled.
     */
    void setAutoContinue(boolean mode);

    /**
     * isAutoContinueEnabled - validation if playlist controller is configured to support setAutoContinue mode.
     *
     * @return - boolean
     */
    boolean isAutoContinueEnabled();

    /**
     * setRecoverOnError - ignore media playback errors and continue
     *
     * @param mode - enabled/disabled.
     */
    void setRecoverOnError(boolean mode);

    /**
     * isRecoverOnError - validation if playlist controller should recover on error.
     *
     * @return - boolean
     */
    boolean isRecoverOnError();

    /**
     * release - should e called if we want to reuse the player for single media or for loading new play list.
     */
    void release();

    /**
     * setPlaylistOptions - configure the controller with the initial playlist options
     *
     * @param playlistOptions - playlist initial configuration.
     */
    void setPlaylistOptions(PlaylistOptions playlistOptions);

    /**
     * setPlaylistCountDownOptions - update the current playlist countdown configuration
     *
     * @param playlistCountDownOptions - playlist countdown.
     */
    void setPlaylistCountDownOptions(@Nullable CountDownOptions playlistCountDownOptions);
}
