package com.android305.lights.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android305.lights.R;
import com.android305.lights.adapters.LampAdapter;
import com.android305.lights.adapters.TimerAdapter;
import com.android305.lights.interfaces.ActivityAttachService;
import com.android305.lights.interfaces.UpdateableFragment;
import com.android305.lights.util.Group;

public class GroupFragment extends Fragment implements UpdateableFragment<Group> {
    private static final String ARG_GROUP = "group";
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
    private TextView mTitle;
    private TextView mEmptyLamps;
    private RecyclerView mLampList;

    private TextView mEmptyTimers;
    private RecyclerView mTimerList;

    public GroupFragment() {
    }

    @Override
    public void onAttach(Activity activity) {
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
        Group group = (Group) args.getSerializable(ARG_GROUP);
        if (group == null)
            throw new RuntimeException("Group was lost somewhere in the memory");
        mGroupId = group.getId();
        mRootView = inflater.inflate(R.layout.fragment_lamp, container, false);
        mTitle = find(R.id.group_title);

        mLampList = find(R.id.lamp_list);
        mLampList.setLayoutManager(new LinearLayoutManager(getActivity()));
        mEmptyLamps = find(R.id.no_lamps);

        mTimerList = find(R.id.timer_list);
        mTimerList.setLayoutManager(new LinearLayoutManager(getActivity()));
        mEmptyTimers = find(R.id.no_timers);
        update(group);
        return mRootView;
    }

    @SuppressWarnings("unchecked")
    public <E> E find(int id) {
        return (E) mRootView.findViewById(id);
    }

    @Override
    public void update(Group group) {
        mTitle.setText(group.getName());
        if (group.getLamps() != null) {
            LampAdapter adapter = new LampAdapter(mListener.getService(), group.getLamps());
            mLampList.swapAdapter(adapter, false);
            mLampList.setVisibility(View.VISIBLE);
            mEmptyLamps.setVisibility(View.GONE);
        } else {
            mLampList.setAdapter(null);
            mLampList.setVisibility(View.GONE);
            mEmptyLamps.setVisibility(View.VISIBLE);
        }
        if (group.getTimers() != null) {
            TimerAdapter adapter = new TimerAdapter(group.getTimers());
            mTimerList.swapAdapter(adapter, false);
            mTimerList.setVisibility(View.VISIBLE);
            mEmptyTimers.setVisibility(View.GONE);
        } else {
            mTimerList.setAdapter(null);
            mTimerList.setVisibility(View.GONE);
            mEmptyTimers.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getDataId() {
        return mGroupId;
    }
}