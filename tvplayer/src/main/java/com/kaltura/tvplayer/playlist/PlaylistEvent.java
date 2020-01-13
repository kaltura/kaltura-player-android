package com.kaltura.tvplayer.playlist;

import com.kaltura.netkit.utils.ErrorElement;
import com.kaltura.playkit.PKError;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKPlaylist;

@SuppressWarnings({"unused", "WeakerAccess"})
public class PlaylistEvent implements PKEvent {


    public static final Class<PlaylistLoaded> playListLoaded = PlaylistLoaded.class;
    public static final Class<PlaylistStarted> playListStarted = PlaylistStarted.class;
    public static final Class<PlaylistEnded> playListEnded = PlaylistEnded.class;
    public static final Class<PlaylistMediaCountDown> playlistMediaCountDown = PlaylistMediaCountDown.class;
    public static final Class<PlaylistLoopStateChanged> playlistLoopStateChanged = PlaylistLoopStateChanged.class;
    public static final Class<PlaylistShuffleStateChanged> playlistShuffleStateChanged = PlaylistShuffleStateChanged.class;
    public static final Class<PlaylistError> playListError = PlaylistError.class;

    public final Type type;

    public PlaylistEvent(PlaylistEvent.Type type) {
        this.type = type;
    }

    public static class Generic extends PlaylistEvent {
        public Generic(Type type) {
            super(type);
        }
    }

    public static class PlaylistLoaded extends PlaylistEvent {

        public final PKPlaylist playlist;

        public PlaylistLoaded(PKPlaylist playlist) {
            super(Type.PLAYLIST_LOADED);
            this.playlist = playlist;
        }
    }

    public static class PlaylistStarted extends PlaylistEvent {

        public final PKPlaylist playlist;

        public PlaylistStarted(PKPlaylist playlist) {
            super(Type.PLAYLIST_STARTED);
            this.playlist = playlist;
        }
    }

    public static class PlaylistEnded extends PlaylistEvent {

        public final PKPlaylist playlist;

        public PlaylistEnded(PKPlaylist playlist) {
            super(Type.PLAYLIST_ENDED);
            this.playlist = playlist;
        }
    }

    public static class PlaylistMediaCountDown extends PlaylistEvent {

        public final int currentPlayingIndex;
        public final CountDownOptions countDownOptions;

        public PlaylistMediaCountDown(int currentPlayingIndex, CountDownOptions countDownOptions) {
            super(Type.PLAYLIST_MEDIA_COUNT_DOWN);
            this.currentPlayingIndex = currentPlayingIndex;
            this.countDownOptions = countDownOptions;
        }
    }

    public static class PlaylistLoopStateChanged extends PlaylistEvent {

        public final boolean mode;

        public PlaylistLoopStateChanged(boolean mode) {
            super(Type.PLAYLIST_LOOP_STATE_CHANGED);
            this.mode = mode;
        }
    }

    public static class PlaylistShuffleStateChanged extends PlaylistEvent {

        public final boolean mode;

        public PlaylistShuffleStateChanged(boolean mode) {
            super(Type.PLAYLIST_SUFFLE_STATE_CHANGED);
            this.mode = mode;
        }
    }

    public static class PlaylistError extends PlaylistEvent {

        public final ErrorElement error;

        public PlaylistError(ErrorElement error) {
            super(Type.PLAYLIST_ERROR);
            this.error = error;
        }
    }

    public enum Type {
        PLAYLIST_LOADED,
        PLAYLIST_STARTED,
        PLAYLIST_ENDED,
        PLAYLIST_MEDIA_COUNT_DOWN,
        PLAYLIST_LOOP_STATE_CHANGED,
        PLAYLIST_SUFFLE_STATE_CHANGED,
        PLAYLIST_ERROR
        //PLAYLIST_MEDIA_SELECTED,
        //PLAYLIST_MEDIA_ENDED,
        //PLAYLIST_MEDIA_ERROR,
    }

    @Override
    public Enum eventType() {
        return this.type;
    }


}
