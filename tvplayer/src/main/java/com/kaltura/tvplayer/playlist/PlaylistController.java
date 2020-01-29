package com.kaltura.tvplayer.playlist;


import com.kaltura.playkit.PKPlaylist;
import com.kaltura.playkit.PKPlaylistMedia;

public interface PlaylistController {

    PKPlaylist getPlaylist();

    PKPlaylistMedia getCurrentPlaylistMedia();

    int getCurrentMediaIndex();

    CountDownOptions getCurrentCountDownOptions();

    void disableCountDown();

    void preloadNext();

    void preloadItem(int index);

    void playItem(int index);

    void playItem(int index, boolean isAutoPlay);

    void playNext();

    void playPrev();

    void replay();

    boolean isMediaLoaded(int index);

    void loop(boolean mode);

    boolean isLoopEnabled();

    void shuffle(boolean mode);

    boolean isShuffleEnabled(boolean mode);

    void setAutoContinue(boolean mode);

    boolean isAutoContinueEnabled();

    void reset();

    void release();

    void setPlaylistOptions(PlaylistOptions playlistOptions);
}
