package com.android305.lights.adapters;

import android.content.res.ColorStateList;
import android.os.AsyncTask;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android305.lights.LampEditActivity_;
import com.android305.lights.R;
import com.android305.lights.dialogs.DeleteLampConfirmationDialog;
import com.android305.lights.fragments.GroupFragment;
import com.android305.lights.service.ClientService;
import com.android305.lights.service.LampUtils;
import com.android305.lights.util.Lamp;
import com.android305.lights.util.UIUtils;

import java.util.ArrayList;
import java.util.Arrays;

public class LampAdapter extends RecyclerView.Adapter<LampAdapter.ViewHolder> {
    private static final String TAG = LampAdapter.class.getName();
    private ClientService mService;
    private ArrayList<Lamp> mDataset;

    private GroupFragment mFragment;

    public LampAdapter(ClientService service, Lamp[] myDataset, GroupFragment fragment) {
        mService = service;
        mDataset = new ArrayList<>(Arrays.asList(myDataset));
        mFragment = fragment;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private Lamp mLamp;
        private CardView mCardView;

        private final ColorStateList originalColor;

        private ToggleLampTask mToggleLampTask;
        private DeleteLampTask mDeleteLampTask;
        private LampAdapter mAdapter;
        private GroupFragment mFragment;

        private RelativeLayout mInfoLayout;
        private Toolbar mToolbar;
        private ProgressBar mProgressSpinner;
        private TextView mIpAddress;
        private TextView mInverted;
        private TextView mErrorTxt;
        private TextView mError;
        private ImageButton mDelete;

        ViewHolder(LampAdapter adapter, CardView v) {
            super(v);
            mAdapter = adapter;
            mFragment = mAdapter.mFragment;
            originalColor = v.getCardBackgroundColor();
            mCardView = v;
            mInfoLayout = (RelativeLayout) v.findViewById(R.id.lamp_info);
            mToolbar = (Toolbar) v.findViewById(R.id.card_toolbar);
            mToolbar.inflateMenu(R.menu.menu_card_lamp);

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
            mIpAddress = ((TextView) mInfoLayout.findViewById(R.id.ip_address));
            mInverted = ((TextView) mInfoLayout.findViewById(R.id.inverted));
            mErrorTxt = ((TextView) mInfoLayout.findViewById(R.id.error_txt));
            mError = ((TextView) mInfoLayout.findViewById(R.id.error));
            mDelete = (ImageButton) mInfoLayout.findViewById(R.id.delete);

            mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    switch (menuItem.getItemId()) {
                        case R.id.action_toggle:
                            toggleLamp();
                            return true;
                        case R.id.action_edit:
                            LampEditActivity_.intent(mAdapter.mFragment.getContext()).mLamp(mLamp).mPosition(getAdapterPosition()).startForResult(GroupFragment.LAMP_UPDATE);
                            return true;
                        default:
                            return false;
                    }
                }
            });
            mDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DeleteLampConfirmationDialog dialog = DeleteLampConfirmationDialog.newInstance(mLamp.getName(), getAdapterPosition());
                    dialog.setTargetFragment(mFragment, 1);
                    dialog.show(mFragment.getChildFragmentManager(), DeleteLampConfirmationDialog.TAG);
                }
            });
        }

        void toggleLamp() {
            if (mToggleLampTask == null && mDeleteLampTask == null) {
                mToggleLampTask = new ToggleLampTask(mAdapter, mProgressSpinner, getAdapterPosition(), mLamp);
                mToggleLampTask.execute();
            }
        }

        void deleteLamp() {
            if (mDeleteLampTask == null && mToggleLampTask == null) {
                mDeleteLampTask = new DeleteLampTask(mAdapter, mProgressSpinner, getAdapterPosition(), mLamp);
                mDeleteLampTask.execute();
            }
        }

        void bindLamp(Lamp lamp) {
            mInfoLayout.setVisibility(View.GONE);
            mDeleteLampTask = null;
            mToggleLampTask = null;
            mLamp = lamp;
            mToolbar.setTitle(lamp.getName());
            mIpAddress.setText(lamp.getIpAddress());
            mInverted.setText(lamp.isInvert() ? mFragment.getString(R.string.yes) : mFragment.getString(R.string.no));
            if (lamp.getError() == null) {
                mErrorTxt.setVisibility(View.INVISIBLE);
                mError.setVisibility(View.INVISIBLE);
            } else {
                mErrorTxt.setVisibility(View.VISIBLE);
                mError.setVisibility(View.VISIBLE);
                mError.setText(lamp.getError() == null ? "No error" : lamp.getError());
            }
            if (lamp.getStatus() == Lamp.STATUS_ON) {
                mCardView.setCardBackgroundColor(ContextCompat.getColor(mFragment.getContext(), R.color.lamp_on));
                mProgressSpinner.setVisibility(View.GONE);
            } else if (lamp.getStatus() == Lamp.STATUS_PENDING) {
                mProgressSpinner.setVisibility(View.VISIBLE);
            } else if (lamp.getStatus() == Lamp.STATUS_ERROR) {
                mCardView.setCardBackgroundColor(ContextCompat.getColor(mFragment.getContext(), R.color.lamp_error));
                mProgressSpinner.setVisibility(View.GONE);
            } else {
                mCardView.setCardBackgroundColor(originalColor);
                mProgressSpinner.setVisibility(View.GONE);
            }
        }
    }

    public void toggleLamps() {
        for (int i = 0; i < mDataset.size(); i++) {
            Lamp lamp = get(i);
            ViewHolder vh = lamp.getBoundViewHolder();
            if (vh != null) {
                vh.toggleLamp();
            } else {
                Log.w(TAG, lamp.getName() + " is not bound.");
            }
        }
    }

    public void setData(Lamp[] lamps) {
        mDataset = new ArrayList<>(Arrays.asList(lamps));
        notifyDataSetChanged();
    }

    public void add(Lamp lamp) {
        mDataset.add(lamp);
        Log.d(TAG, "Add " + lamp.getName() + " Position " + (mDataset.size() - 1));
        notifyItemInserted(mDataset.size() - 1);
    }

    public void update(Lamp lamp, int position) {
        Log.d(TAG, "Update " + lamp.getName() + ". Position " + position);
        mDataset.set(position, lamp);
        notifyItemChanged(position);
    }

    public void delete(int position) {
        Log.d(TAG, "Delete position " + position);
        Lamp lamp = get(position);
        ViewHolder vh = lamp.getBoundViewHolder();
        if (vh != null) {
            vh.deleteLamp();
        } else {
            //We should not have gotten here, but if we do, let's go ahead and invalidate the entire dataset
            Log.w(TAG, lamp.getName() + " is not bound.");
            notifyDataSetChanged();
        }
    }

    private Lamp get(int position) {
        return mDataset.get(position);
    }

    private void remove(int position) {
        Log.d(TAG, "Remove position " + position);
        Lamp lamp = get(position);
        mDataset.remove(position);
        lamp.unbind();
        notifyItemRemoved(position);
    }

    @Override
    public LampAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_lamp, parent, false);
        return new ViewHolder(this, (CardView) itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Lamp lamp = mDataset.get(position);
        lamp.setBoundViewHolder(holder);
        holder.bindLamp(lamp);
    }

    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    private static class ToggleLampTask extends AsyncTask<Void, Void, Lamp> {
        ProgressBar mProgress;
        Lamp mLamp;
        int mPosition;
        LampAdapter mAdapter;

        ToggleLampTask(LampAdapter adapter, ProgressBar progress, int position, Lamp lamp) {
            mAdapter = adapter;
            mProgress = progress;
            mPosition = position;
            mLamp = lamp;
        }

        protected void onPreExecute() {
            mProgress.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(Lamp lamp) {
            mProgress.setVisibility(View.GONE);
            if (lamp != null) {
                mAdapter.update(lamp, mPosition);
                ViewHolder vh = mLamp.getBoundViewHolder();
                if (vh != null) {
                    vh.mToggleLampTask = null;
                } else {
                    Log.w(TAG, lamp.getName() + " is not bound.");
                }
            }
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

    private static class DeleteLampTask extends AsyncTask<Void, Void, Boolean> {
        ProgressBar mProgress;
        Lamp mLamp;
        int mPosition;
        LampAdapter mAdapter;

        DeleteLampTask(LampAdapter adapter, ProgressBar progress, int position, Lamp lamp) {
            mAdapter = adapter;
            mProgress = progress;
            mPosition = position;
            mLamp = lamp;
        }

        protected void onPreExecute() {
            mProgress.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(Boolean success) {
            mProgress.setVisibility(View.GONE);
            ViewHolder vh = mLamp.getBoundViewHolder();
            if (vh != null) {
                vh.mDeleteLampTask = null;
            } else {
                Log.w(TAG, mLamp.getName() + " is not bound.");
            }
            if (success) {
                mAdapter.remove(mPosition);
            } else {
                Toast.makeText(mAdapter.mFragment.getContext(), R.string.unknown_error_check_server_console, Toast.LENGTH_LONG).show();
            }
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