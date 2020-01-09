package com.kaltura.tvplayer.playlist;

import com.kaltura.playkit.providers.PlaylistMetadata;
import com.kaltura.playkit.providers.PlaylistProvider;
import com.kaltura.playkit.providers.ovp.KalturaOvpPlaylistProvider;
import com.kaltura.playkit.providers.ovp.OVPMediaAsset;
import com.kaltura.tvplayer.OVPMediaOptions;

import java.util.ArrayList;
import java.util.List;

public class OVPPlaylistIdOptions extends ProviderPlaylistOptions {
    public String playlistId;

    public OVPPlaylistIdOptions(String playlistId) {
        this.playlistId = playlistId;
    }

    public OVPPlaylistIdOptions() {}

    @Override
    public PlaylistProvider buildPlaylistProvider(String serverUrl, int partnerId, String ks) {
        return new KalturaOvpPlaylistProvider(serverUrl, partnerId, ks).setPlaylistId(playlistId);
    }
}

