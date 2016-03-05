package com.android305.lights.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android305.lights.R;
import com.android305.lights.TimerEditActivity_;
import com.android305.lights.dialogs.DeleteLampConfirmationDialog;
import com.android305.lights.dialogs.DeleteTimerConfirmationDialog;
import com.android305.lights.fragments.GroupFragment;
import com.android305.lights.service.ClientService;
import com.android305.lights.service.TimerUtils;
import com.android305.lights.util.Timer;

import java.sql.Time;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;

public class TimerAdapter extends RecyclerView.Adapter<TimerAdapter.ViewHolder> {
    private static final String TAG = TimerAdapter.class.getName();
    private ClientService mService;
    private final GroupFragment mFragment;
    private ArrayList<Timer> mDataset;

    public TimerAdapter(ClientService service, Timer[] myDataset, GroupFragment fragment) {
        mService = service;
        mDataset = new ArrayList<>(Arrays.asList(myDataset));
        mFragment = fragment;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public Timer mTimer;

        private DeleteTimerTask mDeleteTimerTask;
        public TimerAdapter mAdapter;
        public CardView mCardView;
        public Toolbar mToolbar;
        public ProgressBar mProgressSpinner;
        public final Drawable originalColor;

        public ViewHolder(TimerAdapter adapter, CardView v) {
            super(v);
            originalColor = v.getBackground();
            mAdapter = adapter;
            mCardView = v;
            mToolbar = (Toolbar) v.findViewById(R.id.card_toolbar);
            mToolbar.inflateMenu(R.menu.menu_card_timer);
            mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    switch (menuItem.getItemId()) {
                        case R.id.action_edit:
                            TimerEditActivity_.intent(mAdapter.mFragment.getContext()).mTimer(mTimer).mPosition(getAdapterPosition()).startForResult(GroupFragment.TIMER_UPDATE);
                            return true;
                        case R.id.action_delete:
                            DateFormat df = DateFormat.getTimeInstance();
                            String timerName = String.format("%s - %s", df.format(mTimer.getStart()), df.format(mTimer.getEnd()));
                            DeleteTimerConfirmationDialog dialog = DeleteTimerConfirmationDialog.newInstance(timerName, getAdapterPosition());
                            dialog.setTargetFragment(mAdapter.mFragment, 1);
                            dialog.show(mAdapter.mFragment.getChildFragmentManager(), DeleteTimerConfirmationDialog.TAG);
                            return true;
                        default:
                            return false;
                    }
                }
            });
            mProgressSpinner = (ProgressBar) mToolbar.findViewById(R.id.progress_spinner);
        }

        public void bindTimer(Timer timer) {
            Context c = mAdapter.mFragment.getContext();
            TextView titleView = ((TextView) mToolbar.findViewById(R.id.timer));
            TextView daysView = ((TextView) mToolbar.findViewById(R.id.timer_days));
            mTimer = timer;
            mDeleteTimerTask = null;
            Time start = timer.getStart();
            Time end = timer.getEnd();
            DateFormat df = DateFormat.getTimeInstance();
            titleView.setText(String.format("%s - %s", df.format(start), df.format(end)));
            String days;
            if (mTimer.isEveryday()) {
                days = c.getString(R.string.everyday);
            } else if (mTimer.isWeekdays()) {
                days = c.getString(R.string.weekdays);
            } else if (mTimer.isWeekend()) {
                days = c.getString(R.string.weekend);
            } else {
                ArrayList<String> list = new ArrayList<>();
                if (mTimer.isSunday())
                    list.add("S");
                if (mTimer.isMonday())
                    list.add("M");
                if (mTimer.isTuesday())
                    list.add("T");
                if (mTimer.isWednesday())
                    list.add("W");
                if (mTimer.isThursday())
                    list.add("TH");
                if (mTimer.isFriday())
                    list.add("F");
                if (mTimer.isSaturday())
                    list.add("SA");
                days = TextUtils.join(",", list);
            }
            daysView.setText(days);
            if (timer.getStatus() == 1) {
                mCardView.setBackgroundResource(R.color.lamp_on);
            } else if (timer.getStatus() == 2) {
                mProgressSpinner.setVisibility(View.VISIBLE);
            } else if (timer.getStatus() == 3) {
                mCardView.setBackgroundResource(R.color.lamp_error);
            } else if (Build.VERSION.SDK_INT > 15) {
                mCardView.setBackground(originalColor);
            } else {
                //noinspection deprecation
                mCardView.setBackgroundDrawable(originalColor);
            }
        }

        public void deleteTimer() {
            if (mDeleteTimerTask == null) {
                mDeleteTimerTask = new DeleteTimerTask(mAdapter, mProgressSpinner, getAdapterPosition(), mTimer);
                mDeleteTimerTask.execute();
            }
        }
    }

    @Override
    public TimerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.timer_card, parent, false);
        return new ViewHolder(this, (CardView) itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Timer timer = mDataset.get(position);
        timer.setBoundViewHolder(holder);
        holder.bindTimer(timer);
    }

    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    private Timer get(int position) {
        return mDataset.get(position);
    }

    public void add(Timer timer) {
        mDataset.add(timer);
        notifyItemInserted(mDataset.size() - 1);
    }

    public void update(Timer timer, int position) {
        mDataset.set(position, timer);
        notifyItemChanged(position);
    }

    public void delete(int position) {
        Log.d(TAG, "Delete position " + position);
        Timer timer = get(position);
        ViewHolder vh = timer.getBoundViewHolder();
        if (vh != null) {
            vh.deleteTimer();
        } else {
            //We should not have gotten here, but if we do, let's go ahead and invalidate the entire dataset
            Log.w(TAG, timer.getId() + " timer is not bound.");
            notifyDataSetChanged();
        }
    }

    private void remove(int position) {
        Log.d(TAG, "Remove position " + position);
        Timer timer = get(position);
        mDataset.remove(position);
        timer.unbind();
        notifyItemRemoved(position);
        notifyDataSetChanged();
    }

    static class DeleteTimerTask extends AsyncTask<Void, Void, Boolean> {
        ProgressBar mProgress;
        Timer mTimer;
        int mPosition;
        TimerAdapter mAdapter;

        public DeleteTimerTask(TimerAdapter adapter, ProgressBar progress, int position, Timer timer) {
            mAdapter = adapter;
            mProgress = progress;
            mPosition = position;
            mTimer = timer;
        }

        protected void onPreExecute() {
            mProgress.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(Boolean success) {
            mProgress.setVisibility(View.GONE);
            ViewHolder vh = mTimer.getBoundViewHolder();
            if (vh != null) {
                vh.mDeleteTimerTask = null;
            } else {
                Log.w(TAG, mTimer.getId() + " timer is not bound.");
            }
            if (success) {
                mAdapter.remove(mPosition);
            } else {
                Toast.makeText(mAdapter.mFragment.getContext(), R.string.unknown_error_check_server_console, Toast.LENGTH_LONG).show();
            }
        }

        @Override
        protected Boolean doInBackground(Void... args) {
            return TimerUtils.deleteTimer(mAdapter.mService, mTimer);
        }

        @Override
        protected void onCancelled() {
            mProgress.setVisibility(View.GONE);
        }
    }
}