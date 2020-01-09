package com.kaltura.tvplayer.playlist;

import com.kaltura.playkit.providers.PlaylistMetadata;
import com.kaltura.playkit.providers.PlaylistProvider;

public abstract class PlaylistOptions {
    public String ks;
    public boolean useApiCaptions;
    public boolean loopEnabled;
    public boolean shuffleEnabled;

    public abstract PlaylistProvider buildPlaylistProvider(String serverUrl, int partnerId, String ks);
}

