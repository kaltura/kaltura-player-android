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

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.vision.barcode.Barcode;
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
import com.google.gson.Gson;
import com.kaltura.kalturaplayertestapp.adapter.TestConfigurationAdapter;
import com.kaltura.kalturaplayertestapp.converters.TestDescriptor;
import com.kaltura.kalturaplayertestapp.models.Configuration;
import com.kaltura.kalturaplayertestapp.qrcode.BarcodeCaptureActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;


public class MainActivity extends AppCompatActivity implements TestConfigurationAdapter.OnJsonSelectedListener {

    private static final String TAG = "MainActivity";
    private static final int RC_BARCODE_CAPTURE = 9001;
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
    private TestConfigurationAdapter mAdapter;
    private String newPath;

    @Override
    protected void onStart() {
        super.onStart();
        mFirestore.collection("users").document(currentUser.getUid()).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(DocumentSnapshot documentSnapshot, FirebaseFirestoreException e) {
                if (documentSnapshot == null ) {
                    Log.e(TAG, "ZZZ documentSnapshot = null");
                    return;
                }
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
        mAdapter = new TestConfigurationAdapter(mQuery, this) {
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
            case R.id.add_items_scan:
                onScanItemsClicked();
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

    private void onScanItemsClicked() {
        Intent intent = new Intent(this, BarcodeCaptureActivity.class);
        //intent.putExtra(BarcodeCaptureActivity.AutoFocus, autoFocus.isChecked());
        //intent.putExtra(BarcodeCaptureActivity.UseFlash, useFlash.isChecked());

        startActivityForResult(intent, RC_BARCODE_CAPTURE);
    }

    private void onAddItemsClicked() {
        // Add a bunch of random restaurants
        CollectionReference collectionReference = mFirestore.collection("users").document(currentUser.getUid()).collection("configurations");
        Configuration folder = new Configuration();
        folder.setType(1);
        folder.setTitle("folder1");
        collectionReference.add(folder).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
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
            Class destinationClass = PlayerActivity.class;
            Intent intent = new Intent(context, destinationClass);
            intent.putExtra(PlayerActivity.PLAYER_CONFIG_TITLE_KEY, configuration.getTitle());
            intent.putExtra(PlayerActivity.PLAYER_CONFIG_JSON_KEY, configuration.getJson());
            startActivity(intent);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_BARCODE_CAPTURE) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {
                    Barcode barcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);
                    //barcodeValue.setText(barcode.displayValue);
                    Log.d(TAG, "Barcode read: " + barcode.displayValue);
                    String jsonTests = "";
                    try {
                        jsonTests =  new DownloadFileFromURL().execute(barcode.displayValue).get();
                        Log.d("XXX", jsonTests);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }

                    Gson gson = new Gson();
                    TestDescriptor[] testDescriptors = gson.fromJson(jsonTests, TestDescriptor[].class);

                    for (TestDescriptor test : testDescriptors) {
                        Log.d(TAG, "got Test " + test.getTitle());
                        String [] splittedPath = test.getUrl().split("standalonePlayer/");
                        List<String> testPath = new ArrayList<>();
                        if (splittedPath.length > 1) {
                            String[] testPathParts = splittedPath[1].split("/");
                            if (testPathParts.length > 0)
                                for (int i = 0; i < testPathParts.length - 1; i++) {
                                    testPath.add(testPathParts[i]);
                                }
                        }
                        try {
                            String jsonTest =  new DownloadFileFromURL().execute(test.getUrl()).get();
                            Configuration testConfig = new Configuration();
                            testConfig.setType(0);
                            testConfig.setTitle(test.getTitle());
                            testConfig.setJson(jsonTest);
                            CollectionReference collectionReference = mFirestore.collection("users").document(currentUser.getUid()).collection("configurations");
                            buildFoldersHirarchy(collectionReference, testPath, testConfig);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        }
                    }
                    //Log.d(TAG, tests.keySet().toString());


                } else {
                    // statusMessage.setText(R.string.barcode_failure);
                    Log.d(TAG, "No barcode captured, intent data is null");
                }
            } else {
                Log.e(TAG, "Error, " +  CommonStatusCodes.getStatusCodeString(resultCode));
            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void buildFoldersHirarchy(CollectionReference collectionReference, final List<String> subFolder, final Configuration testConfig) {
        Configuration folder = new Configuration();
        if (subFolder.size() == 0) {
            folder = testConfig;
            collectionReference.add(folder);
            return;
        } else {
            folder.setType(1);
            String fold = subFolder.get(0);
            subFolder.remove(0);
            folder.setTitle(fold);
        }
        collectionReference.add(folder).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                                                                  @Override
                                                                  public void onComplete(@NonNull Task<DocumentReference> task) {
                                                                      newPath = task.getResult().collection("configurations").getPath();
                                                                      if (subFolder.size() == 0) {
                                                                          task.getResult().collection("configurations").add(testConfig);
                                                                      } else {
                                                                          buildFoldersHirarchy(mFirestore.collection(newPath), subFolder, testConfig);
                                                                      }
                                                                  }
                                                              }
        );
    }

//    private void onAddItemsClicked1() {
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
//            for (Configuration config : list) {
//                config.setType(0);
//                DocumentReference ConfigurationRef = restRef.collection("configurations").document();
//                config.setId(ConfigurationRef.getId());
//                batch.set(ConfigurationRef, config);
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
