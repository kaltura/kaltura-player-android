package com.kaltura.kalturaplayertestapp.models.ima;

public class UiElements {
    private boolean adAttribution;
    private boolean adCountDown;

    public UiElements(boolean adAttribution, boolean adCountDown) {
        this.adAttribution = adAttribution;
        this.adCountDown = adCountDown;
    }

    public boolean isAdAttribution() {
        return adAttribution;
    }

    public boolean isAdCountDown() {
        return adCountDown;
    }
}
