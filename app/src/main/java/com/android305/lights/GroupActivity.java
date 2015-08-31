package com.android305.lights;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android305.lights.adapters.SectionsPagerAdapter;
import com.android305.lights.interfaces.ActivityAttachService;
import com.android305.lights.service.ClientService;
import com.android305.lights.util.Group;
import com.android305.lights.util.loaders.LampAndGroupLoader;

public class GroupActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<SparseArray<Group>>, ActivityAttachService {
    private static final String TAG = "GroupActivity";
    private ClientService mService;
    private boolean mBound = false;
    private LostConnectionTask mLostConnectionTask = null;
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;
    private TextView mNoGroupView;
    private ProgressBar mLoadingGroups;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group);
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        mNoGroupView = (TextView) findViewById(R.id.no_groups);
        mLoadingGroups = (ProgressBar) findViewById(R.id.loading_groups);
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                setTitle(mSectionsPagerAdapter.getPageTitle(position));
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
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
        getMenuInflater().inflate(R.menu.menu_group, menu);
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
                if (mLostConnectionTask == null) {
                    mLostConnectionTask = new LostConnectionTask();
                    mLostConnectionTask.execute();
                }
                break;
            case ClientService.GROUP_NEEDS_REFRESH:
                /*Group g = (Group) data.getSerializable(ClientService.GROUP_EXTRA);
                if (g != null)
                    mSectionsPagerAdapter.updateGroup(g);*/
                //TODO: actually update, commented code doesn't work
                getSupportLoaderManager().restartLoader(0, null, GroupActivity.this);
                break;
            case ClientService.GROUPS_NEEDS_REFRESH:
                getSupportLoaderManager().restartLoader(0, null, GroupActivity.this);
                break;
            default:
                break;
        }
    }

    @Override
    public ClientService getService() {
        return mService;
    }

    class LostConnectionTask extends AsyncTask<Void, Void, Integer> {
        View mConnectionLostView;

        public LostConnectionTask() {
            mConnectionLostView = findViewById(R.id.connection_lost_view);
        }

        protected void onPreExecute() {
            mConnectionLostView.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(final Integer code) {
            Intent i;
            switch (code) {
                case ClientService.ERROR_PASSWORD_INVALID:
                    mLostConnectionTask = null;
                    i = new Intent(GroupActivity.this, LoginActivity.class);
                    i.putExtra(LoginActivity.EXTRA_ASK_SETTINGS, true);
                    i.putExtra(LoginActivity.EXTRA_PASSWORD_INVALID, true);
                    startActivity(i);
                    finish();
                    break;
                case ClientService.ERROR_KEY_INVALID:
                    mLostConnectionTask = null;
                    i = new Intent(GroupActivity.this, LoginActivity.class);
                    i.putExtra(LoginActivity.EXTRA_ASK_SETTINGS, true);
                    i.putExtra(LoginActivity.EXTRA_KEY_INVALID, true);
                    startActivity(i);
                    finish();
                    break;
                case ClientService.ERROR_HOST_INVALID:
                    mLostConnectionTask = new LostConnectionTask();
                    mLostConnectionTask.execute();
                    break;
                case ClientService.ERROR_UNKNOWN:
                    mLostConnectionTask = null;
                    Toast.makeText(getApplicationContext(), "Unknown error. Check server console.", Toast.LENGTH_SHORT).show();
                    break;
                case ClientService.SUCCESS:
                    mLostConnectionTask = null;
                    mConnectionLostView.setVisibility(View.GONE);
                    break;
            }
        }

        @Override
        protected Integer doInBackground(Void... args) {
            return mService.reconnect();
        }

        @Override
        protected void onCancelled() {
            mLostConnectionTask = null;
            mConnectionLostView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onLoadFinished(Loader<SparseArray<Group>> loader, SparseArray<Group> data) {
        if (data != null)
            mSectionsPagerAdapter.setData(data);
        mLoadingGroups.setVisibility(View.GONE);
        if (data != null && data.size() > 0) {
            setTitle(mSectionsPagerAdapter.getPageTitle(0));
            mViewPager.setVisibility(View.VISIBLE);
            mNoGroupView.setVisibility(View.GONE);
        } else {
            mViewPager.setVisibility(View.GONE);
            mNoGroupView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onLoaderReset(Loader<SparseArray<Group>> loader) {
    }

    @Override
    public Loader<SparseArray<Group>> onCreateLoader(int id, Bundle args) {
        mLoadingGroups.setVisibility(View.VISIBLE);
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
            getSupportLoaderManager().initLoader(0, null, GroupActivity.this);
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

}