package com.android305.lights;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android305.lights.adapters.SectionsPagerAdapter;
import com.android305.lights.dialogs.DeleteGroupConfirmationDialog;
import com.android305.lights.interfaces.ActivityAttachService;
import com.android305.lights.service.ClientService;
import com.android305.lights.service.GroupUtils;
import com.android305.lights.util.Group;
import com.android305.lights.util.loaders.LampAndGroupLoader;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OnActivityResult;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.apache.mina.util.Base64;
import org.jasypt.util.text.BasicTextEncryptor;

@EActivity(R.layout.activity_group)
public class GroupActivity extends MyAppCompatActivity implements LoaderManager.LoaderCallbacks<SparseArray<Group>>, ActivityAttachService, DeleteGroupConfirmationDialog.DeleteGroupConfirmationListener {
    private static final String TAG = "GroupActivity";
    private SectionsPagerAdapter mSectionsPagerAdapter;

    public static final int GROUP_CHANGE = 1000;

    private boolean lostConnection = false;

    @ViewById(R.id.pager)
    ViewPager mViewPager;

    @ViewById(R.id.no_groups)
    TextView mNoGroupView;

    @ViewById(R.id.loading_groups)
    ProgressBar mLoadingGroups;

    @ViewById(R.id.connection_lost_view)
    View mLostConnectionView;

    Group mLoadedGroup;
    int mCurrPosition;

    Group addedOrEditedGroup;

    @Override
    public void onServiceBind(ClientService mService) {
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                setTitle(mSectionsPagerAdapter.getPageTitle(position));
                mLoadedGroup = mSectionsPagerAdapter.getGroupAt(position);
                mCurrPosition = position;
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
        getSupportLoaderManager().initLoader(0, null, GroupActivity.this);
        LocalBroadcastManager.getInstance(this).registerReceiver((mMessageReceiver), new IntentFilter(ClientService.FILTER));
    }

    @Override
    @Background
    public void onServiceReBind(ClientService mService) {
        LocalBroadcastManager.getInstance(this).registerReceiver((mMessageReceiver), new IntentFilter(ClientService.FILTER));
        if (!mService.isConnected()) {
            mService.reconnect();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing()) {
            Intent intent = new Intent(this, ClientService.class);
            stopService(intent);
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
            case R.id.action_add_group:
                GroupEditActivity_.intent(this).startForResult(GROUP_CHANGE);
                return true;
            case R.id.action_edit_group:
                GroupEditActivity_.intent(this).mGroup(mLoadedGroup).startForResult(GROUP_CHANGE);
                return true;
            case R.id.action_delete_group:
                DeleteGroupConfirmationDialog dialog = DeleteGroupConfirmationDialog.newInstance(mLoadedGroup);
                dialog.show(getSupportFragmentManager(), DeleteGroupConfirmationDialog.TAG);
                return true;
            case R.id.action_qr_share:
                SharedPreferences pref = getSharedPreferences(getPackageName(), CONTEXT_RESTRICTED);
                String host = pref.getString(LoginActivity_.PREF_HOST, "");
                String password = pref.getString(LoginActivity_.PREF_PASSWORD, "");
                String sKey = pref.getString(LoginActivity_.PREF_SECRET_KEY, "");
                String qrData = String.format("%s|%s|%s", host, password, sKey);
                BasicTextEncryptor textEncryptor = new BasicTextEncryptor();
                textEncryptor.setPassword("deadpoolisawesome");
                qrData = textEncryptor.encrypt(qrData);
                qrData = new String(Base64.encodeBase64(qrData.getBytes()));
                String qr = "https://chart.googleapis.com/chart?cht=qr&chld=M|4&chs=547x547&chl=" + qrData;
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(qr));
                startActivity(i);
                return true;
            case R.id.action_settings:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @OnActivityResult(GROUP_CHANGE)
    void onResult(@OnActivityResult.Extra Group group) {
        addedOrEditedGroup = group;
        getSupportLoaderManager().restartLoader(0, null, GroupActivity.this);
    }

    @Background
    public void onDeleteGroup(Group group) {
        GroupUtils.deleteGroup(getService(), group);
        onGroupDeleted();
    }

    @UiThread
    void onGroupDeleted() {
        addedOrEditedGroup = null;
        getSupportLoaderManager().restartLoader(0, null, GroupActivity.this);
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
                if (!lostConnection) {
                    lostConnection = true;
                    lostConnection();
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

    @Background
    void lostConnection() {
        showLostConnectionView();
        int code = mService.reconnect();
        onLostConnectionReturn(code);
    }

    @UiThread
    void showLostConnectionView() {
        mLostConnectionView.setVisibility(View.VISIBLE);
    }

    @UiThread
    void onLostConnectionReturn(int code) {
        Intent i;
        switch (code) {
            case ClientService.ERROR_PASSWORD_INVALID:
                lostConnection = false;
                i = new Intent(GroupActivity.this, LoginActivity_.class);
                i.putExtra(LoginActivity.EXTRA_ASK_SETTINGS, true);
                i.putExtra(LoginActivity.EXTRA_PASSWORD_INVALID, true);
                startActivity(i);
                finish();
                break;
            case ClientService.ERROR_KEY_INVALID:
                lostConnection = false;
                i = new Intent(GroupActivity.this, LoginActivity_.class);
                i.putExtra(LoginActivity.EXTRA_ASK_SETTINGS, true);
                i.putExtra(LoginActivity.EXTRA_KEY_INVALID, true);
                startActivity(i);
                finish();
                break;
            case ClientService.ERROR_HOST_INVALID:
                lostConnection();
                break;
            case ClientService.ERROR_UNKNOWN:
                lostConnection = false;
                Toast.makeText(getApplicationContext(), "Unknown error. Check server console.", Toast.LENGTH_SHORT).show();
                break;
            case ClientService.SUCCESS:
                lostConnection = false;
                mLostConnectionView.setVisibility(View.GONE);
                break;
        }
    }

    @Override
    public void onLoadFinished(Loader<SparseArray<Group>> loader, @Nullable SparseArray<Group> data) {
        mSectionsPagerAdapter.setData(data);
        mLoadingGroups.setVisibility(View.GONE);
        if (data != null && data.size() > 0) {
            int pos = addedOrEditedGroup != null ? mSectionsPagerAdapter.getPosition(addedOrEditedGroup) : -1;
            if (pos >= 0) {
                mViewPager.setCurrentItem(pos);
                setTitle(mSectionsPagerAdapter.getPageTitle(pos));
                mLoadedGroup = mSectionsPagerAdapter.getGroupAt(pos);
                mCurrPosition = pos;
            } else {
                mViewPager.setCurrentItem(0);
                setTitle(mSectionsPagerAdapter.getPageTitle(0));
                mLoadedGroup = mSectionsPagerAdapter.getGroupAt(0);
                mCurrPosition = 0;
            }
            mViewPager.setVisibility(View.VISIBLE);
            mNoGroupView.setVisibility(View.GONE);
        } else {
            setTitle(R.string.title_activity_group);
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
}