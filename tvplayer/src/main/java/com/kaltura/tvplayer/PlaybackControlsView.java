package com.kaltura.tvplayer;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.kaltura.android.exoplayer2.Player;
import com.kaltura.android.exoplayer2.Timeline;
import com.kaltura.android.exoplayer2.ui.DefaultTimeBar;
import com.kaltura.android.exoplayer2.ui.TimeBar;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.PlayerState;
import com.kaltura.playkit.ads.AdController;
import com.kaltura.playkit.plugins.ads.AdEvent;
import com.kaltura.playkit.utils.Consts;

import java.util.Formatter;
import java.util.Locale;

import static com.kaltura.playkit.PKMediaEntry.MediaEntryType.DvrLive;
import static com.kaltura.playkit.PKMediaEntry.MediaEntryType.Live;


public class PlaybackControlsView extends LinearLayout {

    private static final PKLog log = PKLog.get("PlaybackControlsView");
    private static final int PROGRESS_BAR_MAX = 100;
    private static final int UPDATE_TIME_INTERVAL = 300; //1000
    private static final int LIVE_EDGE_THRESHOLD = 60000; // in milliseconds

    private KalturaPlayer player;
    private PlayerState playerState;
    private boolean isError;

    private final Formatter formatter;
    private final StringBuilder formatBuilder;

    private ImageButton playPauseToggle;
    private DefaultTimeBar seekBar;
    private TextView tvCurTime, tvTime, tvLiveIndicator;

    private boolean dragging = false;
    private boolean adTagHasPostroll;

    private final ComponentListener componentListener;

    private final Runnable updateProgressAction = this::updateProgress;

    public PlaybackControlsView(Context context) {
        this(context, null);
    }

    public PlaybackControlsView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PlaybackControlsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater.from(context).inflate(R.layout.playback_layout, this);
        formatBuilder = new StringBuilder();
        formatter = new Formatter(formatBuilder, Locale.getDefault());
        componentListener = new ComponentListener();
        initPlaybackControls();
    }

    private void initPlaybackControls() {
        playPauseToggle = this.findViewById(R.id.toggleButton);
        playPauseToggle.setOnClickListener(view -> {
            if (playerState == null) {
                return;
            }
            togglePlayPauseClick();
        });

        seekBar = this.findViewById(R.id.kexo_progress);
        seekBar.addListener(componentListener);

        tvCurTime = this.findViewById(R.id.time_current);
        tvTime = this.findViewById(R.id.time);
        tvLiveIndicator = this.findViewById(R.id.liveIndicator);
    }

    private void updateProgress() {
        long duration = Consts.TIME_UNSET;
        long position = Consts.POSITION_UNSET;
        long bufferedPosition = 0;
        if(player != null) {
            AdController adController = player.getController(AdController.class);
            if (adController != null && adController.isAdDisplayed()) {
                duration = adController.getAdDuration();
                position = adController.getAdCurrentPosition();
                //log.d("adController Duration:" + duration);
                //log.d("adController Position:" + position);
            } else {
                duration = player.getDuration();
                position = player.getCurrentPosition();
                //log.d("Duration:" + duration);
                //log.d("Position:" + position);
                bufferedPosition = player.getBufferedPosition();
            }
        }

        if (player != null && player.getMediaEntry() != null &&
                player.getMediaEntry().getMediaType() != null &&
                player.getMediaEntry().getMediaType().equals(Live)) {
            tvLiveIndicator.setVisibility(VISIBLE);
            tvCurTime.setVisibility(INVISIBLE);
            tvTime.setVisibility(View.INVISIBLE);
            seekBar.setVisibility(INVISIBLE);
        } else {
            if(duration != Consts.TIME_UNSET){
                tvTime.setText(stringForTime(duration));
            }
            if (!dragging && position != Consts.POSITION_UNSET && duration != Consts.TIME_UNSET) {
                tvCurTime.setText(stringForTime(position));
                seekBar.setPosition(position);
                seekBar.setDuration(duration);
            }

            if (player != null && player.getMediaEntry() != null &&
                    player.getMediaEntry().getMediaType() != null &&
                    player.getMediaEntry().getMediaType().equals(DvrLive)) {
                tvLiveIndicator.setVisibility(VISIBLE);
                if (!dragging && position > (duration - LIVE_EDGE_THRESHOLD)) {
                    tvLiveIndicator.setBackgroundResource(R.drawable.red_background);
                } else {
                    tvLiveIndicator.setBackgroundResource(R.drawable.grey_background);
                }
            } else {
                tvLiveIndicator.setVisibility(GONE);
            }

            seekBar.setBufferedPosition(bufferedPosition);
        }

        // Remove scheduled updates.
        removeCallbacks(updateProgressAction);
        // Schedule an update if necessary.
        if (playerState != PlayerState.IDLE) {
            postDelayed(updateProgressAction, UPDATE_TIME_INTERVAL);
        }
    }

    /**
     * Component Listener for Default time bar from ExoPlayer UI
     */
    private final class ComponentListener implements Player.Listener, TimeBar.OnScrubListener, OnClickListener {

        @Override
        public void onScrubStart(@NonNull TimeBar timeBar, long position) {
            dragging = true;
        }

        @Override
        public void onScrubMove(@NonNull TimeBar timeBar, long position) {
            if (player != null) {
                tvCurTime.setText(stringForTime(position));
            }
        }

        @Override
        public void onScrubStop(@NonNull TimeBar timeBar, long position, boolean canceled) {
            dragging = false;
            if (player != null) {
                player.seekTo(position);
            }
        }

        @Override
        public void onPlaybackStateChanged(int playbackState) {
            updateProgress();
        }

        @Override
        public void onPositionDiscontinuity(@NonNull Player.PositionInfo oldPosition, @NonNull Player.PositionInfo newPosition, int reason) {
            updateProgress();
        }

        @Override
        public void onTimelineChanged(@NonNull Timeline timeline, int reason) {
            updateProgress();
        }

        @Override
        public void onClick(View view) { }
    }

    private int progressBarValue(long position) {
        int progressValue = 0;
        if(player != null){
            long duration = player.getDuration();
            if (duration > 0) {
                progressValue = (int) ((position * PROGRESS_BAR_MAX) / duration);
            }
        }

        return progressValue;
    }

    private long positionValue(int progress) {
        long positionValue = 0;
        if(player != null){
            long duration = player.getDuration();
            positionValue = (duration * progress) / PROGRESS_BAR_MAX;
        }

        return positionValue;
    }

    private String stringForTime(long timeMs) {
        long totalSeconds = (timeMs + 500) / 1000;
        long seconds = totalSeconds % 60;
        long minutes = (totalSeconds / 60) % 60;
        long hours = totalSeconds / 3600;
        formatBuilder.setLength(0);
        return hours > 0 ? formatter.format("%d:%02d:%02d", hours, minutes, seconds).toString()
                : formatter.format("%02d:%02d", minutes, seconds).toString();
    }

    public void setPlayer(KalturaPlayer player) {
        this.player = player;
        this.player.addListener(this, PlayerEvent.stateChanged, event -> {
            log.d("stateChanged newState = " + event.newState);
            if (setIdleStateAfterPostroll(player, event)) {
                return;
            }
            setPlayerState(event.newState);
        });

        this.player.addListener(this, AdEvent.cuepointsChanged, event -> {
            log.d("cuepointsChanged");
            if (event.cuePoints != null) {
                adTagHasPostroll = event.cuePoints.hasPostRoll();
            }

            if (playerState == null) {
                setPlayerState(PlayerState.IDLE);
            }
        });

        this.player.addListener(this, AdEvent.allAdsCompleted, event -> {
            log.d("allAdsCompleted");
            if (player != null && player.getCurrentPosition() > 0 && player.getDuration() > 0 && player.getCurrentPosition() >= player.getDuration()) {
                setPlayerState(PlayerState.IDLE);
            }
        });

        this.player.addListener(this, PlayerEvent.error, event -> {
            isError = true;
        });

        this.player.addListener(this, PlayerEvent.canPlay, event -> {
            isError = false;
        });
    }

    private boolean setIdleStateAfterPostroll(KalturaPlayer player, PlayerEvent.StateChanged stateChanged) {
        boolean setIdleStateAfterPostroll = false;
        if (stateChanged.newState != PlayerState.IDLE) {
            return setIdleStateAfterPostroll;
        }

        if (player == null) {
            return setIdleStateAfterPostroll;
        }

        AdController adController = player.getController(AdController.class);
        if (adController == null) {
            return setIdleStateAfterPostroll;
        }

        if (player.getCurrentPosition() > 0 && player.getDuration() > 0 &&
                player.getCurrentPosition() >= player.getDuration() &&
                (adController.isAdDisplayed() || adTagHasPostroll)) {
            setIdleStateAfterPostroll = true;
        }
        return setIdleStateAfterPostroll;
    }

    public void setPlayerState(PlayerState playerState) {
        log.d("setPlayerState " + playerState.name());
        this.playerState = playerState;
        updateProgress();
    }

    public ImageButton getPlayPauseToggle() {
        return playPauseToggle;
    }

    public DefaultTimeBar getSeekBar() {
        return seekBar;
    }

    private void setPlayImage() {
        playPauseToggle.setBackgroundResource(R.drawable.play);
    }

    private void setPauseImage() {
        playPauseToggle.setBackgroundResource(R.drawable.pause);
    }

    public void destroy() {
        if (player != null) {
            player.removeListeners(this);
            player.destroy();
            player = null;
        }
    }

    public void togglePlayPauseClick() {
        if (player == null || isError) {
            return;
        }

        AdController adController = player.getController(AdController.class);
        if (adController != null && adController.isAdDisplayed()) {
            if (adController.isAdPlaying()) {
                adController.pause();
                setPlayImage();
            } else {
                adController.play();
                setPauseImage();
            }
        } else {
            if (player.isPlaying()) {
                player.pause();
                setPlayImage();

            } else {
                if (player.getCurrentPosition() > 0  && player.getDuration() > 0 && player.getCurrentPosition() >= player.getDuration()) {
                    player.replay();
                } else {
                    player.play();
                }
                setPauseImage();
            }
        }
    }

    public void release() {
        removeCallbacks(updateProgressAction);
    }

    public void resume() {
        updateProgress();
    }

    public void setSeekbarDisabled() {
        seekBar.setEnabled(false);
    }

    public void setSeekbarEnabled() {
        seekBar.setEnabled(true);
    }

    public void setSeekBarVisibility(int visibility) {
        seekBar.setVisibility(visibility);
        tvCurTime.setVisibility(visibility);
        tvTime.setVisibility(visibility);
    }
}
