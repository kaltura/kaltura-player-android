package com.kaltura.tvplayer.playlist;

import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKPlaylistMedia;

public class BasicMediaOptions extends PKPlaylistMedia {

    private PKMediaEntry pkMediaEntry;
    private CountDownOptions countDownOptions;

    public BasicMediaOptions(PKMediaEntry pkMediaEntry) {
        super.setId(pkMediaEntry.getId());
        super.setName(pkMediaEntry.getName());
        super.setMetadata(pkMediaEntry.getMetadata());
        super.setMsDuration(pkMediaEntry.getDuration());
        super.setType(pkMediaEntry.getMediaType());
        this.pkMediaEntry = pkMediaEntry;
        this.countDownOptions = new CountDownOptions();
    }

    public BasicMediaOptions(PKMediaEntry pkMediaEntry, CountDownOptions countDownOptions) {
        this(pkMediaEntry);
        this.countDownOptions = countDownOptions;
    }

    public PKMediaEntry getPKMediaEntry() {
        return pkMediaEntry;
    }

    public CountDownOptions getCountDownOptions() {
        return countDownOptions;
    }
}
