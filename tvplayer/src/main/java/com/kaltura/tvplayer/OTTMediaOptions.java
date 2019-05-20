package com.kaltura.tvplayer;

import com.kaltura.playkit.providers.api.phoenix.APIDefines;
import com.kaltura.playkit.providers.ott.PhoenixMediaProvider;

public class OTTMediaOptions extends MediaOptions {
    public static final String HTTP = PhoenixMediaProvider.HttpProtocol.Http;;
    public static final String HTTPS = PhoenixMediaProvider.HttpProtocol.Https;

    public String assetId;
    public APIDefines.KalturaAssetType assetType;
    public APIDefines.PlaybackContextType contextType;
    public APIDefines.AssetReferenceType assetReferenceType;
    public String protocol = HTTPS;
    public String[] formats;
    public String[] fileIds;
}
