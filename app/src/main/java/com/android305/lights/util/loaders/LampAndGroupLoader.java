package com.android305.lights.util.loaders;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.util.SparseArray;

import com.android305.lights.service.ClientService;
import com.android305.lights.service.GroupUtils;
import com.android305.lights.util.Group;

public class LampAndGroupLoader extends AsyncTaskLoader<SparseArray<Group>> {

    ClientService mService;

    public LampAndGroupLoader(Context context, ClientService service) {
        super(context);
        mService = service;
    }

    @Override
    protected void onStartLoading() {
        forceLoad();
    }

    @Override
    public SparseArray<Group> loadInBackground() {
        return GroupUtils.getGroups(mService);
    }
}