package com.android305.lights.fragments;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android305.lights.LampEditActivity;
import com.android305.lights.R;
import com.android305.lights.adapters.LampAdapter;
import com.android305.lights.adapters.TimerAdapter;
import com.android305.lights.dialogs.DeleteConfirmationDialog;
import com.android305.lights.interfaces.ActivityAttachService;
import com.android305.lights.interfaces.UpdateableFragment;
import com.android305.lights.util.Group;
import com.android305.lights.util.Lamp;

public class GroupFragment extends Fragment implements UpdateableFragment<Group>, DeleteConfirmationDialog.DeleteConfirmationListener {
    private static final String ARG_GROUP = "group";
    public static final int LAMP_ADD = 1000;
    public static final int LAMP_UPDATE = 1001;

    private ActivityAttachService mListener;

    public static GroupFragment newInstance(@NonNull Group group) {
        GroupFragment fragment = new GroupFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_GROUP, group);
        fragment.setArguments(args);
        return fragment;
    }

    private int mGroupId;
    private View mRootView;
    private TextView mEmptyLamps;
    private RecyclerView mLampList;

    private TextView mEmptyTimers;
    private RecyclerView mTimerList;

    private Toolbar mLampToolbar;
    private Toolbar mTimerToolbar;

    private LampAdapter mLampAdapter;
    private Group mGroup;

    public GroupFragment() {
    }

    @SuppressWarnings("deprecation")
    @Override
    @TargetApi(22)
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (ActivityAttachService) activity;
        } catch (ClassCastException e) {
            throw new RuntimeException(activity.toString() + " must implement ActivityAttachService");
        }
    }

    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);
        try {
            mListener = (ActivityAttachService) activity;
        } catch (ClassCastException e) {
            throw new RuntimeException(activity.toString() + " must implement ActivityAttachService");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Bundle args;
        if (savedInstanceState != null) {
            args = savedInstanceState;
        } else {
            args = getArguments();
        }
        mGroup = (Group) args.getSerializable(ARG_GROUP);
        if (mGroup == null)
            throw new RuntimeException("Group was lost somewhere in the memory");
        mGroupId = mGroup.getId();
        mRootView = inflater.inflate(R.layout.fragment_group, container, false);

        mLampToolbar = find(R.id.lamp_toolbar);
        mLampToolbar.setTitle(R.string.lamps);
        mLampToolbar.inflateMenu(R.menu.menu_lamp);
        mTimerToolbar = find(R.id.timer_toolbar);
        mTimerToolbar.setTitle(R.string.timer_entries);
        mTimerToolbar.inflateMenu(R.menu.menu_timer);

        mLampList = find(R.id.lamp_list);
        mLampList.setLayoutManager(new LinearLayoutManager(getActivity()));
        mEmptyLamps = find(R.id.no_lamps);

        mTimerList = find(R.id.timer_list);
        mTimerList.setLayoutManager(new LinearLayoutManager(getActivity()));
        mEmptyTimers = find(R.id.no_timers);
        update(mGroup);
        return mRootView;
    }

    @SuppressWarnings("unchecked")
    public <E> E find(int id) {
        return (E) mRootView.findViewById(id);
    }

    @Override
    public void update(Group group) {
        mGroup = group;
        Lamp[] lamps = group.getLamps();
        if (lamps == null || lamps.length == 0) {
            mLampList.setVisibility(View.GONE); //TODO: FIXME
            mEmptyLamps.setVisibility(View.VISIBLE);
        } else {
            mLampList.setVisibility(View.VISIBLE);
            mEmptyLamps.setVisibility(View.GONE);
        }
        if (mLampAdapter == null) {
            mLampAdapter = new LampAdapter(mListener.getService(), group.getLamps(), this);
            mLampList.setAdapter(mLampAdapter);
            mLampList.setVisibility(View.VISIBLE);
            mEmptyLamps.setVisibility(View.GONE);
        } else {
            mLampAdapter.setData(group.getLamps());
        }
        if (group.getTimers() != null) {
            TimerAdapter adapter = new TimerAdapter(mListener.getService(), group.getTimers(), this);
            mTimerList.swapAdapter(adapter, false);
            mTimerList.setVisibility(View.VISIBLE);
            mEmptyTimers.setVisibility(View.GONE);
        } else {
            mTimerList.setAdapter(null);
            mTimerList.setVisibility(View.GONE);
            mEmptyTimers.setVisibility(View.VISIBLE);
        }
        mLampToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.action_toggle:
                        mLampAdapter.toggleLamps();
                        return true;
                    case R.id.action_add:
                        Intent i = new Intent(getContext(), LampEditActivity.class);
                        i.putExtra(LampEditActivity.EXTRA_GROUP_ID, mGroupId);
                        startActivityForResult(i, LAMP_ADD);
                        return true;
                    default:
                        return false;
                }
            }
        });
        mTimerToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.action_add:
                        //TODO: add timer
                        return true;
                    default:
                        return false;
                }
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case LAMP_ADD:
                if (resultCode == Activity.RESULT_OK) {
                    Lamp lamp = (Lamp) data.getSerializableExtra(LampEditActivity.EXTRA_LAMP);
                    mLampAdapter.add(lamp);
                }
                break;
            case LAMP_UPDATE:
                if (resultCode == Activity.RESULT_OK) {
                    Lamp lamp = (Lamp) data.getSerializableExtra(LampEditActivity.EXTRA_LAMP);
                    int pos = data.getIntExtra(LampEditActivity.EXTRA_POSITION, -1);
                    mLampAdapter.update(lamp, pos);
                }
                break;
        }
        updateLampList();
    }

    @UiThread
    @Override
    public void onDeleteLamp(int position) {
        mLampAdapter.delete(position);
        updateLampList();
    }

    private void updateLampList() {
        if (mLampAdapter.getItemCount() == 0) {
            mLampList.setVisibility(View.GONE);
            mEmptyLamps.setVisibility(View.VISIBLE);
        } else {
            mLampList.setVisibility(View.VISIBLE);
            mEmptyLamps.setVisibility(View.GONE);
        }
    }

    @Override
    public int getDataId() {
        return mGroupId;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable(ARG_GROUP, mGroup);
    }
}