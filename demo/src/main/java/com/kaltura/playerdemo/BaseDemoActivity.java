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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kaltura.netkit.utils.ErrorElement;
import com.kaltura.playkit.PKDrmParams;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKPluginConfigs;
import com.kaltura.playkit.player.MediaSupport;
import com.kaltura.playkit.utils.GsonReader;
import com.kaltura.tvplayer.KalturaPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class BaseDemoActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, KalturaPlayer.OnEntryLoadListener {

    private static final PKLog log = PKLog.get("BaseDemoActivity");
    
    protected final Context context = this;
    protected JsonObject uiConf;
//    protected PlayerInitOptions initOptions;
    Integer uiConfId;
    String ks;
    DemoItem[] items;
    Integer uiConfPartnerId;     // for player config
    ViewGroup contentContainer;
    ListView itemListView;
    Integer partnerId;
    PKPluginConfigs pluginConfigs;
    String serverUrl;
    Boolean autoplay;
    Boolean preload;

    protected abstract DemoItem[] items();
    protected abstract DemoItem parseItem(JsonObject object);
    protected abstract void loadPlayerConfig();
    protected abstract String demoName();
    protected abstract void loadConfigFile();
    
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

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        contentContainer = findViewById(R.id.content_container);
        contentContainer.addView(getItemListView());
        navigationView.setCheckedItem(R.id.nav_gallery);
    }

    private PKPluginConfigs parsePluginConfigs(JsonObject json) {
        PKPluginConfigs configs = new PKPluginConfigs();
        // TODO: plugin-specific handling
        if (json != null) {
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                final String pluginName = entry.getKey();
                final JsonElement value = entry.getValue();
                configs.setPluginConfig(pluginName, value);
            }
        }
        return configs;
    }
    
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

    protected void parseCommonOptions(GsonReader reader) {

        GsonReader backendReader = reader.getReader("backend");
        partnerId = backendReader.getInteger("partnerId");
        ks = backendReader.getString("ks");
        serverUrl = backendReader.getString("serverUrl");

        GsonReader uiConf = reader.getReader("uiConf");
        if (uiConf != null) {
            Integer partnerId = uiConf.getInteger("partnerId");
            if (partnerId != null) {
                uiConfPartnerId = partnerId;
            } else {
                uiConfPartnerId = this.partnerId;
            }
            uiConfId = uiConf.getInteger("id");
        }

        GsonReader playback = reader.getReader("playback");
        autoplay = playback.getBoolean("autoplay");
        preload = playback.getBoolean("preload");


        final JsonArray jsonItems = reader.getArray("items");
        List<DemoItem> itemList = new ArrayList<>(jsonItems.size());
        for (JsonElement item : jsonItems) {
            final JsonObject object = item.getAsJsonObject();
            itemList.add(parseItem(object));
        }

        items = itemList.toArray(new DemoItem[itemList.size()]);

        this.pluginConfigs = parsePluginConfigs(reader.getObject("plugins"));
    }

    protected int partnerId() {
        return partnerId;
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

        return true;
    }
    
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        contentContainer.removeAllViews();

        switch (item.getItemId()) {
            case R.id.nav_gallery:
                contentContainer.addView(getItemListView());
                break;
            case R.id.nav_downloads:
                // TODO
                break;
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
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
                                loadItem(((DemoItem) parent.getItemAtPosition(position)));
                            }
                        }).show();
            }
        });

        return itemListView;
    }
}
