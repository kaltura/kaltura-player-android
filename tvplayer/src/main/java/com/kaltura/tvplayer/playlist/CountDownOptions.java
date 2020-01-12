package com.kaltura.tvplayer.playlist;

import com.kaltura.playkit.utils.Consts;

public class CountDownOptions {
    private long timeToShowMS;
    private long durationMS;
    private boolean shouldDisplay;
    private boolean eventSent;

    public CountDownOptions() {
        this.timeToShowMS = 10 * Consts.MILLISECONDS_MULTIPLIER;
        this.durationMS =  10 * Consts.MILLISECONDS_MULTIPLIER;
        this.shouldDisplay = true;
        eventSent = false;
    }

    public CountDownOptions(long timeToShowMS, long durationMS, boolean shouldDisplay) {
        if (timeToShowMS <= 0) {
            timeToShowMS = 10 * Consts.MILLISECONDS_MULTIPLIER;
            durationMS =  10 * Consts.MILLISECONDS_MULTIPLIER;
        }
        this.timeToShowMS = timeToShowMS;
        if (durationMS > timeToShowMS) {
            this.durationMS = timeToShowMS;
        } else {
            this.durationMS = durationMS;
        }
        this.shouldDisplay = shouldDisplay;
        eventSent = false;
    }

    public long getTimeToShowMS() {
        return timeToShowMS;
    }

    public void setTimeToShowMS(long timeToShowMS) {
        this.timeToShowMS = timeToShowMS;
    }

    public long getDurationMS() {
        return durationMS;
    }

    public void setDurationMS(long durationMS) {
        this.durationMS = durationMS;
    }

    public boolean shouldDisplay() {
        return shouldDisplay;
    }

    public void setShouldDisplay(boolean shouldDisplay) {
        this.shouldDisplay = shouldDisplay;
    }

    public boolean isEventSent() {
        return eventSent;
    }

    public void setEventSent(boolean eventSent) {
        this.eventSent = eventSent;
    }
}