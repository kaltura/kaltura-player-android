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
import android.widget.CompoundButton;

import com.kaltura.netkit.utils.ErrorElement;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaSource;

import java.util.Collections;

class TestData {
    static final int partnerId = 2215841;
    static final String ks = null;
    private static final Entry[] entries = Entry.values();
    
    enum Entry {
        sintelShort("1_9bwuo813"),
        sintelFull("1_w9zx2eti"),
        player("http://cdnapi.kaltura.com/p/243342/playManifest/entryId/1_sf5ovm7u/format/applehttp/protocol/http/a.m3u8"),
        oren("http://85.21.100.234/dnetime/testsd7-dash.isml/manifest.mpd");

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

    private KalturaOvpPlayer player;
    private PlaybackControlsView controlsView;

    // Benchmark
    private double loadStartTime, loadEndTime, prepareStartTime, canPlayTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        player = new KalturaOvpPlayer(this, TestData.partnerId, TestData.ks);
        
        player.setAutoPrepare(true);    // prepare after media provider has finished.

        ((ViewGroup) findViewById(R.id.player_container)).addView(player.getView());

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

        final CheckBox checkBox = (CheckBox) findViewById(R.id.autoplay);
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                player.setAutoPlay(isChecked);
            }
        });
        checkBox.setChecked(true);
    }

    private void loadTestEntry(TestData.Entry entry) {
        final View view = controlsView;
        player.stop();

        if (entry.id.startsWith("http")) {
            player.setMedia(new PKMediaEntry().setSources(Collections.singletonList(new PKMediaSource().setUrl(entry.id))));
            return;
        } 
        player.loadMedia(entry.id, new KalturaPlayer.OnEntryLoadListener() {
            @Override
            public void onMediaEntryLoaded(PKMediaEntry entry, ErrorElement error) {
                if (error != null) {
                    Log.d("onMediaEntryLoaded", " error: " + error);
                } else {
                    player.prepare();
                }
            }
        });
    }
}
