package com.kaltura.tvplayer.playlist;

import com.kaltura.playkit.utils.Consts;

public class CountDownOptions {
    private long timeToShowMS;
    private long origTimeToShowMS;
    private long durationMS;
    private boolean shouldDisplay;
    private boolean eventSent;

    public CountDownOptions() {
        this.timeToShowMS = -1; // default - show 10 sec before end
        this.origTimeToShowMS = this.timeToShowMS;
        this.durationMS =  10 * Consts.MILLISECONDS_MULTIPLIER;
        this.shouldDisplay = true;
        eventSent = false;
    }

    public CountDownOptions(long timeToShowMS, long durationMS, boolean shouldDisplay) {
        if (timeToShowMS <= 0) {
            timeToShowMS = -1; // default - show from the end - DurationMS
        }
        this.timeToShowMS = timeToShowMS;
        this.origTimeToShowMS = timeToShowMS;
        if (timeToShowMS > 0 && durationMS > timeToShowMS) {
            this.durationMS = 10 * Consts.MILLISECONDS_MULTIPLIER;
        } else {
            this.durationMS = durationMS;
        }
        this.shouldDisplay = shouldDisplay;
        eventSent = false;
    }

    // will start from the enn - durationMS
    public CountDownOptions(long durationMS, boolean shouldDisplay) {
        this(-1, durationMS, shouldDisplay);
    }

    public long getTimeToShowMS() {
        return timeToShowMS;
    }

    public long getOrigTimeToShowMS() {
        return origTimeToShowMS;
    }

    public long getDurationMS() {
        return durationMS;
    }

    public boolean isEventSent() {
        return eventSent;
    }

    public boolean shouldDisplay() {
        return shouldDisplay;
    }

    void setTimeToShowMS(long timeToShowMS) {
        this.timeToShowMS = timeToShowMS;
    }

    void setDurationMS(long durationMS) {
        this.durationMS = durationMS;
    }

    void setShouldDisplay(boolean shouldDisplay) {
        this.shouldDisplay = shouldDisplay;
    }

    void setEventSent(boolean eventSent) {
        this.eventSent = eventSent;
    }
}
