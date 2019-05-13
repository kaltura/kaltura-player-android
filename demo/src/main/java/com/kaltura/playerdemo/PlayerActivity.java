package com.kaltura.playerdemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.ViewGroup;

import com.kaltura.playkit.PKLog;
import com.kaltura.tvplayer.KalturaPlayer;

import org.greenrobot.eventbus.EventBus;

public class PlayerActivity extends AppCompatActivity {

    private static final PKLog log = PKLog.get("PlayerActivity");
    
    private KalturaPlayer player;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        EventBus.getDefault().post(this);
    }

    @Override
    protected void onPause() {
        if (player != null) {
            player.onApplicationPaused();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (player != null) {
            player.onApplicationResumed();
            player.play();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            ViewGroup container = findViewById(R.id.player_container);
            container.removeAllViews();

            player.stop();
            player.destroy();
            player = null;
        }
    }

    public void setPlayer(KalturaPlayer player) {
        this.player = player;

        ViewGroup container = findViewById(R.id.player_container);
        container.addView(player.getPlayerView());
    }
}
