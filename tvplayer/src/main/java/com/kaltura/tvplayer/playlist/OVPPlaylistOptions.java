package com.kaltura.tvplayer.playlist;

import com.kaltura.playkit.providers.PlaylistMetadata;
import com.kaltura.playkit.providers.PlaylistProvider;
import com.kaltura.playkit.providers.ovp.KalturaOvpPlaylistProvider;
import com.kaltura.playkit.providers.ovp.OVPMediaAsset;
import com.kaltura.tvplayer.OVPMediaOptions;

import java.util.ArrayList;
import java.util.List;

public class OVPPlaylistOptions extends ProviderPlaylistOptions {
    public PlaylistMetadata playlistMetadata;
    public List<OVPMediaOptions> ovpMediaOptionsList;

    public OVPPlaylistOptions(PlaylistMetadata playlistMetadata, List<OVPMediaOptions> ovpMediaOptionsList) {
        this.playlistMetadata = playlistMetadata;
        this.ovpMediaOptionsList = ovpMediaOptionsList;
    }

    public OVPPlaylistOptions() {}

    @Override
    public PlaylistProvider buildPlaylistProvider(String serverUrl, int partnerId, String ks) {
        List<OVPMediaAsset> ovpMediaAssetList = new ArrayList<>();
        for(OVPMediaOptions ovpMediaOptionsItem : ovpMediaOptionsList) {
            if (ovpMediaOptionsItem != null) {
                ovpMediaAssetList.add(ovpMediaOptionsItem.getOvpMediaAsset());
            }
        }
        return new KalturaOvpPlaylistProvider(serverUrl, partnerId, ks).setPlaylistParams(playlistMetadata != null ? playlistMetadata : new PlaylistMetadata(), ovpMediaAssetList);
    }
}

