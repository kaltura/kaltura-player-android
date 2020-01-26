package com.kaltura.tvplayer.playlist;

import com.kaltura.playkit.providers.PlaylistMetadata;

import java.util.List;

public class BasicPlaylistOptions extends  PlaylistOptions {
    public PlaylistMetadata playlistMetadata;
    public List<BasicMediaOptions> basicMediaOptionsList;

    public BasicPlaylistOptions(PlaylistMetadata playlistMetadata, List<BasicMediaOptions> basicMediaOptionsList) {
        this.playlistMetadata = playlistMetadata;
        this.basicMediaOptionsList = basicMediaOptionsList;
    }

    public BasicPlaylistOptions() {}
}
