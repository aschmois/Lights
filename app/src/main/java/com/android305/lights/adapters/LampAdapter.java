package com.android305.lights.adapters;

import android.content.Intent;
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
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android305.lights.LampEditActivity;
import com.android305.lights.R;
import com.android305.lights.dialogs.DeleteConfirmationDialog;
import com.android305.lights.fragments.GroupFragment;
import com.android305.lights.service.ClientService;
import com.android305.lights.service.LampUtils;
import com.android305.lights.util.Lamp;
import com.android305.lights.util.UIUtils;

import java.util.ArrayList;
import java.util.Arrays;

public class LampAdapter extends RecyclerView.Adapter<LampAdapter.ViewHolder> {
    private ClientService mService;
    private ArrayList<Lamp> mDataset;
    SparseArray<ViewHolder> viewHolders = new SparseArray<>();

    private GroupFragment mFragment;

    public LampAdapter(ClientService service, Lamp[] myDataset, GroupFragment fragment) {
        mService = service;
        mDataset = new ArrayList<>(Arrays.asList(myDataset));
        mFragment = fragment;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private CardView mCardView;
        private Toolbar mToolbar;
        private ProgressBar mProgressSpinner;
        private final Drawable originalColor;

        private ToggleLampTask mToggleLampTask;
        private DeleteLampTask mDeleteLampTask;
        private LampAdapter mAdapter;
        private RelativeLayout mInfoLayout;

        public ViewHolder(LampAdapter adapter, CardView v) {
            super(v);
            mAdapter = adapter;
            originalColor = v.getBackground();
            mCardView = v;
            mInfoLayout = (RelativeLayout) v.findViewById(R.id.lamp_info);
            mToolbar = (Toolbar) v.findViewById(R.id.card_toolbar);
            mToolbar.inflateMenu(R.menu.menu_card_lamp);

            ImageButton collapse = (ImageButton) mInfoLayout.findViewById(R.id.collapse);
            collapse.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    UIUtils.collapse(mInfoLayout);
                }
            });

            mToolbar.setClickable(true);
            mToolbar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mInfoLayout.getVisibility() == View.GONE)
                        UIUtils.expand(mInfoLayout);
                    else
                        UIUtils.collapse(mInfoLayout);
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

        public void deleteLamp() {
            if (mDeleteLampTask == null) {
                mDeleteLampTask = new DeleteLampTask(this);
                mDeleteLampTask.execute();
            }
        }
    }

    public void toggleLamps() {
        for (int i = 0; i < viewHolders.size(); i++) {
            viewHolders.valueAt(i).toggleLamp();
        }
    }

    public void add(Lamp lamp) {
        mDataset.add(lamp);
        notifyItemChanged(mDataset.size() - 1);
    }

    public void update(Lamp lamp, int position) {
        mDataset.set(position, lamp);
        notifyItemChanged(position);
    }

    public void delete(int position) {
        viewHolders.get(position).deleteLamp();
    }

    private void remove(int position) {
        mDataset.remove(position);
        viewHolders.delete(position);
        notifyItemRemoved(position);
    }

    @Override
    public LampAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.lamp_card, parent, false);
        return new ViewHolder(this, (CardView) itemView);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        viewHolders.put(position, holder);
        final Lamp lamp = mDataset.get(position);
        holder.mToolbar.setTitle(lamp.getName());
        ((TextView) holder.mInfoLayout.findViewById(R.id.ip_address)).setText(lamp.getIpAddress());
        ((TextView) holder.mInfoLayout.findViewById(R.id.inverted)).setText(lamp.isInvert() ? mFragment.getString(R.string.yes) : mFragment.getString(R.string.no));
        holder.mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.action_toggle:
                        holder.toggleLamp();
                        return true;
                    case R.id.action_edit:
                        Intent i = new Intent(mFragment.getContext(), LampEditActivity.class);
                        i.putExtra(LampEditActivity.EXTRA_LAMP, lamp);
                        i.putExtra(LampEditActivity.EXTRA_POSITION, position);
                        mFragment.startActivityForResult(i, GroupFragment.LAMP_UPDATE);
                        return true;
                    default:
                        return false;
                }
            }
        });
        Button delete = (Button) holder.mInfoLayout.findViewById(R.id.delete);
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DeleteConfirmationDialog dialog = DeleteConfirmationDialog.newInstance(lamp.getName(), position);
                dialog.setTargetFragment(mFragment, 1);
                dialog.show(mFragment.getChildFragmentManager(), "dialog_deleteConfirmation");
            }
        });
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
        return mDataset.size();
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
            mLamp = mAdapter.mDataset.get(mPosition);
            mViewHolder = viewHolder;
        }

        protected void onPreExecute() {
            mProgress.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(Lamp lamp) {
            mProgress.setVisibility(View.GONE);
            if (lamp != null) {
                mAdapter.update(lamp, mPosition);
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

    static class DeleteLampTask extends AsyncTask<Void, Void, Boolean> {
        ProgressBar mProgress;
        Lamp mLamp;
        ViewHolder mViewHolder;
        int mPosition;
        LampAdapter mAdapter;

        public DeleteLampTask(ViewHolder viewHolder) {
            mAdapter = viewHolder.mAdapter;
            mProgress = viewHolder.mProgressSpinner;
            mPosition = viewHolder.getAdapterPosition();
            mLamp = mAdapter.mDataset.get(mPosition);
            mViewHolder = viewHolder;
        }

        protected void onPreExecute() {
            mProgress.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(Boolean success) {
            mProgress.setVisibility(View.GONE);
            if (success) {
                mAdapter.remove(mPosition);
            } else {
                Toast.makeText(mAdapter.mFragment.getContext(), R.string.unknown_error_check_server_console, Toast.LENGTH_LONG).show();
            }
            mViewHolder.mToggleLampTask = null;
        }

        @Override
        protected Boolean doInBackground(Void... args) {
            return LampUtils.deleteLamp(mAdapter.mService, mLamp);
        }

        @Override
        protected void onCancelled() {
            mProgress.setVisibility(View.GONE);
        }
    }
}