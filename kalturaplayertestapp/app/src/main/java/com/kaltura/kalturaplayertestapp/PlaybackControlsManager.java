package com.kaltura.kalturaplayertestapp;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;

import com.kaltura.kalturaplayertestapp.tracks.TracksSelectionController;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.ads.AdEvent;
import com.kaltura.playkit.utils.Consts;
import com.kaltura.tvplayer.KalturaPlayer;
import com.kaltura.tvplayer.PlaybackControlsView;

import static com.kaltura.playkit.PlayerEvent.Type.PLAYING;

/**
 * Created by gilad.nadav on 3/18/18.
 */

public class PlaybackControlsManager implements PlaybackControls {

    private static final PKLog log = PKLog.get("PlaybackControlsManager");
    private static final int REMOVE_CONTROLS_TIMEOUT = 3000; //3250

    private PlayerActivity playerActivity;
    private KalturaPlayer player;
    private PlaybackControlsView playbackControlsView;
    private TracksSelectionController tracksSelectionController;

    private Button videoTracksBtn;
    private Button audioTracksBtn;
    private Button textTracksBtn;
    private Button prevBtn;
    private Button nextBtn;


    private Enum playerState;
    private Enum adPlayerState;
    private boolean isAdDisplayed;

    private Handler hideButtonsHandler = new Handler(Looper.getMainLooper());
    private Runnable hideButtonsRunnable = new Runnable() {
        @Override
        public void run() {
            if (playerState == PLAYING || adPlayerState == AdEvent.Type.STARTED || adPlayerState == AdEvent.Type.RESUMED || adPlayerState == AdEvent.Type.COMPLETED) {
                showControls(View.INVISIBLE);
            }
        }
    };

    public Enum getPlayerState() {
        return playerState;
    }

    public Enum getAdPlayerState() {
        return adPlayerState;
    }

    public PlaybackControlsManager(PlayerActivity playerActivity, KalturaPlayer player, PlaybackControlsView playbackControlsView) {
        this.playerActivity = playerActivity;
        this.player = player;
        this.playbackControlsView = playbackControlsView;
        this.videoTracksBtn = playerActivity.findViewById(R.id.video_tracks);
        this.textTracksBtn  = playerActivity.findViewById(R.id.text_tracks);
        this.audioTracksBtn = playerActivity.findViewById(R.id.audio_tracks);
        addTracksButtonsListener();
        this.prevBtn        = playerActivity.findViewById(R.id.prev_btn);
        this.nextBtn        = playerActivity.findViewById(R.id.next_btn);
        showControls(View.INVISIBLE);
    }

    @Override
    public void handleContainerClick() {
        log.d("CLICK handleContainerClick XXXXXXXXX playerState = " + playerState);
        if (playerState == null) {
            return;
        }
        showControls(View.VISIBLE);
        hideButtonsHandler.removeCallbacks(hideButtonsRunnable);
        hideButtonsHandler.postDelayed(hideButtonsRunnable, REMOVE_CONTROLS_TIMEOUT);
    }

    @Override
    public void showControls(int visability) {
        if (playbackControlsView != null) {
            if (isAdDisplayed) {
                if (player.isPlaying()) {
                    playbackControlsView.getPlayPauseToggle().setBackgroundResource(R.drawable.pause);
                } else {
                    playbackControlsView.getPlayPauseToggle().setBackgroundResource(R.drawable.play);
                }
            }
            playbackControlsView.setVisibility(visability);
        }
        if (!isAdDisplayed) {
            nextBtn.setVisibility(visability);
            prevBtn.setVisibility(visability);
            videoTracksBtn.setVisibility(visability);
            audioTracksBtn.setVisibility(visability);
            textTracksBtn.setVisibility(visability);
        }
    }

    @Override
    public void setContentPlayerState(Enum playerState) {
        this.playerState = playerState;

    }

    @Override
    public void setAdPlayerState(Enum adPlayerState) {
        this.adPlayerState = adPlayerState;
        if (adPlayerState == AdEvent.Type.STARTED || adPlayerState == AdEvent.Type.CONTENT_PAUSE_REQUESTED || adPlayerState == AdEvent.Type.TAPPED) {
            isAdDisplayed = true;
        }  else if (adPlayerState == AdEvent.Type.CONTENT_RESUME_REQUESTED || adPlayerState == AdEvent.Type.ALL_ADS_COMPLETED) {
            isAdDisplayed = false;
        }
    }

    private void addTracksButtonsListener() {
        videoTracksBtn.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                if (tracksSelectionController != null && !isAdDisplayed) {
                    tracksSelectionController.showTracksSelectionDialog(Consts.TRACK_TYPE_VIDEO);
                }
            }
        });
        textTracksBtn.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                if (tracksSelectionController != null && !isAdDisplayed) {
                    tracksSelectionController.showTracksSelectionDialog(Consts.TRACK_TYPE_TEXT);
                }
            }
        });
        audioTracksBtn.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                if (tracksSelectionController != null && !isAdDisplayed) {
                    tracksSelectionController.showTracksSelectionDialog(Consts.TRACK_TYPE_AUDIO);
                }
            }
        });
    }
    public void updatePrevNextBtnFunctionality(int currentPlayedMediaIndex, int mediaListSize) {
        if (mediaListSize > 1) {
            if (currentPlayedMediaIndex == 0) {
                nextBtn.setClickable(true);
                nextBtn.setBackgroundColor(Color.rgb(66,165,245));
                prevBtn.setClickable(false);
                prevBtn.setBackgroundColor(Color.RED);
            } else if (currentPlayedMediaIndex == mediaListSize - 1) {
                nextBtn.setClickable(false);
                nextBtn.setBackgroundColor(Color.RED);
                prevBtn.setClickable(true);
                prevBtn.setBackgroundColor(Color.rgb(66,165,245));
            } else if (currentPlayedMediaIndex > 0 && currentPlayedMediaIndex < mediaListSize - 1) {
                nextBtn.setClickable(true);
                nextBtn.setBackgroundColor(Color.rgb(66,165,245));
                prevBtn.setClickable(true);
                prevBtn.setBackgroundColor(Color.rgb(66,165,245));
            } else {
                nextBtn.setClickable(false);
                nextBtn.setBackgroundColor(Color.RED);
                prevBtn.setClickable(false);
                prevBtn.setBackgroundColor(Color.RED);
            }
        } else {
            nextBtn.setClickable(false);
            nextBtn.setBackgroundColor(Color.RED);
            prevBtn.setClickable(false);
            prevBtn.setBackgroundColor(Color.RED);
        }
    }

    public void addChangeMediaButtonsListener(final int mediaListSize) {
        prevBtn.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                playerActivity.setCurrentPlayedMediaIndex(playerActivity.getCurrentPlayedMediaIndex() - 1);
                if (mediaListSize <= 1)  {
                    return;
                }
                updatePrevNextBtnFunctionality(playerActivity.getCurrentPlayedMediaIndex(), mediaListSize);
                playerActivity.clearLogView();
                player.stop();

                playerActivity.changeMedia();
            }
        });
        nextBtn.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                playerActivity.setCurrentPlayedMediaIndex(playerActivity.getCurrentPlayedMediaIndex() + 1);

                if (mediaListSize <= 1)  {
                    return;
                }
                updatePrevNextBtnFunctionality(playerActivity.getCurrentPlayedMediaIndex(), mediaListSize);
                playerActivity.clearLogView();
                player.stop();
                playerActivity.changeMedia();
            }
        });
    }

    public void setTracksSelectionController(TracksSelectionController tracksSelectionController) {
        this.tracksSelectionController = tracksSelectionController;
    }
}