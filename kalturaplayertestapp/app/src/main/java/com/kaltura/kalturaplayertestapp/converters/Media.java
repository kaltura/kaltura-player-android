package com.kaltura.kalturaplayertestapp.converters;

import com.kaltura.playkit.providers.api.phoenix.APIDefines;

/**
 * Created by gilad.nadav on 1/24/18.
 */

public class Media {
    private String entryId;   // ovp
    private String ks;        // ovp or ott
    private String assetId;   // ott
    private String format;    // ott
    private Integer fileId;   // ott
    private String assetType; // ott
    private String playbackContextType; // ott

    public Media() {}

    public String getEntryId() {
        return entryId;
    }

    public String getKs() {
        return ks;
    }

    public void setKs(String ks) {
        this.ks = ks;
    }

    public void setEntryId(String entryId) {
        this.entryId = entryId;
    }


    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Integer getFileId() {
        return fileId;
    }

    public void setFileId(Integer fileId) {
        this.fileId = fileId;
    }

    public APIDefines.KalturaAssetType getAssetType() {
        if (assetType == null) {
            return null;
        }

        if (APIDefines.KalturaAssetType.Media.value.equals(assetType.toLowerCase())) {
            return APIDefines.KalturaAssetType.Media;
        } else if (APIDefines.KalturaAssetType.Epg.value.equals(assetType.toLowerCase())) {
            return APIDefines.KalturaAssetType.Epg;
        } else if (APIDefines.KalturaAssetType.Recording.value.equals(assetType.toLowerCase())) {
            return APIDefines.KalturaAssetType.Recording;
        }
        return null;
    }

    public void setAssetType(String assetType) {
        this.assetType = assetType;
    }

    public APIDefines.PlaybackContextType getPlaybackContextType() {
        if (playbackContextType == null) {
            return null;
        }

        if (APIDefines.PlaybackContextType.Playback.value.toLowerCase().equals(playbackContextType.toLowerCase())) {
            return APIDefines.PlaybackContextType.Playback;
        } else if (APIDefines.PlaybackContextType.StartOver.value.toLowerCase().equals(playbackContextType.toLowerCase())) {
            return APIDefines.PlaybackContextType.StartOver;
        } else if (APIDefines.PlaybackContextType.Trailer.value.toLowerCase().equals(playbackContextType.toLowerCase())) {
            return APIDefines.PlaybackContextType.Trailer;
        } else if (APIDefines.PlaybackContextType.Catchup.value.toLowerCase().equals(playbackContextType.toLowerCase())) {
            return APIDefines.PlaybackContextType.Catchup;
        }

        return null;
    }

    public void setPlaybackContextType(String playbackContextType) {
        this.playbackContextType = playbackContextType;
    }
}

