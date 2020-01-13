package com.kaltura.tvplayer.playlist;

import com.kaltura.playkit.providers.PlaylistMetadata;

import java.util.List;

public class BasicPlaylistOptions extends  PlaylistOptions {
    public PlaylistMetadata playlistMetadata;
    public List<PlaylistPKMediaEntry> playlistPKMediaEntryList;

    public BasicPlaylistOptions(PlaylistMetadata playlistMetadata, List<PlaylistPKMediaEntry> playlistPKMediaEntryList) {
        this.playlistMetadata = playlistMetadata;
        this.playlistPKMediaEntryList = playlistPKMediaEntryList;
    }

    public BasicPlaylistOptions() {}
}
