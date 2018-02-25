package com.kaltura.kalturaplayertestapp;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

/**
 * Created by gilad.nadav on 2/25/18.
 */

public class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.EventItemViewHolder> {

    private static final String TAG = EventsAdapter.class.getSimpleName();

    private static int viewHolderCount;
    private int numberOfItems;
    private List<String> eventsList;


    public EventsAdapter(int numberOfItems) {
        this.numberOfItems = numberOfItems;
        viewHolderCount = 0;
    }
    @Override
    public EventItemViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        Context context = viewGroup.getContext();
        int layoutIdForListItem = R.layout.event_list_item;
        LayoutInflater inflater = LayoutInflater.from(context);
        boolean shouldAttachToParentImmediately = false;

        View view = inflater.inflate(layoutIdForListItem, viewGroup, shouldAttachToParentImmediately);
        EventItemViewHolder viewHolder = new EventItemViewHolder(view);



        viewHolder.itemView.setBackgroundColor(Color.LTGRAY);

        viewHolderCount++;
        Log.d(TAG, "onCreateViewHolder: number of ViewHolders created: "
                + viewHolderCount);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(EventItemViewHolder holder, int position) {
        Log.d(TAG, "#" + position);
        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        return(null != eventsList ? eventsList.size()  :0);

    }

    public void notifyData(List<String> eventsList) {
        Log.d("notifyData ", eventsList.size() + "");
        this.eventsList = eventsList;
        notifyDataSetChanged();
    }

    public class EventItemViewHolder extends RecyclerView.ViewHolder {

        TextView eventNumberView;
        TextView eventDesc;

        public EventItemViewHolder(View itemView) {
            super(itemView);

            eventNumberView = itemView.findViewById(R.id.event_item_number);
            eventDesc = itemView.findViewById(R.id.event_view_holder_instance);
        }

        void bind(int listIndex) {
            eventDesc.setText(eventsList.get(listIndex));
            eventNumberView.setText(String.valueOf(listIndex) + ":");
        }
    }
}
