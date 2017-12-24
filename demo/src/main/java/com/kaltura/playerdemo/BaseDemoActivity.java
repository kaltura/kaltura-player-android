package com.kaltura.playerdemo;

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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kaltura.kalturaplayer.KalturaPlayer;
import com.kaltura.kalturaplayer.PlayerInitOptions;
import com.kaltura.netkit.utils.ErrorElement;
import com.kaltura.playkit.PKDrmParams;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKPluginConfigs;
import com.kaltura.playkit.player.MediaSupport;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class BaseDemoActivity <PlayerType extends KalturaPlayer> extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, KalturaPlayer.OnEntryLoadListener {

    private static final PKLog log = PKLog.get("BaseDemoActivity");
    
    protected final Context context = this;
    protected PlayerType player;
    int uiConfId;
    String ks;
    DemoItem[] items;
    private ViewGroup contentContainer;
    protected FrameLayout playerContainer;
    private NavigationView navigationView;
    private ListView itemListView;
    protected JsonObject playerConfig;
    private boolean resumePlaying;
    protected PlayerInitOptions initOptions;
    
    protected abstract DemoItem[] items();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        initDrm();

        loadConfigFile();

        loadPlayerConfig();
        
        final DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        drawer.openDrawer(Gravity.START);

        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        contentContainer = findViewById(R.id.content_container);
        contentContainer.addView(getItemListView());
        navigationView.setCheckedItem(R.id.nav_items);

        createPlayerContainer();
    }

    protected void parseInitOptions(JsonObject json) {
        final PlayerInitOptions options = new PlayerInitOptions();
        final Integer partnerId = safeInteger(json, "partnerId");
        if (partnerId == null) {
            throw new IllegalArgumentException("partnerId must not be null");
        }
        

        options
                .setPartnerId(partnerId)
                .setServerUrl(safeString(json, "serverUrl"))
                .setAutoPlay(safeBoolean(json, "autoPlay"))
                .setPreload(safeBoolean(json, "preload"))
                .setKs(safeString(json, "ks"))
                .setPlayerConfig(safeObject(json, "playerConfig"))
                .setPluginConfigs(parsePluginConfigs(json.get("plugins")))
                .setReferrer(safeString(json, "referrer"));

        initOptions = options;
    }

    private PKPluginConfigs parsePluginConfigs(JsonElement json) {
        PKPluginConfigs configs = new PKPluginConfigs();
        if (json != null && json.isJsonObject()) {
            final JsonObject obj = json.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                final String pluginName = entry.getKey();
                final JsonElement value = entry.getValue();
                configs.setPluginConfig(pluginName, value);
            }
        }
        return configs;
    }

    protected JsonObject safeObject(JsonObject json, String key) {
        final JsonElement jsonElement = json.get(key);
        if (jsonElement != null && jsonElement.isJsonObject()) {
            return jsonElement.getAsJsonObject();
        }
        return null;
    }

    protected String safeString(JsonObject json, String key) {
        final JsonElement jsonElement = json.get(key);
        if (jsonElement != null && jsonElement.isJsonPrimitive()) {
            return jsonElement.getAsString();
        }
        return null;
    }

    protected Boolean safeBoolean(JsonObject json, String key) {
        final JsonElement jsonElement = json.get(key);
        if (jsonElement != null && jsonElement.isJsonPrimitive()) {
            return jsonElement.getAsBoolean();
        }
        return null;
    }

    protected Integer safeInteger(JsonObject json, String key) {
        final JsonElement jsonElement = json.get(key);
        if (jsonElement != null && jsonElement.isJsonPrimitive()) {
            return jsonElement.getAsInt();
        }
        return null;
    }

    protected abstract void loadConfigFile();

    void initDrm() {
        MediaSupport.initializeDrm(this, new MediaSupport.DrmInitCallback() {
            @Override
            public void onDrmInitComplete(Set<PKDrmParams.Scheme> supportedDrmSchemes, boolean provisionPerformed, Exception provisionError) {
                if (provisionPerformed) {
                    if (provisionError != null) {
                        log.e("DRM Provisioning failed", provisionError);
                    } else {
                        log.d("DRM Provisioning succeeded");
                    }
                }
                log.d("DRM initialized; supported: " + supportedDrmSchemes);

                // Now it's safe to look at `supportedDrmSchemes`
            }
        });
    }

    protected void parseCommonOptions(JsonObject json) {
        parseInitOptions(safeObject(json, "initOptions"));

        ks = initOptions.ks;
        final JsonArray jsonItems = json.get("items").getAsJsonArray();
        List<DemoItem> itemList = new ArrayList<>(jsonItems.size());
        for (JsonElement item : jsonItems) {
            final JsonObject object = item.getAsJsonObject();
            itemList.add(parseItem(object));
        }

        items = itemList.toArray(new DemoItem[itemList.size()]);
    }

    @NonNull
    protected abstract DemoItem parseItem(JsonObject object);

    protected abstract void loadPlayerConfig();
    
    protected int partnerId() {
        return initOptions.partnerId;
    }

    private void createPlayerContainer() {
        playerContainer = new FrameLayout(this);
        playerContainer.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        final TextView textView = new TextView(this);
        textView.setText("Nothing to play");
        playerContainer.addView(textView);
    }
    
    protected abstract void loadItem(DemoItem item);

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

        TextView partnerIdTextView = findViewById(R.id.partnerIdTextView);
        partnerIdTextView.setText("Partner: " + partnerId());

        TextView demoNameTextView = findViewById(R.id.demoNameTextView);
        demoNameTextView.setText(demoName());

        ImageView icon = findViewById(R.id.imageView);
        icon.setColorFilter(android.R.color.holo_green_dark);

        return true;
    }

    protected abstract String demoName();

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

        ArrayAdapter<DemoItem> itemArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
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
                                loadItem(((DemoItem) parent.getItemAtPosition(position)));
                            }
                        }).show();
            }
        });

        return itemListView;
    }
}
