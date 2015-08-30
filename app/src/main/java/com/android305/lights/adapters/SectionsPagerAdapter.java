package com.android305.lights.adapters;

import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.SparseArray;

import com.android305.lights.fragments.GroupFragment;
import com.android305.lights.util.Group;
import com.android305.lights.interfaces.UpdateableFragment;

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

    public void updateGroup(@NonNull Group group) {
        mData.put(group.getId(), group);
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

    @SuppressWarnings("unchecked")
    @Override
    public int getItemPosition(Object object) {
        if (object instanceof UpdateableFragment) {
            UpdateableFragment<Group> fragment = (UpdateableFragment<Group>) object;
            fragment.update(mData.get(fragment.getDataId()));
        }
        //don't return POSITION_NONE, avoid fragment recreation.
        return super.getItemPosition(object);
    }
}