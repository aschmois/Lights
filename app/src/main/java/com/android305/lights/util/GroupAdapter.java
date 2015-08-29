package com.android305.lights.util;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.android305.lights.R;

public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.ViewHolder> {
    private Lamp[] mDataset;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public Context mContext;
        public CardView cardView;
        public TextView lampName;
        public TextView ipAddress;
        public Button toggle;
        public final Drawable originalColor;

        public ViewHolder(Context context, CardView v) {
            super(v);
            originalColor = v.getBackground();
            mContext = context;
            cardView = v;
            lampName = (TextView) v.findViewById(R.id.lamp_name);
            ipAddress = (TextView) v.findViewById(R.id.ip_address);
            toggle = (Button) v.findViewById(R.id.toggle);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public GroupAdapter(Lamp[] myDataset) {
        mDataset = myDataset;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public GroupAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.lamp_card, parent, false);
        return new ViewHolder(parent.getContext(), (CardView) itemView);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        Lamp lamp = mDataset[position];
        holder.lampName.setText(lamp.getName());
        holder.ipAddress.setText(lamp.getIpAddress());
        if (lamp.isStatus()) {
            holder.cardView.setBackgroundResource(R.color.lamp_on);
        } else if (Build.VERSION.SDK_INT > 15) {
            holder.cardView.setBackground(holder.originalColor);
        } else {
            //noinspection deprecation
            holder.cardView.setBackgroundDrawable(holder.originalColor);
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.length;
    }
}