package com.kaltura.ovpplayerdemo;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.kaltura.kalturaplayer.KalturaPlayer;
import com.kaltura.kalturaplayer.MediaOptions;
import com.kaltura.kalturaplayer.PlayerConfigManager;
import com.kaltura.kalturaplayer.PlayerInitOptions;
import com.kaltura.netkit.utils.ErrorElement;
import com.kaltura.ovpplayer.KalturaOvpPlayer;
import com.kaltura.ovpplayer.OVPMediaOptions;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.tvplayer.KalturaTvPlayer;
import com.kaltura.tvplayer.TVMediaOptions;

class Factory {

    enum Mode {ovp, tv}
    
    static Mode mode = Mode.ovp;
    
    static class OVP {
//        static final int partnerId = 2222401;
//        static final int uiConfId = 40125321;
        static final int partnerId = 2215841;
        static final int uiConfId = 41188731;
        static final String ks = null;

        static final Item[] items = {
                new Item("Sintel", "1_w9zx2eti"),
                new Item("Sintel - snippet", "1_9bwuo813"),
//                new Item("Sintel", "1_f93tepsn"),
//                new Item("Sintel - snippet", "1_q81a5nbp"),
//                new Item("Big Buck Bunny", "1_m1vpaory"),
//                new Item("Kaltura Video Solutions for Media Companies", "1_ytsd86sc"),
//                new Item("Kaltura Video Platform Overview", "1_25q88snr"),
        };

        static PlayerInitOptions options(JsonObject playerConfig) {
            return new PlayerInitOptions()
                    .setPlayerConfig(playerConfig)
                    .setAutoPlay(true)
                    .setPartnerId(partnerId);
        }
        
        static KalturaPlayer player(Context context, JsonObject playerConfig) {
            return KalturaOvpPlayer.create(context, options(playerConfig));
        }
    }
    
    static class TV {
        static final String serverUrl = "http://api-preprod.ott.kaltura.com/v4_5/api_v3/";
        static final int partnerId = 198;
        static final String ks = null;

        static final int ovpPartnerId = 2215841;
        static final int uiConfId = 41188731;

        static final Item[] items = {
                new Item("Sintel", "1_f93tepsn"),
                new Item("Sintel - snippet", "1_q81a5nbp"),
                new Item("Big Buck Bunny", "1_m1vpaory"),
                new Item("Kaltura Video Solutions for Media Companies", "1_ytsd86sc"),
                new Item("Kaltura Video Platform Overview", "1_25q88snr"),
        };

        static PlayerInitOptions options(JsonObject playerConfig) {
            return new PlayerInitOptions()
                    .setPlayerConfig(playerConfig)
                    .setAutoPlay(true)
                    .setServerUrl(serverUrl)
                    .setPartnerId(partnerId);
        }


        static KalturaPlayer player(Context context, JsonObject playerConfig) {
            return KalturaTvPlayer.create(context, options(playerConfig));
        }

    }
    
    static int partnerId() {
        return mode == Mode.ovp ? OVP.partnerId : TV.partnerId;
    }
    
    static Item[] items() {
        return mode == Mode.ovp ? OVP.items : TV.items;
    }
    
    static int configId() {
        return mode == Mode.ovp ? OVP.uiConfId : TV.uiConfId;
    }
    
    static String ks() {
        return mode == Mode.ovp ? OVP.ks : TV.ks;
    }
    
    static KalturaPlayer player(Context context, JsonObject playerConfig) {
        return mode == Mode.ovp ? OVP.player(context, playerConfig) : TV.player(context, playerConfig);
    }
    
    static MediaOptions mediaOptions(Item item) {

        switch (mode) {
            case ovp:
                return new OVPMediaOptions().setEntryId(item.id);
            case tv:
                return new TVMediaOptions().setAssetId(item.id);
        }
        return null;
    }
}

class Item {
    public final String name;
    public final String id;
    
    Item(String name, String id) {
        this.id = id;
        this.name = name;
    }

    @Override
    public String toString() {
        return name + " ➜ " + id;
    }
}


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, KalturaPlayer.OnEntryLoadListener {

    private static final String TAG = "Main";
    private KalturaPlayer player;
    private ViewGroup contentContainer;
    private FrameLayout playerContainer;
    private NavigationView navigationView;
    private ListView itemListView;
    private JsonObject playerConfig;
    private boolean resumePlaying;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        PlayerConfigManager.initialize(this);
        PlayerConfigManager.retrieve(Factory.configId(), Factory.partnerId(), Factory.ks(), null, new PlayerConfigManager.OnPlayerConfigLoaded() {
            @Override
            public void onConfigLoadComplete(int id, JsonObject config, ErrorElement error, int freshness) {
                Toast.makeText(MainActivity.this, "Loaded config, freshness=" + freshness, Toast.LENGTH_LONG).show();
                playerConfig = config;
            }
        });

        final DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        drawer.openDrawer(Gravity.START);
        drawer.postDelayed(new Runnable() {
            @Override
            public void run() {
                drawer.closeDrawer(Gravity.START);
            }
        }, 1000);

        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        contentContainer = findViewById(R.id.content_container);
        contentContainer.addView(getItemListView());
        navigationView.setCheckedItem(R.id.nav_items);

        createPlayerContainer();
        
    }

    private void createPlayerContainer() {
        playerContainer = new FrameLayout(this);
        playerContainer.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        final TextView textView = new TextView(this);
        textView.setText("Nothing to play");
        playerContainer.addView(textView);
    }

    private void loadItem(Item item) {

        if (player == null) {
            player = Factory.player(this, playerConfig);
            playerContainer.removeAllViews();
            playerContainer.addView(player.getView());
        }
        
        player.stop();

        MediaOptions mediaOptions = Factory.mediaOptions(item);

        player.loadMedia(mediaOptions, this);
    }

    @Override
    public void onEntryLoadComplete(PKMediaEntry entry, ErrorElement error) {
        if (error != null) {
            Log.d("onEntryLoadComplete", " error: " + error);
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
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        changeView(id);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) {
            player.onApplicationPaused();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (player != null) {
            player.onApplicationResumed();
        }
    }

    private void changeView(int id) {
        final boolean playerShown = isPlayerVisible();
        if (playerShown && id == R.id.nav_player) {
            return;
        }
        
        // Temporarily release the player.
        if (playerShown && player != null) {
            resumePlaying = player.isPlaying();
            player.onApplicationPaused();
        }

        contentContainer.removeAllViews();

        if (id == R.id.nav_player) {
            if (player != null) {
                player.onApplicationResumed();
                if (resumePlaying) {
                    player.play();
                }
            }
            contentContainer.addView(playerContainer);

        } else if (id == R.id.nav_items) {
            contentContainer.addView(getItemListView());
        }
    }

    private boolean isPlayerVisible() {
        return playerContainer.getParent() != null;
    }

    private ListView getItemListView() {
        if (itemListView != null) {
            return itemListView;
        }

        ArrayAdapter<Item> itemArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        itemArrayAdapter.addAll(Factory.items());


        itemListView = new ListView(this);

        itemListView.setAdapter(itemArrayAdapter);
        
        itemListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> parent, View view, final int position, long id) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Select action")
                        .setItems(new String[]{"Play"}, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                navigationView.setCheckedItem(R.id.nav_player);
                                navigationView.getMenu().performIdentifierAction(R.id.nav_player, 0);
                                loadItem(((Item) parent.getItemAtPosition(position)));
                            }
                        }).show();
            }
        });

        return itemListView;
    }
}
