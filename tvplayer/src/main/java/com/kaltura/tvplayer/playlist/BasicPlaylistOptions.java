package com.kaltura.tvplayer.playlist;

import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.providers.PlaylistMetadata;
import com.kaltura.tvplayer.OTTMediaOptions;

import java.util.List;

public class BasicPlaylistOptions extends  PlaylistOptions{
    public PlaylistMetadata playlistMetadata;
    public List<PKMediaEntry> pkMediaEntryList;

    public BasicPlaylistOptions(PlaylistMetadata playlistMetadata, List<PKMediaEntry> pkMediaEntryList) {
        this.playlistMetadata = playlistMetadata;
        this.pkMediaEntryList = pkMediaEntryList;
    }

    public BasicPlaylistOptions() {}
}
