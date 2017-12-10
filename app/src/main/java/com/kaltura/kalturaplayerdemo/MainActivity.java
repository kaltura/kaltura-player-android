package com.kaltura.kalturaplayerdemo;

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

import com.kaltura.kalturaplayer.KalturaOvpPlayer;
import com.kaltura.kalturaplayer.KalturaPhoenixPlayer;
import com.kaltura.kalturaplayer.KalturaPlayer;
import com.kaltura.kalturaplayer.MediaOptions;
import com.kaltura.kalturaplayer.OVPMediaOptions;
import com.kaltura.kalturaplayer.PlayerInitOptions;
import com.kaltura.kalturaplayer.TVMediaOptions;
import com.kaltura.netkit.utils.ErrorElement;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaSource;

import java.util.Collections;


//let ottServerUrl = "http://api-preprod.ott.kaltura.com/v4_5/api_v3"
//        let ottPartnerId: Int64 = 198
//        let ottAssetId = "259153"
//        let ottFileId = "804398"

class TestData {
    static final String ottServerUrl = "http://api-preprod.ott.kaltura.com/v4_5/api_v3/";
    static final int ottPartnerId = 198;
    static final int partnerId = 2215841;
//    static final int partnerId = 1851571;
    static final int uiConfId = 31956421;
    static final String ks = null;
    private static final Entry[] entries = Entry.values();
    
    static final boolean ott = false;
    
    enum Entry {
        ott1("259153", "804398"),
        sintelShort("1_9bwuo813"),
        sintelFull("1_w9zx2eti"),
        player("http://cdnapi.kaltura.com/p/243342/playManifest/entryId/1_sf5ovm7u/format/applehttp/protocol/http/a.m3u8"),
        oren("http://85.21.100.234/dnetime/testsd7-dash.isml/manifest.mpd");

        final String id;
        final String fileId;
        
        Entry(String id, String fileId) {
            this.id = id;
            this.fileId = fileId;
        }

        Entry(String id) {
            this(id, null);
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        if (TestData.ott) {
            KalturaPhoenixPlayer.create(this, new PlayerInitOptions().setServerUrl(TestData.ottServerUrl).setPartnerId(TestData.ottPartnerId), new KalturaPlayer.PlayerReadyCallback() {
                @Override
                public void onPlayerReady(KalturaPlayer player) {
                    MainActivity.this.player = player;
                }
            });
        } else {
            KalturaOvpPlayer.create(this, new PlayerInitOptions().setPartnerId(TestData.partnerId), new KalturaPlayer.PlayerReadyCallback() {
                @Override
                public void onPlayerReady(KalturaPlayer player) {
                    MainActivity.this.player = player;
                }
            });
        }
        
        
        player.setPreload(true);

        final ViewGroup playerContainer = findViewById(R.id.player_container);
        playerContainer.addView(player.getView());

        FloatingActionButton fab = findViewById(R.id.fab);
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

        final CheckBox checkBox = findViewById(R.id.autoplay);
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                player.setAutoPlay(isChecked);
            }
        });
        checkBox.setChecked(true);
    }

    private void loadTestEntry(TestData.Entry entry) {

        player.stop();

        if (entry.id.startsWith("http")) {
            player.setMedia(new PKMediaEntry().setSources(Collections.singletonList(new PKMediaSource().setUrl(entry.id))));
            return;
        }
        
        MediaOptions mediaOptions;
        if (TestData.ott) {
            mediaOptions = new TVMediaOptions().setAssetId(entry.id).setFileIds(new String[]{entry.fileId});
        } else {
            mediaOptions = new OVPMediaOptions().setEntryId(entry.id);
        }
        
        player.loadMedia(mediaOptions, new KalturaPlayer.OnEntryLoadListener() {
            @Override
            public void onMediaEntryLoaded(PKMediaEntry entry, ErrorElement error) {
                if (error != null) {
                    Log.d("onMediaEntryLoaded", " error: " + error);
                }
            }
        });
    }
}
