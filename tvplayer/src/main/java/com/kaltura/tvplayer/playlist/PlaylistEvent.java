package com.kaltura.tvplayer.playlist;


import androidx.annotation.NonNull;
import com.kaltura.playkit.PKError;
import com.kaltura.playkit.PKEvent;



import java.util.List;

@SuppressWarnings({"unused", "WeakerAccess"})
public class PlaylistEvent implements PKEvent {

    public final PlaylistEvent.Type type;

//    public static final Class<com.kaltura.playkit.PlayerEvent.Error> error = com.kaltura.playkit.PlayerEvent.Error.class;
//    public static final Class<com.kaltura.playkit.PlayerEvent.StateChanged> stateChanged = com.kaltura.playkit.PlayerEvent.StateChanged.class;
//
//    public static final com.kaltura.playkit.PlayerEvent.Type canPlay = com.kaltura.playkit.PlayerEvent.Type.CAN_PLAY;



    public PlaylistEvent (Type type) {
        this.type = type;
    }

    public static class Generic extends PlaylistEvent {
        public Generic(Type type) {
            super(type);
        }
    }


//
//    public static class DurationChanged extends com.kaltura.playkit.PlayerEvent {
//
//        public final long duration;
//
//        public DurationChanged(long duration) {
//            super(Type.DURATION_CHANGE);
//            this.duration = duration;
//        }
//    }


//
//    public static class Error extends com.kaltura.playkit.PlayerEvent {
//
//        public final PKError error;
//
//        public Error(PKError error) {
//            super(Type.ERROR);
//            this.error = error;
//        }
//    }



    public enum Type {
        PLAYLIST_LOADED,
        PLAYLIST_STARTED,
        PLAYLIST_ENDED,
        PLAYLIST_MEDIA_SELECTED, //mediaId + index
        PLAYLIST_MEDIA_COUNT_DOWN, //  mediaId + index + counrdown
        PLAYLIST_MEDIA_ENDED, // mediaId + index
        PLAYLIST_MEDIA_ERROR,// mediaId + index // index is giving null media
        PLAYLIST_LOOP_STATE_CHANGED,
        PLAYLIST_SUFFLE_STATE_CHANGED,
        PLAYLIST_ERROR;
    }

    @Override
    public Enum eventType() {
        return this.type;
    }


}
