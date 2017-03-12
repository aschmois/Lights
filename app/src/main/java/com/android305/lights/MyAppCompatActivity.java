package com.android305.lights;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;

import com.android305.lights.service.ClientService;

public abstract class MyAppCompatActivity extends AppCompatActivity {
    private boolean created;
    private boolean mBound;
    protected ClientService mService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent(this, ClientService.class);
        startService(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, ClientService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mService.cancelConnections();
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            ClientService.LocalBinder binder = (ClientService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            if (!created) {
                created = true;
                onServiceBind(mService);
            }
            onServiceReBind(mService);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    /**
     * Similar to onCreate but will wait for service to be bound. Use this method to replace onCreate if the activity relies on the service.
     *
     * @param mService bound service
     */
    protected void onServiceBind(ClientService mService) {
    }

    /**
     * Similar to onStart but will wait for service to be bound. Use this method to replace onStart if the starting of the activity relies on the service.
     *
     * @param mService bound service
     */
    protected void onServiceReBind(ClientService mService) {
    }
}
