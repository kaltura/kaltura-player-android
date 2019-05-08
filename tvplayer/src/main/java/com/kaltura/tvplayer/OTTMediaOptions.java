package com.kaltura.tvplayer;

import android.support.annotation.NonNull;

import com.kaltura.playkit.providers.api.phoenix.APIDefines;
import com.kaltura.playkit.providers.ott.PhoenixMediaProvider;

public class OTTMediaOptions extends MediaOptions {
    public String assetId;
    public APIDefines.KalturaAssetType assetType;
    public APIDefines.PlaybackContextType contextType;
    public APIDefines.AssetReferenceType assetReferenceType;
    public String protocol;
    public String[] formats;
    public String[] fileIds;



}
