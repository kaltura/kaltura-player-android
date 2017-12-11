package com.kaltura.ovpplayerdemo;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.kaltura.kalturaplayer.KalturaPlayer;
import com.kaltura.kalturaplayer.PlayerInitOptions;
import com.kaltura.netkit.utils.ErrorElement;
import com.kaltura.ovpplayer.KalturaOvpPlayer;
import com.kaltura.ovpplayer.OVPMediaOptions;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaSource;

import java.util.Collections;

class TestData {
    static final int partnerId = 2215841;
    static final int uiConfId = 41188731;
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

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, KalturaPlayer.OnEntryLoadListener {

    private KalturaOvpPlayer player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Player
        player = KalturaOvpPlayer.create(this, new PlayerInitOptions()
                .setAutoPlay(true)
                .setPartnerId(TestData.partnerId));

        final ViewGroup playerContainer = findViewById(R.id.player_container);
        playerContainer.addView(player.getView());

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        
    }

    private void loadTestEntry(TestData.Entry entry) {

        if (player == null) {
            Toast.makeText(this, "Player not ready", Toast.LENGTH_SHORT).show();
            return;
        }
        
        player.stop();

        if (entry.id.startsWith("http")) {
            player.setMedia(new PKMediaEntry().setSources(Collections.singletonList(new PKMediaSource().setUrl(entry.id))));
            return;
        }

        OVPMediaOptions mediaOptions = new OVPMediaOptions().setEntryId(entry.id);
        player.loadMedia(mediaOptions, this);
    }

    @Override
    public void onMediaEntryLoaded(PKMediaEntry entry, ErrorElement error) {
        if (error != null) {
            Log.d("onMediaEntryLoaded", " error: " + error);
        }
    }
    
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
