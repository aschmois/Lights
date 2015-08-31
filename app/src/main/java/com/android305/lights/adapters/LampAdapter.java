package com.android305.lights.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.android305.lights.R;
import com.android305.lights.service.ClientService;
import com.android305.lights.service.LampUtils;
import com.android305.lights.util.Lamp;

public class LampAdapter extends RecyclerView.Adapter<LampAdapter.ViewHolder> {
    private ClientService mService;
    private Lamp[] mDataset;
    SparseArray<ViewHolder> viewHolders = new SparseArray<>();

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public int mlampId;

        public Context mContext;
        public CardView mCardView;
        public Toolbar mToolbar;
        public ProgressBar mProgressSpinner;
        public final Drawable originalColor;

        public ToggleLampTask mToggleLampTask;
        private LampAdapter mAdapter;

        public ViewHolder(Context context, LampAdapter adapter, CardView v) {
            super(v);
            mAdapter = adapter;
            originalColor = v.getBackground();
            mContext = context;
            mCardView = v;
            mToolbar = (Toolbar) v.findViewById(R.id.card_toolbar);
            mToolbar.inflateMenu(R.menu.menu_card_lamp);
            mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    switch (menuItem.getItemId()) {
                        case R.id.action_toggle:
                            toggleLamp();
                            return true;
                        default:
                            return false;
                    }
                }
            });
            mProgressSpinner = (ProgressBar) mToolbar.findViewById(R.id.progress_spinner);
        }

        public void toggleLamp() {
            if (mToggleLampTask == null) {
                mToggleLampTask = new ToggleLampTask(this);
                mToggleLampTask.execute();
            }
        }
    }

    public void toggleLamps() {
        for (int i = 0; i < getItemCount(); i++) {
            viewHolders.get(i).toggleLamp();
        }
    }

    public LampAdapter(ClientService service, Lamp[] myDataset) {
        mService = service;
        mDataset = myDataset;
    }

    @Override
    public LampAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.lamp_card, parent, false);
        return new ViewHolder(parent.getContext(), this, (CardView) itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        viewHolders.put(position, holder);
        Lamp lamp = mDataset[position];
        holder.mlampId = lamp.getId();
        holder.mToolbar.setTitle(lamp.getName());
        if (lamp.getStatus() == Lamp.STATUS_ON) {
            holder.mCardView.setBackgroundResource(R.color.lamp_on);
            holder.mProgressSpinner.setVisibility(View.GONE);
        } else if (lamp.getStatus() == Lamp.STATUS_PENDING) {
            holder.mProgressSpinner.setVisibility(View.VISIBLE);
        } else if (lamp.getStatus() == Lamp.STATUS_ERROR) {
            holder.mCardView.setBackgroundResource(R.color.lamp_error);
            holder.mProgressSpinner.setVisibility(View.GONE);
        } else if (Build.VERSION.SDK_INT > 15) {
            holder.mCardView.setBackground(holder.originalColor);
            holder.mProgressSpinner.setVisibility(View.GONE);
        } else {
            //noinspection deprecation
            holder.mCardView.setBackgroundDrawable(holder.originalColor);
            holder.mProgressSpinner.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return mDataset.length;
    }

    static class ToggleLampTask extends AsyncTask<Void, Void, Lamp> {
        ProgressBar mProgress;
        Lamp mLamp;
        ViewHolder mViewHolder;
        int mPosition;
        LampAdapter mAdapter;

        public ToggleLampTask(ViewHolder viewHolder) {
            mAdapter = viewHolder.mAdapter;
            mProgress = viewHolder.mProgressSpinner;
            mPosition = viewHolder.getAdapterPosition();
            mLamp = mAdapter.mDataset[mPosition];
            mViewHolder = viewHolder;
        }

        protected void onPreExecute() {
            mProgress.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(Lamp lamp) {
            mProgress.setVisibility(View.GONE);
            if (lamp != null) {
                mAdapter.mDataset[mPosition] = lamp;
                mAdapter.onBindViewHolder(mViewHolder, mPosition);
            }
            mViewHolder.mToggleLampTask = null;
        }

        @Override
        protected Lamp doInBackground(Void... args) {
            return LampUtils.toggleLamp(mAdapter.mService, mLamp);
        }

        @Override
        protected void onCancelled() {
            mProgress.setVisibility(View.GONE);
        }
    }
}