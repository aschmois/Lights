package com.android305.lights.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.android305.lights.R;
import com.android305.lights.util.Timer;

import java.sql.Time;
import java.text.DateFormat;

public class TimerAdapter extends RecyclerView.Adapter<TimerAdapter.ViewHolder> {
    private Timer[] mDataset;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public int mTimerId;

        public Context mContext;
        public CardView mCardView;
        public Toolbar mToolbar;
        public ProgressBar mProgressSpinner;
        public final Drawable originalColor;

        public ViewHolder(Context context, CardView v) {
            super(v);
            originalColor = v.getBackground();
            mContext = context;
            mCardView = v;
            mToolbar = (Toolbar) v.findViewById(R.id.card_toolbar);
            mToolbar.inflateMenu(R.menu.menu_card_timer);
            mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    return true;
                }
            });
            mProgressSpinner = (ProgressBar) mToolbar.findViewById(R.id.progress_spinner);
        }
    }

    public TimerAdapter(Timer[] myDataset) {
        mDataset = myDataset;
    }

    @Override
    public TimerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.lamp_card, parent, false);
        return new ViewHolder(parent.getContext(), (CardView) itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Timer timer = mDataset[position];
        holder.mTimerId = timer.getId();
        Time start = timer.getStart();
        Time end = timer.getEnd();
        DateFormat df = DateFormat.getTimeInstance();
        holder.mToolbar.setTitle(df.format(start) + " - " + df.format(end));
        if (timer.getStatus() == 1) {
            holder.mCardView.setBackgroundResource(R.color.lamp_on);
        } else if (timer.getStatus() == 2) {
            holder.mProgressSpinner.setVisibility(View.VISIBLE);
        } else if (timer.getStatus() == 3) {
            holder.mCardView.setBackgroundResource(R.color.lamp_error);
        } else if (Build.VERSION.SDK_INT > 15) {
            holder.mCardView.setBackground(holder.originalColor);
        } else {
            //noinspection deprecation
            holder.mCardView.setBackgroundDrawable(holder.originalColor);
        }
    }

    @Override
    public int getItemCount() {
        return mDataset.length;
    }
}