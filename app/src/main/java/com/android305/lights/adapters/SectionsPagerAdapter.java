package com.android305.lights.adapters;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.SparseArray;

import com.android305.lights.fragments.GroupFragment;
import com.android305.lights.interfaces.UpdatableFragment;
import com.android305.lights.util.Group;

public class SectionsPagerAdapter extends FragmentStatePagerAdapter {
    private SparseArray<Group> mData;

    public SectionsPagerAdapter(FragmentManager fm) {
        super(fm);
        mData = new SparseArray<>();
    }

    public void setData(@Nullable SparseArray<Group> data) {
        if (data != null)
            mData = data;
        else
            mData = new SparseArray<>();
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

    public Group getGroupAt(int position) {
        return mData.valueAt(position);
    }

    public int getPosition(@NonNull Group group) {
        return mData.indexOfKey(group.getId());
    }

    @SuppressWarnings("unchecked")
    @Override
    public int getItemPosition(Object object) {
        if (object instanceof UpdatableFragment) {
            UpdatableFragment<Group> fragment = (UpdatableFragment<Group>) object;
            Group g = mData.get(fragment.getDataId());
            if (g != null)
                fragment.update(g);
            else
                return POSITION_NONE;
        }
        //don't return POSITION_NONE, avoid fragment recreation.
        return super.getItemPosition(object);
    }
}