package com.android305.lights;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.android305.lights.util.Group;
import com.android305.lights.util.loaders.LampAndGroupLoader;
import com.android305.lights.util.ui.UpdateableFragment;

public class LampActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<SparseArray<Group>> {
    private static final String TAG = "LampActivity";
    private ClientService mService;
    private boolean mBound = false;
    private ProgressTask mProgressTask = null;
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lamp);
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        LocalBroadcastManager.getInstance(this).registerReceiver((mMessageReceiver), new IntentFilter(ClientService.FILTER));
    }

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver((mMessageReceiver), new IntentFilter(ClientService.FILTER));
        // Bind to LocalService
        Intent intent = new Intent(this, ClientService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_lamp, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_settings:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public class SectionsPagerAdapter extends FragmentStatePagerAdapter {
        private SparseArray<Group> mData;

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
            mData = new SparseArray<>();
        }

        public void setData(@NonNull SparseArray<Group> data) {
            mData = data;
            notifyDataSetChanged();
        }

        @Override
        public Fragment getItem(int position) {
            return GroupFragment.newInstance(mData.valueAt(position));
        }

        @Override
        public int getCount() {
            return mData.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mData.valueAt(position).getName();
        }

        @Override
        public int getItemPosition(Object object) {
            if (object instanceof UpdateableFragment) {
                ((UpdateableFragment) object).update(mData.get(((UpdateableFragment) object).getGroupId()));
            }
            //don't return POSITION_NONE, avoid fragment recreation.
            return super.getItemPosition(object);
        }
    }

    public static class GroupFragment extends Fragment implements UpdateableFragment {
        private static final String ARG_GROUP = "group";

        public static GroupFragment newInstance(@NonNull Group group) {
            GroupFragment fragment = new GroupFragment();
            Bundle args = new Bundle();
            args.putSerializable(ARG_GROUP, group);
            fragment.setArguments(args);
            return fragment;
        }

        private int groupId;
        private TextView title;

        public GroupFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            Bundle args;
            if (savedInstanceState != null) {
                args = savedInstanceState;
            } else {
                args = getArguments();
            }
            Group group = (Group) args.getSerializable(ARG_GROUP);
            if (group == null)
                throw new RuntimeException("Group was lost somewhere in the memory");
            groupId = group.getId();
            View rootView = inflater.inflate(R.layout.fragment_lamp, container, false);
            title = (TextView) rootView.findViewById(R.id.lamp_or_group_title);
            title.setText(group.getName());
            return rootView;
        }

        @Override
        public void update(Group group) {
            title.setText(group.getName());
        }

        @Override
        public int getGroupId() {
            return groupId;
        }
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            handleMessage(intent);
        }
    };

    private void handleMessage(Intent msg) {
        Bundle data = msg.getExtras();
        switch (data.getInt(ClientService.COMMAND, 0)) {
            case ClientService.LOST_CONNECTION:
                mProgressTask = new ProgressTask(true);
                mProgressTask.execute();
                break;
            case ClientService.LAMP_STATUS_NEEDS_REFRESH:
                getSupportLoaderManager().restartLoader(0, null, this);
                break;
            default:
                break;
        }
    }

    class ProgressTask extends AsyncTask<Void, Void, Integer> {
        private ProgressDialog dialog;
        private boolean background;

        public ProgressTask(boolean background) {
            this.background = background;
            if (!background)
                dialog = new ProgressDialog(LampActivity.this);
        }

        public ProgressTask(ProgressDialog dialog) {
            this.background = true;
            this.dialog = dialog;
        }

        protected void onPreExecute() {
            if (!background) {
                dialog.setMessage("Connection lost...");
                dialog.show();
            }
        }

        @Override
        protected void onPostExecute(final Integer code) {
            Intent i;
            switch (code) {
                case ClientService.ERROR_PASSWORD_INVALID:
                    mProgressTask = null;
                    i = new Intent(LampActivity.this, LoginActivity.class);
                    i.putExtra(LoginActivity.EXTRA_ASK_SETTINGS, true);
                    i.putExtra(LoginActivity.EXTRA_PASSWORD_INVALID, true);
                    startActivity(i);
                    break;
                case ClientService.ERROR_KEY_INVALID:
                    mProgressTask = null;
                    i = new Intent(LampActivity.this, LoginActivity.class);
                    i.putExtra(LoginActivity.EXTRA_ASK_SETTINGS, true);
                    i.putExtra(LoginActivity.EXTRA_KEY_INVALID, true);
                    startActivity(i);
                    break;
                case ClientService.ERROR_HOST_INVALID:
                    mProgressTask = new ProgressTask(dialog);
                    mProgressTask.execute();
                    break;
                case ClientService.ERROR_UNKNOWN:
                    mProgressTask = null;
                    Toast.makeText(getApplicationContext(), "Unknown error. Check server console.", Toast.LENGTH_SHORT).show();
                    break;
                case ClientService.SUCCESS:
                    mProgressTask = null;
                    if (!background && dialog.isShowing()) {
                        dialog.dismiss();
                    }
                    break;
            }
        }

        @Override
        protected Integer doInBackground(Void... args) {
            return mService.reconnect();
        }

        @Override
        protected void onCancelled() {
            mProgressTask = null;
            if (!background && dialog.isShowing())
                dialog.cancel();
        }
    }

    @Override
    public void onLoadFinished(Loader<SparseArray<Group>> loader, SparseArray<Group> data) {
        mSectionsPagerAdapter.setData(data);
    }

    @Override
    public void onLoaderReset(Loader<SparseArray<Group>> loader) {

    }

    @Override
    public Loader<SparseArray<Group>> onCreateLoader(int id, Bundle args) {
        return new LampAndGroupLoader(this, mService);
    }

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to ClientService, cast the IBinder and get LocalService instance
            ClientService.LocalBinder binder = (ClientService.LocalBinder) service;
            mService = binder.getService();
            getSupportLoaderManager().initLoader(0, null, LampActivity.this);
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

}