package com.kaltura.tvplayer;

import com.kaltura.kalturaplayer.MediaOptions;
import com.kaltura.playkit.api.phoenix.APIDefines;

public class TVMediaOptions extends MediaOptions {
    String assetId;
    APIDefines.KalturaAssetType assetType;
    APIDefines.PlaybackContextType contextType;
    String[] formats;
    String[] fileIds;

    public TVMediaOptions setAssetId(String assetId) {
        this.assetId = assetId;
        return this;
    }

    public TVMediaOptions setAssetType(APIDefines.KalturaAssetType assetType) {
        this.assetType = assetType;
        return this;
    }

    public TVMediaOptions setContextType(APIDefines.PlaybackContextType contextType) {
        this.contextType = contextType;
        return this;
    }

    public TVMediaOptions setFormats(String[] formats) {
        this.formats = formats;
        return this;
    }

    public TVMediaOptions setFileIds(String[] fileIds) {
        this.fileIds = fileIds;
        return this;
    }
}
