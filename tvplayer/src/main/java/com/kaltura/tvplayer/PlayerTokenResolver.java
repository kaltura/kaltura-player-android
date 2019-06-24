package com.kaltura.tvplayer;

import android.text.TextUtils;

import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.tvplayer.utils.MapTokenResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class PlayerTokenResolver extends MapTokenResolver {

    private List<String> entryKeys = new ArrayList<>();
    private List<String> globalKeys = new ArrayList<>();

    void update(PKMediaEntry mediaEntry, String ks) {

        removeAll(entryKeys);

        if (mediaEntry != null) {
            Map<String, String> mediaEntryMetadata = mediaEntry.getMetadata();
            if (mediaEntryMetadata != null) {
                for (Map.Entry<String, String> metadataEntry : mediaEntryMetadata.entrySet()) {
                    set(metadataEntry.getKey(), metadataEntry.getValue());
                    entryKeys.add(metadataEntry.getKey());
                }
            }

            if (mediaEntry.getMediaType() != null) {
                set("entryType", mediaEntry.getMediaType().name());
            }

            if (ks != null) {
                set("ks", ks);
            }

            if (TextUtils.isDigitsOnly(mediaEntry.getId())) /* OTT Media */ {
                set("entryId", isVeaidEntryIdInMetadata(mediaEntryMetadata) ? mediaEntryMetadata.get("entryId") : "unknown");
                set("assetId", mediaEntry.getId());
            } else {
                set("entryId", mediaEntry.getId());
            }
            set("entryName", mediaEntry.getName());
            if (mediaEntry.getMediaType() != null) {
                set("entryType", mediaEntry.getMediaType().name());
            }

            entryKeys.add("entryId");
            entryKeys.add("entryName");
            entryKeys.add("entryType");

            rebuild();
        }
    }

    private boolean isVeaidEntryIdInMetadata(Map<String, String> mediaEntryMetadata) {
        return mediaEntryMetadata != null && mediaEntryMetadata.containsKey("entryId") && !TextUtils.isEmpty(mediaEntryMetadata.get("entryId"));
    }

    void update(PlayerInitOptions initOptions) {

        removeAll(globalKeys);

        if (initOptions != null && initOptions.phoenixTVPlayerDMSParams != null) {
            if (initOptions.phoenixTVPlayerDMSParams.uiConfId != null) {
                set("uiConfId", String.valueOf(initOptions.phoenixTVPlayerDMSParams.uiConfId));
            }
            if (initOptions.partnerId != null) {
                set("partnerId", String.valueOf(initOptions.partnerId));
            }
            if (initOptions.phoenixTVPlayerDMSParams != null) {
                set("kavaPartnerId", String.valueOf(initOptions.phoenixTVPlayerDMSParams));
            }

            set("ks", (initOptions.ks != null) ? initOptions.ks : "");
            set("referrer", (initOptions.referrer != null) ? initOptions.referrer : "");

            globalKeys.add("uiConfId");
            globalKeys.add("partnerId");
            globalKeys.add("kavaPartnerId");
            globalKeys.add("ks");
            globalKeys.add("referrer");
        }

        rebuild();
    }

}
