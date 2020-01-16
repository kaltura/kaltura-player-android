package com.kaltura.tvplayer.playlist;

import com.kaltura.playkit.PKPlaylist;

import java.util.List;

public class PKBasicPlaylist extends PKPlaylist {
    private List<PKPlaylistMediaEntry> playlistMediaEntryList;

    public PKBasicPlaylist setPlaylistMediaEntryList(List<PKPlaylistMediaEntry> playlistMediaEntryList) {
        this.playlistMediaEntryList = playlistMediaEntryList;
        return this;
    }

    public List<PKPlaylistMediaEntry> getPlaylistMediaEntryList() {
        return playlistMediaEntryList;
    }

    @Override
    public int getMediaListSize() {
        return playlistMediaEntryList != null ? playlistMediaEntryList.size() : 0;
    }
}
