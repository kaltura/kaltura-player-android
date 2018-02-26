package com.kaltura.kalturaplayertestapp.tracks;

import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;
import com.kaltura.kalturaplayertestapp.R;
import java.util.List;

public class TracksAdapter extends RecyclerView.Adapter<TracksAdapter.ViewHolder> {

    private List<TrackItem> trackItems;
    private String selectedItemUniqueId;
    private int lastTrackSelection;

    TracksAdapter(List<?> trackItems, int lastTrackSelection) {
        if (trackItems.size() > 0) {
            if (trackItems.get(0) instanceof TrackItem) {
                this.trackItems = (List<TrackItem>) trackItems;
                selectedItemUniqueId = this.trackItems.get(lastTrackSelection).getUniqueId();
            }
        }
        this.lastTrackSelection = lastTrackSelection;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ConstraintLayout layout = (ConstraintLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.track_selection_row_item, parent, false);

        return new ViewHolder(layout);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        holder.radioButton.setChecked(position == lastTrackSelection);
            holder.textView.setText(trackItems.get(position).getTrackDescription());
    }


    @Override
    public int getItemCount() {
        if (trackItems != null) {
            return trackItems.size();
        }
        return 0;
    }

    String getTrackItemId() {
        return selectedItemUniqueId;
    }

    int getLastTrackSelection() {
        return lastTrackSelection;
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private TextView textView;
        private RadioButton radioButton;

        ViewHolder(ConstraintLayout layout) {
            super(layout);
            textView = layout.findViewById(R.id.tvTrackDescription);
            radioButton = layout.findViewById(R.id.rbTrackItem);

            radioButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    lastTrackSelection = getAdapterPosition();
                    if (trackItems != null) {
                        selectedItemUniqueId = trackItems.get(lastTrackSelection).getUniqueId();
                        notifyItemRangeChanged(0, trackItems.size());
                    }
                }
            });
        }
    }
}



