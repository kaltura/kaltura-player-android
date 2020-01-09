package com.kaltura.tvplayer;

import android.os.Parcel;

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
}
