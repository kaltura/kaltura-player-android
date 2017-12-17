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

import com.google.gson.JsonObject;
import com.kaltura.kalturaplayer.KalturaPlayer;
import com.kaltura.netkit.utils.ErrorElement;
import com.kaltura.playkit.PKMediaEntry;

public abstract class BaseDemoActivity <PlayerType extends KalturaPlayer> extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, KalturaPlayer.OnEntryLoadListener {

    private static final String TAG = "Main";
    protected final Context context = this;
    protected PlayerType player;
    private ViewGroup contentContainer;
    protected FrameLayout playerContainer;
    private NavigationView navigationView;
    private ListView itemListView;
    protected JsonObject playerConfig;
    private boolean resumePlaying;
    
    protected abstract Item[] items();
    
//    protected DemoFactory demoFactory = new OVPDemoFactory();
//    private DemoFactory demoFactory = new TVDemoFactory();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        loadPlayerConfig();

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

    protected abstract void loadPlayerConfig();

    private void createPlayerContainer() {
        playerContainer = new FrameLayout(this);
        playerContainer.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        final TextView textView = new TextView(this);
        textView.setText("Nothing to play");
        playerContainer.addView(textView);
    }
    
    protected abstract void loadItem(Item item);

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
        itemArrayAdapter.addAll(items());


        itemListView = new ListView(this);

        itemListView.setAdapter(itemArrayAdapter);
        
        itemListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> parent, View view, final int position, long id) {
                new AlertDialog.Builder(context)
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
