package com.kaltura.kalturaplayer;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Toast;

import com.kaltura.netkit.utils.ErrorElement;
import com.kaltura.playkit.PKMediaEntry;

class TestData {
    static final int partnerId = 2215841;
    static final String ks = null;
    private static final Entry[] entries = Entry.values();
    
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

    private KalturaPlayer player;
    private PlaybackControlsView controlsView;

    // Benchmark
    private double loadStartTime, loadEndTime, prepareStartTime, canPlayTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        player = new KalturaPlayer(this, TestData.partnerId, TestData.ks);

        ViewGroup playerContainer = ((ViewGroup) findViewById(R.id.player_container));
        playerContainer.addView(player.getView());

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

    private boolean shouldAutoPlay() {
        CheckBox checkBox = (CheckBox) findViewById(R.id.autoplay);
        return checkBox.isChecked();
    }
    
    private void loadTestEntry(TestData.Entry entry) {
        final View view = controlsView;
        player.stop();

        if (shouldAutoPlay()) {
            player.loadAndPlay(entry.id, 0, new KalturaPlayer.OnEntryLoadListener() {
                @Override
                public void onMediaEntryLoaded(PKMediaEntry entry, ErrorElement error) {
                    if (error != null) {
                        Toast.makeText(MainActivity.this, "Load failed", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            player.loadMedia(entry.id, new KalturaPlayer.OnEntryLoadListener() {
                @Override
                public void onMediaEntryLoaded(PKMediaEntry entry, ErrorElement error) {
                    if (error != null) {
                        Log.d("onMediaEntryLoaded", " error: " + error);
                    } else {
                        player.prepare(entry, 0);
                    }
                }
            });
        }
    }
}
