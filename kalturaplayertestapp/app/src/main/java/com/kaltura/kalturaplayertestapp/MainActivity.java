package com.kaltura.kalturaplayertestapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import com.kaltura.kalturaplayertestapp.adapter.JsonAdapter;
import com.kaltura.kalturaplayertestapp.models.Configuration;

public class MainActivity extends AppCompatActivity implements JsonAdapter.OnJsonSelectedListener {

    private static final String TAG = "MainActivity";
    public static final String KEY_NEW_CONFIGURATION_PATH = "key_new_configuration_path";
    public static final String KEY_JSON_STRING = "key_json_string";
    public static final int LIMIT = 50;

    private RecyclerView mConfigurationsRecycler;
    private TextView mEmptyView;

    private FirebaseFirestore mFirestore;
    private CollectionReference collRef;
    private Query mQuery;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private JsonAdapter mAdapter;

    @Override
    protected void onStart() {
        super.onStart();
        mFirestore.collection("users").document(currentUser.getUid()).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(DocumentSnapshot documentSnapshot, FirebaseFirestoreException e) {
                String name = documentSnapshot.getString("name");
                Log.d(TAG, "ZZZ " + name);

                CollectionReference ref = documentSnapshot.getReference().collection("configurations");
            }

        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        mConfigurationsRecycler = findViewById(R.id.recycler_configuration);
        mEmptyView = findViewById(R.id.view_empty);
        // Enable Firestore logging
        FirebaseFirestore.setLoggingEnabled(true);

        // Firestore
        mFirestore = FirebaseFirestore.getInstance();
        //if (staticCollRef == null) {
        collRef = mFirestore.collection("users").document(currentUser.getUid()).collection("configurations");
        //} else {
        //    collRef = staticCollRef;
        //}
        DocumentReference docs = mFirestore.collection("users").document(currentUser.getUid());
        mQuery = collRef
                // .orderBy("avgRating", Query.Direction.DESCENDING)
                .limit(LIMIT);

        // RecyclerView
        mAdapter = new JsonAdapter(mQuery, this) {
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
                Snackbar.make(findViewById(android.R.id.content),
                        "Error: check logs for info.", Snackbar.LENGTH_LONG).show();
            }
        };

        mConfigurationsRecycler.setLayoutManager(new LinearLayoutManager(this));
        mConfigurationsRecycler.setHasFixedSize(true);
        mConfigurationsRecycler.setAdapter(mAdapter);
        mAdapter.startListening();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_items:
                onAddItemsClicked();
                break;
            case R.id.action_delete:
                mFirestore.collection("users").document(currentUser.getUid()).collection("configurations");
                break;
            case R.id.action_add_folder:
                mFirestore.collection("users").document(currentUser.getUid()).collection("configurations");
                break;
            case R.id.action_add_json:
                mFirestore.collection("users").document(currentUser.getUid()).collection("configurations");
                break;
            case R.id.action_remove_folder:
                mFirestore.collection("users").document(currentUser.getUid()).collection("configurations");
                break;
            case R.id.action_remove_json:
                mFirestore.collection("users").document(currentUser.getUid()).collection("configurations");
                break;
            case R.id.action_logout:
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(this, SignInActivity.class));
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void onAddItemsClicked() {
        // Add a bunch of random restaurants
        CollectionReference colRffff = mFirestore.collection("users").document(currentUser.getUid()).collection("configurations");

        Configuration folder = new Configuration();
        folder.setType(1);
        folder.setTitle("folder1");
        colRffff.add(folder).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                                                       @Override
                                                       public void onComplete(@NonNull Task<DocumentReference> task) {
                                                           WriteBatch batch = mFirestore.batch();
                                                           for (int i = 0; i < 10; i++) {
                                                               DocumentReference restRef = mFirestore.collection("users").document(currentUser.getUid()).collection("configurations").document();
                                                               Configuration randomConfig = ConfigurationUtil.getRandom(getApplicationContext());
                                                               task.getResult().collection("configurations").add(randomConfig);
                                                           }
                                                       }
                                                   }
        );
    }

    @Override
    public void onJsonSelected(Configuration configuration) {
        Snackbar.make(findViewById(android.R.id.content),
                "Item Selected " + configuration.getId(), Snackbar.LENGTH_SHORT).show();
        Context context = this;
        if (configuration.getType() == Configuration.FOLDER) {
            //staticCollRef = collRef.document(configuration.getId()).collection("configurations");
            String path = collRef.document(configuration.getId()).collection("configurations").getPath();
            Class destinationClass = JsonDetailActivity.class;
            CollectionReference folder = collRef.document(configuration.getId()).collection("configurations");
            Intent intent = new Intent(context, destinationClass);
            intent.putExtra(KEY_NEW_CONFIGURATION_PATH, path);
            startActivity(intent);
            return;
        } else if (configuration.getType() == Configuration.JSON) {
            Class destinationClass = JsonDetailActivity.class;
            Intent intent = new Intent(context, destinationClass);
            intent.putExtra(KEY_JSON_STRING, configuration.getJson());

            // DocumentReference alovelaceDocumentRef = db.document("users/alovelace");
            startActivity(intent);
        }
    }


//    private void onAddItemsClicked() {
//        // Add a bunch of random restaurants
//        List<Configuration> list = new ArrayList<>();
//        for (int i = 0; i < 10; i++) {
//            Configuration randomConfig = ConfigurationUtil.getRandom(this, i + "");
//            list.add(randomConfig);
//        }
//        WriteBatch batch = mFirestore.batch();
//        for (int i = 0; i < 10; i++) {
//            DocumentReference restRef = mFirestore.collection("users").document(currentUser.getUid()).collection("configurations").document();
//
//            // Create random restaurant / ratings
//            Configuration randomConfig = ConfigurationUtil.getRandom(this,restRef.getId());
//            randomConfig.setJson("");
//            // Add restaurant
//            batch.set(restRef, randomConfig);
//
//            // Add ratings to subcollection
//            for (Configuration rating : list) {
//                rating.setConfigurationType(0);
//                DocumentReference ratingRef = restRef.collection("configurations").document();
//                rating.setId(ratingRef.getId());
//                batch.set(ratingRef, rating);
//            }
//        }
//        batch.commit().addOnCompleteListener(new OnCompleteListener<Void>() {
//            @Override
//            public void onComplete(@NonNull Task<Void> task) {
//                if (task.isSuccessful()) {
//                    Log.d(TAG, "Write batch succeeded.");
//                } else {
//                    Log.w(TAG, "write batch failed.", task.getException());
//                }
//            }
//        });
//    }

}
