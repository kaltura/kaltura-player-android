package com.kaltura.tvplayer.ott;

import com.kaltura.playkit.providers.api.phoenix.APIDefines;
import com.kaltura.tvplayer.MediaOptions;

public class OTTMediaOptions extends MediaOptions {
    String assetId;
    APIDefines.KalturaAssetType assetType;
    APIDefines.PlaybackContextType contextType;
    String[] formats;
    String[] fileIds;

    public OTTMediaOptions setAssetId(String assetId) {
        this.assetId = assetId;
        return this;
    }

    public OTTMediaOptions setAssetType(APIDefines.KalturaAssetType assetType) {
        this.assetType = assetType;
        return this;
    }

    public OTTMediaOptions setContextType(APIDefines.PlaybackContextType contextType) {
        this.contextType = contextType;
        return this;
    }

    public OTTMediaOptions setFormats(String[] formats) {
        this.formats = formats;
        return this;
    }

    public OTTMediaOptions setFileIds(String[] fileIds) {
        this.fileIds = fileIds;
        return this;
    }
}
