package com.kaltura.kalturaplayertestapp;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;
import android.widget.TextView;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.kaltura.kalturaplayertestapp.adapter.TestCaseConfigurationAdapter;
import com.kaltura.kalturaplayertestapp.models.Configuration;

public class JsonDetailActivity extends BaseActivity implements TestCaseConfigurationAdapter.OnJsonSelectedListener {

    private static final String TAG = "JsonDetailActivity";

    public static final String EXTRA_POST_KEY = "json_key";

    private FirebaseFirestore mFirestore;
    private RecyclerView mConfigurationsRecycler;
    private CollectionReference currenConfigurationRef;
    private Query mQuery;
    private TextView mEmptyView;
    private TestCaseConfigurationAdapter mAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_json_detail);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);


        String configuatioPath = getIntent().getExtras().getString(MainActivity.KEY_NEW_CONFIGURATION_PATH);
        if (configuatioPath == null) {
            throw new IllegalArgumentException("Must pass extra " + MainActivity.KEY_NEW_CONFIGURATION_PATH);
        }
        mConfigurationsRecycler = findViewById(R.id.details_recycler_configuration);

        ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(final RecyclerView.ViewHolder viewHolder, int direction) {
                final Configuration configuration = mAdapter.getSnapshots().get(viewHolder.getAdapterPosition()).toObject(Configuration.class);

                Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), configuration.getTitle() + " will be removed!", Snackbar.LENGTH_LONG);
                snackbar.setAction("Approve", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //mAdapter.removeItem(viewHolder.getAdapterPosition());

                    }
                });
                snackbar.setActionTextColor(Color.YELLOW);
                snackbar.show();
            }

            @Override
            public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
            }
        };

        // attaching the touch helper to recycler view
        new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(mConfigurationsRecycler);

        mFirestore = FirebaseFirestore.getInstance();
        currenConfigurationRef = mFirestore.collection(configuatioPath);
        mQuery = currenConfigurationRef.limit(MainActivity.LIMIT);
        mEmptyView = findViewById(R.id.view_empty);
        // Enable Firestore logging
        FirebaseFirestore.setLoggingEnabled(true);

        // RecyclerView
        mAdapter = new TestCaseConfigurationAdapter(mQuery, this) {
            @Override
            protected void onDataChanged() {
                // Show/hide content if the query returns empty.
                if (getItemCount() == 0) {
                    mConfigurationsRecycler.setVisibility(View.GONE);
                    mEmptyView.setVisibility(View.VISIBLE);
                } else {
                    mConfigurationsRecycler.setVisibility(View.VISIBLE);
                    mEmptyView.setVisibility(View.GONE);
                }
            }

            @Override
            protected void onError(FirebaseFirestoreException e) {
                // Show a snackbar on errors
                Snackbar.make(findViewById(android.R.id.content),"Error: check logs for info.", Snackbar.LENGTH_LONG).show();
            }
        };
        mConfigurationsRecycler.setLayoutManager(new LinearLayoutManager(this));
        mConfigurationsRecycler.setHasFixedSize(true);
        mConfigurationsRecycler.setAdapter(mAdapter);
        mAdapter.startListening();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }


    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();

        // Remove post value event listener
//        if (mPostListener != null) {
//            mPostReference.removeEventListener(mPostListener);
//        }

    }

    @Override
    public void onJsonSelected(Configuration configuration) {

        //Snackbar.make(findViewById(android.R.id.content), "Item Selected " + configuration.getId(), Snackbar.LENGTH_SHORT).show();
        Context context = this;
        if (configuration.getType() == Configuration.FOLDER) {
            //staticCollRef = collRef.document(configuration.getId()).collection("configurations");
            String path = currenConfigurationRef.document(configuration.getId()).collection("configurations").getPath();
            Class destinationClass = JsonDetailActivity.class;
            CollectionReference folder = currenConfigurationRef.document(configuration.getId()).collection("configurations");
            Intent intent = new Intent(context, destinationClass);
            intent.putExtra(MainActivity.KEY_NEW_CONFIGURATION_PATH, path);
            startActivity(intent);
            return;
        } else if (configuration.getType() == Configuration.JSON) {
            Class destinationClass = PlayerActivity.class;
            Intent intent = new Intent(context, destinationClass);
            intent.putExtra(PlayerActivity.PLAYER_CONFIG_TITLE_KEY, configuration.getTitle());
            intent.putExtra(PlayerActivity.PLAYER_CONFIG_JSON_KEY, configuration.getJson());
            startActivity(intent);
        }

    }
}
