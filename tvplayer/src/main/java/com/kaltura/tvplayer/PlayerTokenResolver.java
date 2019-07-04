package com.kaltura.tvplayer;

import android.text.TextUtils;

import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.tvplayer.config.PhoenixTVPlayerParams;
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
                set("entryId", (mediaEntryMetadata != null && !TextUtils.isEmpty(mediaEntryMetadata.get("entryId"))) ? mediaEntryMetadata.get("entryId") : "unknown");
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

    void update(PlayerInitOptions initOptions) {

        removeAll(globalKeys);

        if (initOptions != null && initOptions.tvPlayerParams != null) {
            if (initOptions.tvPlayerParams.uiConfId != null) {
                set("uiConfId", String.valueOf(initOptions.tvPlayerParams.uiConfId));
            }
            if (initOptions.tvPlayerParams.partnerId != null) {
                set("partnerId", String.valueOf(initOptions.tvPlayerParams.partnerId));
            }
            if (initOptions.tvPlayerParams instanceof PhoenixTVPlayerParams) {
                Integer kavaPartnerId = ((PhoenixTVPlayerParams)initOptions.tvPlayerParams).ovpPartnerId;
                if (kavaPartnerId != null) {
                    set("kavaPartnerId", String.valueOf(kavaPartnerId));
                }
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
