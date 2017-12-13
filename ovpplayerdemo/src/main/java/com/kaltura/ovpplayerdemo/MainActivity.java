package com.kaltura.ovpplayerdemo;

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
import com.kaltura.kalturaplayer.PlayerConfigManager;
import com.kaltura.kalturaplayer.PlayerInitOptions;
import com.kaltura.netkit.utils.ErrorElement;
import com.kaltura.ovpplayer.KalturaOvpPlayer;
import com.kaltura.ovpplayer.OVPMediaOptions;
import com.kaltura.playkit.PKMediaEntry;

class TestData {
    static final int partnerId = 2215841;
    static final int uiConfId = 41188731;
    static final String ks = null;
    
    static final Item[] items = {
            new Item("Sintel", "1_w9zx2eti"),
            new Item("Sintel - snippet", "1_9bwuo813"),
    };
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
        return name + " (" + id + ")";
    }
}


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, KalturaPlayer.OnEntryLoadListener {

    private static final String TAG = "Main";
    private KalturaOvpPlayer player;
    private ViewGroup contentContainer;
    private FrameLayout playerContainer;
    private NavigationView navigationView;
    private ListView itemListView;
    private JsonObject playerConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        PlayerConfigManager.initialize(this);
        PlayerConfigManager.retrieve(TestData.uiConfId, TestData.partnerId, TestData.ks, null, new PlayerConfigManager.OnPlayerConfigLoaded() {
            @Override
            public void onConfigLoadComplete(int id, JsonObject config, ErrorElement error, int freshness) {

                Toast.makeText(MainActivity.this, "Loaded config, freshness=" + freshness, Toast.LENGTH_LONG).show();
                playerConfig = config;

            }
        });

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

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
            player = KalturaOvpPlayer.create(MainActivity.this, new PlayerInitOptions()
                    .setPlayerConfig(playerConfig)
                    .setAutoPlay(true)
                    .setPartnerId(TestData.partnerId));
            playerContainer.removeAllViews();
            playerContainer.addView(player.getView());
        }
        
        player.stop();

        OVPMediaOptions mediaOptions = new OVPMediaOptions().setEntryId(item.id);

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

        contentContainer.removeAllViews();

        if (id == R.id.nav_player) {
            contentContainer.addView(playerContainer);

        } else if (id == R.id.nav_items) {
            contentContainer.addView(getItemListView());
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
    
    private ListView getItemListView() {
        if (itemListView != null) {
            return itemListView;
        }

        ArrayAdapter<Item> itemArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        itemArrayAdapter.addAll(TestData.items);


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
