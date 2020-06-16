package com.kaltura.tvplayer;


import androidx.annotation.Nullable;

import com.kaltura.netkit.utils.ErrorElement;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKPlaylist;

@SuppressWarnings({"unused", "WeakerAccess"})
public class KalturaPlayerEvent implements PKEvent {

    public static final Class<LoadMediaError> loadMediaError = LoadMediaError.class;


    public final Type type;

    public KalturaPlayerEvent(KalturaPlayerEvent.Type type) {
        this.type = type;
    }

    public static class Generic extends KalturaPlayerEvent {
        public Generic(Type type) {
            super(type);
        }
    }

    public static class LoadMediaError extends KalturaPlayerEvent {

        public final ErrorElement loadError;

        public LoadMediaError(ErrorElement loadError) {
            super(Type.LOAD_MEDIA_ERROR);
            this.loadError = loadError;
        }
    }



    public enum Type {
        LOAD_MEDIA_ERROR
    }

    @Override
    public Enum eventType() {
        return this.type;
    }


}