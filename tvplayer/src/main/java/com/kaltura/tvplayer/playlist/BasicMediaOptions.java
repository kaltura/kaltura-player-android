package com.kaltura.tvplayer.playlist;

import androidx.annotation.NonNull;

import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKPlaylistMedia;

public class BasicMediaOptions extends PKPlaylistMedia {

    private PKMediaEntry pkMediaEntry;
    private @NonNull CountDownOptions playlistCountDownOptions;

    public BasicMediaOptions(PKMediaEntry pkMediaEntry) {
        super.setId(pkMediaEntry.getId());
        super.setName(pkMediaEntry.getName());
        super.setMetadata(pkMediaEntry.getMetadata());
        super.setMsDuration(pkMediaEntry.getDuration());
        super.setType(pkMediaEntry.getMediaType());
        this.pkMediaEntry = pkMediaEntry;
        this.playlistCountDownOptions = new CountDownOptions();
    }

    public BasicMediaOptions(PKMediaEntry pkMediaEntry, @NonNull CountDownOptions playlistCountDownOptions) {
        this(pkMediaEntry);
        this.playlistCountDownOptions = playlistCountDownOptions;
    }

    public PKMediaEntry getPKMediaEntry() {
        return pkMediaEntry;
    }

    @NonNull
    public CountDownOptions getPlaylistCountDownOptions() {
        return playlistCountDownOptions;
    }
}
