package com.kaltura.kalturaplayer;

import com.kaltura.playkit.PKEvent;

public class KalturaBackendEvent implements PKEvent {

    private final Type type;
    
    public static KalturaBackendEvent ksExpired = new KalturaBackendEvent(Type.KSExpired);

    private KalturaBackendEvent(Type type) {
        this.type = type;
    }

    public enum Type {
        KSExpired
    }

    @Override
    public Enum eventType() {
        return type;
    }
}
