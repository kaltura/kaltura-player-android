package com.kaltura.tvplayer.playlist;

import com.kaltura.playkit.providers.PlaylistProvider;

public abstract class ProviderPlaylistOptions extends PlaylistOptions{
    public String ks;

    public abstract PlaylistProvider buildPlaylistProvider(String serverUrl, int partnerId, String ks);
}

