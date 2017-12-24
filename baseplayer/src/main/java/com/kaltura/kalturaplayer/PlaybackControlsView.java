package com.kaltura.kalturaplayer;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.PlayerState;
import com.kaltura.playkit.utils.Consts;

import java.util.Formatter;
import java.util.Locale;


public class PlaybackControlsView extends LinearLayout implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    private static final PKLog log = PKLog.get("PlaybackControlsView");
    private static final int PROGRESS_BAR_MAX = 100;

    private KalturaPlayer player;
    private PlayerState playerState;

    private Formatter formatter;
    private StringBuilder formatBuilder;

    private SeekBar seekBar;
    private TextView tvCurTime, tvTime;

    private boolean dragging = false;

    private Runnable updateProgressAction = new Runnable() {
        @Override
        public void run() {
            updateProgress();
        }
    };

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
        initPlaybackControls();
    }

    private void initPlaybackControls() {
        setOnClickListener(this);

        seekBar = this.findViewById(R.id.mediacontroller_progress);
        seekBar.setOnSeekBarChangeListener(this);

        tvCurTime = this.findViewById(R.id.time_current);
        tvTime = this.findViewById(R.id.time);
    }


    private void updateProgress() {
        long duration = Consts.TIME_UNSET;
        long position = Consts.POSITION_UNSET;
        long bufferedPosition = 0;
        if(player != null){
            duration = player.getDuration();
            position = player.getCurrentPosition();
            bufferedPosition = player.getBufferedPosition();
        }

        if(duration != Consts.TIME_UNSET){
            tvTime.setText(stringForTime(duration));
        }

        if (!dragging && position != Consts.POSITION_UNSET && duration != Consts.TIME_UNSET) {
            tvCurTime.setText(stringForTime(position));
            seekBar.setProgress(progressBarValue(position));
        }

        seekBar.setSecondaryProgress(progressBarValue(bufferedPosition));
        // Remove scheduled updates.
        removeCallbacks(updateProgressAction);
        // Schedule an update if necessary.
        if (playerState != PlayerState.IDLE) {
            long delayMs = 1000;
            postDelayed(updateProgressAction, delayMs);
        }
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
        player.addStateChangeListener(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                if (event instanceof PlayerEvent.StateChanged) {
                    PlayerEvent.StateChanged stateChanged = (PlayerEvent.StateChanged) event;

                    setPlayerState(stateChanged.newState);
                }
            }
        });
    }

    public void setPlayerState(PlayerState playerState) {
        this.playerState = playerState;
        updateProgress();
    }

    @Override
    public void onClick(View v) {

        if (player == null) {
            return;
        }
        
        if (player.isPlaying()) {
            player.pause();
        } else {
            player.play();
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            tvCurTime.setText(stringForTime(positionValue(progress)));
        }
    }


    public void onStartTrackingTouch(SeekBar seekBar) {
        dragging = true;
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        dragging = false;
        player.seekTo(positionValue(seekBar.getProgress()));
    }

    public void release() {
        removeCallbacks(updateProgressAction);
    }

    public void resume() {
        updateProgress();
    }
}
