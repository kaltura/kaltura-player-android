package com.kaltura.tvplayer.playlist;

import com.kaltura.playkit.providers.PlaylistMetadata;
import com.kaltura.playkit.providers.PlaylistProvider;
import com.kaltura.playkit.providers.ott.OTTMediaAsset;
import com.kaltura.playkit.providers.ott.PhoenixPlaylistProvider;
import com.kaltura.tvplayer.OTTMediaOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OTTPlaylistOptions extends ProviderPlaylistOptions {
    public PlaylistMetadata playlistMetadata;
    public List<OTTMediaOptions> ottMediaOptionsList;

    public OTTPlaylistOptions(PlaylistMetadata playlistMetadata, List<OTTMediaOptions> ottMediaOptionsList) {
        this.playlistMetadata = playlistMetadata;
        this.ottMediaOptionsList = ottMediaOptionsList;
    }

    public OTTPlaylistOptions() {}

    @Override
    public PlaylistProvider buildPlaylistProvider(String serverUrl, int partnerId, String ks) {
        List<OTTMediaAsset> ottMediaAssetList = new ArrayList<>();
        for(OTTMediaOptions ottMediaOptionsItem : ottMediaOptionsList) {
            if (ottMediaOptionsItem != null) {
                ottMediaAssetList.add(ottMediaOptionsItem.getOttMediaAsset());
            }
        }
        return new PhoenixPlaylistProvider(serverUrl, partnerId, ks).setPlaylistParams(playlistMetadata != null ? playlistMetadata : new PlaylistMetadata(), ottMediaAssetList);
    }
}

