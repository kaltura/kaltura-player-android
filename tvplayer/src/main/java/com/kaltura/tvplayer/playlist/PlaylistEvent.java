package com.kaltura.tvplayer.playlist;

import androidx.annotation.Nullable;

import com.kaltura.netkit.utils.ErrorElement;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKPlaylist;

@SuppressWarnings({"unused", "WeakerAccess"})
public class PlaylistEvent implements PKEvent {

    public static final Class<PlaylistLoaded> playListLoaded = PlaylistLoaded.class;
    public static final Class<PlaylistStarted> playListStarted = PlaylistStarted.class;
    public static final Class<PlaylistEnded> playListEnded = PlaylistEnded.class;
    public static final Class<PlaylistCountDownStart> playlistCountDownStart = PlaylistCountDownStart.class;
    public static final Class<PlaylistCountDownEnd> playlistCountDownEnd = PlaylistCountDownEnd.class;
    public static final Class<PlaylistLoopStateChanged> playlistLoopStateChanged = PlaylistLoopStateChanged.class;
    public static final Class<PlaylistAutoContinueStateChanged> playlistAutoContinueStateChanged = PlaylistAutoContinueStateChanged.class;
    public static final Class<PlaylistError> playListError = PlaylistError.class;
    public static final Class<PlaylistLoadMediaError> playListLoadMediaError = PlaylistLoadMediaError.class;

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

    public static class PlaylistCountDownStart extends PlaylistEvent {

        public final int currentPlayingIndex;
        public final @Nullable CountDownOptions playlistCountDownOptions;

        public PlaylistCountDownStart(int currentPlayingIndex, @Nullable CountDownOptions playlistCountDownOptions) {
            super(Type.PLAYLIST_COUNT_DOWN_START);
            this.currentPlayingIndex = currentPlayingIndex;
            this.playlistCountDownOptions = playlistCountDownOptions;
        }
    }

    public static class PlaylistCountDownEnd extends PlaylistEvent {

        public final int currentPlayingIndex;
        public final @Nullable CountDownOptions playlistCountDownOptions;

        public PlaylistCountDownEnd(int currentPlayingIndex, @Nullable CountDownOptions playlistCountDownOptions) {
            super(Type.PLAYLIST_COUNT_DOWN_END);
            this.currentPlayingIndex = currentPlayingIndex;
            this.playlistCountDownOptions = playlistCountDownOptions;
        }
    }

    public static class PlaylistLoopStateChanged extends PlaylistEvent {

        public final boolean mode;

        public PlaylistLoopStateChanged(boolean mode) {
            super(Type.PLAYLIST_AUTO_CONTINUE_STATE_CHANGED);
            this.mode = mode;
        }
    }

    public static class PlaylistAutoContinueStateChanged extends PlaylistEvent {

        public final boolean mode;

        public PlaylistAutoContinueStateChanged(boolean mode) {
            super(Type.PLAYLIST_LOOP_STATE_CHANGED);
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

    public static class PlaylistLoadMediaError extends PlaylistEvent {

        public final Integer mediaIndex;
        public final ErrorElement error;

        public PlaylistLoadMediaError(Integer mediaIndex, ErrorElement error) {
            super(Type.PLAYLIST_LOAD_MEDIA_ERROR);
            this.mediaIndex = mediaIndex;
            this.error = error;
        }
    }

    public enum Type {
        PLAYLIST_LOADED,
        PLAYLIST_STARTED,
        PLAYLIST_ENDED,
        PLAYLIST_COUNT_DOWN_START,
        PLAYLIST_COUNT_DOWN_END,
        PLAYLIST_LOOP_STATE_CHANGED,
        PLAYLIST_AUTO_CONTINUE_STATE_CHANGED,
        PLAYLIST_ERROR,
        PLAYLIST_LOAD_MEDIA_ERROR
    }

    @Override
    public Enum eventType() {
        return this.type;
    }
}
