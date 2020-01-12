package com.kaltura.tvplayer.playlist;


import com.kaltura.playkit.PKPlaylist;
import com.kaltura.playkit.PKPlaylistMedia;

public interface PlaylistController {

    PKPlaylist getPlaylist();

    PKPlaylistMedia getCurrentPlaylistMedia();

    int getCurrentMediaIndex();

    void preloadNext();

    void playItem(int index);

    void playNext();

    void playPrev();

    void replay();

    boolean isMediaLoaded(int index);

    void loop(boolean mode);

    void shuffle(boolean mode);

    void setAutoContinue(boolean mode);

    void reset();

    void release();

    void setPlaylistOptions(PlaylistOptions playlistOptions);
}
