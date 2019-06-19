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

    void update(PKMediaEntry mediaEntry) {

        removeAll(entryKeys);

        if (mediaEntry != null) {
            if (mediaEntry.getMetadata() != null) {
                for (Map.Entry<String, String> metadataEntry : mediaEntry.getMetadata().entrySet()) {
                    set(metadataEntry.getKey(), metadataEntry.getValue());
                    entryKeys.add(metadataEntry.getKey());
                }
            }

            if (TextUtils.isDigitsOnly(mediaEntry.getId())) /* OTT Media */ {
                set("entryId", mediaEntry.getMetadata().containsKey("entryId") ? mediaEntry.getMetadata().get("entryId") : "unknown");
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

        if (initOptions != null) {
            if (initOptions.uiConfId != null) {
                set("uiConfId", String.valueOf(initOptions.uiConfId));
            }
            if (initOptions.partnerId != null) {
                set("partnerId", String.valueOf(initOptions.partnerId));
            }
            if (initOptions.partnerId != null) {
                set("kavaPartnerId", String.valueOf(initOptions.uiConfPartnerId));
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
