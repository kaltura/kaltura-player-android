package com.kaltura.kalturaplayertestapp.adapter;

import android.app.Dialog;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.kaltura.kalturaplayertestapp.R;
import com.kaltura.kalturaplayertestapp.models.Configuration;
import com.kaltura.playkit.PKLog;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * RecyclerView adapter for a list of Restaurants.
 */
public class TestCaseConfigurationAdapter extends FirestoreAdapter<TestCaseConfigurationAdapter.ViewHolder> {
    private static final PKLog log = PKLog.get("TestCaseConfigurationAdapter");
    private Context context;

    public void removeItem(int adapterPosition) {
        getSnapshots().get(adapterPosition).getReference().delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                log.d( "Document Snapshot successfully deleted!");
            }
        });
    }

    public interface OnJsonSelectedListener {

        void onJsonSelected(Configuration configuration);

    }

    private OnJsonSelectedListener mListener;

    public TestCaseConfigurationAdapter(Query query, OnJsonSelectedListener listener) {
        super(query);
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return new ViewHolder(context, inflater.inflate(R.layout.item_json, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.bind(getSnapshot(position), mListener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private Context context;
        private TextView jsonTitle;
        private ImageView folderIcon;
        private ImageView infoIcon;

        public ViewHolder(Context context, View itemView) {
            super(itemView);
            this.context = context;
            this.jsonTitle = itemView.findViewById(R.id.json_title);
            this.folderIcon = itemView.findViewById(R.id.right_symbol);
            this.infoIcon = itemView.findViewById(R.id.info_symbol);
            infoIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });
        }

        public void bind(final DocumentSnapshot snapshot,
                         final OnJsonSelectedListener listener) {

            final Configuration configuration = snapshot.toObject(Configuration.class);
            configuration.setId(snapshot.getId());
            //Resources resources = itemView.getResources();
            Log.e("GILAD XXX","" + configuration.getTitle());
            jsonTitle.setText(configuration.getTitle());
            if (configuration.getType() == Configuration.FOLDER) {
                folderIcon.setVisibility(View.VISIBLE);
                infoIcon.setVisibility(View.GONE);
            } else {
                folderIcon.setVisibility(View.GONE);
                infoIcon.setVisibility(View.VISIBLE);
                infoIcon.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openJsonDialog(configuration.getTitle(), configuration.getJson());
                    }
                });
            }


            // Click listener
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (listener != null) {
                        listener.onJsonSelected(configuration);
                    }
                }
            });
        }

        private void openJsonDialog(String title, String json) {
            final Dialog dialog = new Dialog(context, R.style.FilterDialogTheme);
            dialog.setContentView(R.layout.dialog_layout);
            dialog.setTitle(title);
            TextView textViewUser = dialog.findViewById(R.id.txt);

            try {
                int spacesToIndentEachLevel = 2;
                JSONObject jsonObject;
                if (!TextUtils.isEmpty(json)) {
                    jsonObject= new JSONObject(json);
                    textViewUser.setText(jsonObject.toString(spacesToIndentEachLevel));
                }  else {
                    textViewUser.setText("Json is not valid!");
                }

                Button dialogButton = dialog.findViewById(R.id.dialogButton);
                dialogButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });
                dialog.show();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
