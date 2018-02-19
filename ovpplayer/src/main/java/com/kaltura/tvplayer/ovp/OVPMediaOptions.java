package com.kaltura.tvplayer.ovp;

import com.kaltura.netkit.connect.response.ResultElement;
import com.kaltura.playkit.MediaEntryProvider;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.api.ovp.SimpleOvpSessionProvider;
import com.kaltura.playkit.mediaproviders.base.OnMediaLoadCompletion;
import com.kaltura.playkit.mediaproviders.ovp.KalturaOvpMediaProvider;
import com.kaltura.tvplayer.MediaOptions;

public class OVPMediaOptions extends MediaOptions {
    String entryId;

    public OVPMediaOptions setEntryId(String entryId) {
        this.entryId = entryId;
        return this;
    }

    @Override
    public void loadMedia(String serverUrl, int partnerId, final OnEntryLoadListener listener) {
        MediaEntryProvider provider = new KalturaOvpMediaProvider()
                .setSessionProvider(new SimpleOvpSessionProvider(serverUrl, partnerId, ks)).setEntryId(entryId);

        provider.load(new OnMediaLoadCompletion() {
            @Override
            public void onComplete(ResultElement<PKMediaEntry> response) {
                listener.onEntryLoadComplete(response.getResponse(), response.getError());
            }
        });
    }
}

