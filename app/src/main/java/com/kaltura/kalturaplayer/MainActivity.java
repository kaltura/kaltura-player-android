package com.kaltura.kalturaplayer;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;

import com.kaltura.netkit.utils.ErrorElement;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKMediaConfig;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerEvent;

class TestData {
    static final int partnerId = 2215841;
    static final String ks = null;
    static final Entry[] entries = Entry.values();
    
    enum Entry {
        sintelShort("1_9bwuo813"),
        sintelFull("1_w9zx2eti");

        final String id;

        Entry(String id) {
            this.id = id;
        }
    }
    
    static String[] names() {
        String[] names = new String[entries.length];

        for (int i=0; i<entries.length; i++) {
            names[i] = entries[i].name();
        }
        
        return names;
    }
    
    static Entry forIndex(int i) {
        return entries[i];
    }
}

public class MainActivity extends AppCompatActivity {

    PlayerFactory playerFactory;
    private ViewGroup playerContainer;
    private ViewGroup controlsContainer;
    private Player player;
    private PlaybackControlsView controlsView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        playerFactory = new PlayerFactory(this.getApplicationContext(), TestData.partnerId, TestData.ks);

        loadPlayer();
        
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Select item")
                        .setItems(TestData.names(), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                TestData.Entry entry = TestData.forIndex(which);
                                loadTestEntry(entry);
                            }
                }).show();
            }
        });
        
    }

    private void loadTestEntry(TestData.Entry entry) {
        final View view = controlsView;
        Snackbar.make(view, "Selected item: " + entry, Snackbar.LENGTH_SHORT).show();
        playerFactory.loadEntry(entry.id, new PlayerFactory.OnMediaEntryLoaded() {
            @Override
            public void entryLoaded(final PKMediaEntry entry, ErrorElement error) {
                if (entry == null) {
                    Snackbar.make(view, "Failed to load entry", Snackbar.LENGTH_SHORT).show();
                } else {
                    Snackbar.make(view, "Entry loaded", Snackbar.LENGTH_SHORT).show();
                    player.prepare(new PKMediaConfig().setMediaEntry(entry));
                }
            }
        });
    }

    private void loadPlayer() {

        if (player == null) {
            player = playerFactory.loadPlayer(this);
            playerContainer = findViewById(R.id.player_container);
            playerContainer.addView(player.getView());
            
            player.addEventListener(new PKEvent.Listener() {
                @Override
                public void onEvent(PKEvent event) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Snackbar.make(controlsView, "Ready to play", Snackbar.LENGTH_SHORT).show();
                        }
                    });
                }
            }, PlayerEvent.Type.CAN_PLAY);
        }

        if (controlsView == null) {
            controlsView = new PlaybackControlsView(this);
            controlsView.setPlayer(player);
            controlsContainer = findViewById(R.id.controls_container);
            controlsContainer.addView(controlsView);
        }
    }
}
