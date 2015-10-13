package com.android305.lights;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android305.lights.service.ClientResponse;
import com.android305.lights.service.ClientService;
import com.android305.lights.service.LampUtils;
import com.android305.lights.util.Lamp;
import com.android305.lights.util.client.Client;

/**
 * A login screen
 */
public class LampEditActivity extends MyAppCompatActivity {
    private static final String TAG = "LampEditActivity";
    private static final String hostRegex = "(?:(\\d+\\.\\d+\\.\\d+\\.\\d+)(?::(\\d+))?)?(?:([A-z0-9.]+)(?::(\\d+))?)?";
    public static final String EXTRA_LAMP = "extraLamp";
    public static final String EXTRA_GROUP_ID = "extraGroupId";
    public static final String EXTRA_POSITION = "pos";

    private LampEditTask mEditTask = null;

    // UI references.
    private TextView mNameTextView;
    private TextView mHostTextView;
    private EditText mNameView;
    private EditText mHostView;
    private View mProgressView;
    private View mFormView;
    private CheckBox mInvertRelay;

    private Lamp mLamp;
    private int mGroupId;
    private int mPosition = -1;

    @Override
    public void onServiceBind(ClientService mService) {
        setContentView(R.layout.activity_lamp_edit);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (getSupportActionBar() == null)
            this.setSupportActionBar(toolbar);
        mNameTextView = (TextView) findViewById(R.id.name_txt);
        mHostTextView = (TextView) findViewById(R.id.ip_address_txt);
        mNameView = (EditText) findViewById(R.id.name);
        mHostView = (EditText) findViewById(R.id.ip_address);

        mInvertRelay = (CheckBox) findViewById(R.id.lamp_invert);

        Button mSubmitButton = (Button) findViewById(R.id.submit);
        mSubmitButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptSubmit(false);
            }
        });

        mFormView = findViewById(R.id.edit_form);
        mProgressView = findViewById(R.id.edit_progress);

        if (getIntent() != null) {
            if (getIntent().hasExtra(EXTRA_LAMP)) {
                mLamp = (Lamp) getIntent().getSerializableExtra(EXTRA_LAMP);
                mGroupId = mLamp.getInternalGroupId();
                mPosition = getIntent().getIntExtra(EXTRA_POSITION, -1);
                mNameTextView.setVisibility(View.VISIBLE);
                mHostTextView.setVisibility(View.VISIBLE);
                mNameView.setText(mLamp.getName());
                mHostView.setText(mLamp.getIpAddress());
                mInvertRelay.setChecked(mLamp.isInvert());
            } else if (getIntent().hasExtra(EXTRA_GROUP_ID)) {
                mGroupId = getIntent().getIntExtra(EXTRA_GROUP_ID, 0);
            }
        }
    }

    public void attemptSubmit(boolean start) {
        if (mEditTask != null) {
            return;
        }

        // Reset errors.
        mNameView.setError(null);
        mHostView.setError(null);

        // Store values at the time of the login attempt.
        String name = mNameView.getText().toString();
        String host = mHostView.getText().toString();
        boolean invert = mInvertRelay.isChecked();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid secret key
        if (TextUtils.isEmpty(name)) {
            mNameView.setError(getString(R.string.error_field_required));
            focusView = mNameView;
            cancel = true;
        }

        // Check for a valid host address.
        if (TextUtils.isEmpty(host)) {
            mHostView.setError(getString(R.string.error_field_required));
            focusView = mHostView;
            cancel = true;
        } else if (!isHostValid(host)) {
            mHostView.setError(getString(R.string.error_invalid_host));
            focusView = mHostView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mEditTask = new LampEditTask(name, host, invert);
            mEditTask.execute((Void) null);
        }
    }

    private boolean isHostValid(String host) {
        return host.matches(hostRegex);
    }

    /**
     * Shows the progress UI and hides the form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mFormView.animate().setDuration(shortAnimTime).alpha(show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Represents an asynchronous task used to submit the form.
     */
    public class LampEditTask extends AsyncTask<Void, Void, ClientResponse> {
        private final String mName;
        private final String mHost;
        private final boolean mInvert;

        LampEditTask(String name, String host, boolean invert) {
            mName = name;
            mHost = host;
            mInvert = invert;
        }

        @Override
        protected ClientResponse doInBackground(Void... params) {
            boolean newLamp = mLamp == null;
            Lamp lamp = mLamp;
            if (newLamp) {
                lamp = new Lamp();
            }
            lamp.setName(mName);
            lamp.setIpAddress(mHost);
            lamp.setInvert(mInvert);
            lamp.setInternalGroupId(mGroupId);
            if (newLamp)
                return LampUtils.addLamp(mService, lamp);
            else {
                return LampUtils.editLamp(mService, lamp);
            }
        }

        @Override
        protected void onPostExecute(final ClientResponse response) {
            mEditTask = null;
            showProgress(false);
            mLamp = response.getLamp();
            switch (response.getResponse()) {
                case Client.LAMP_ADD_SUCCESS:
                case Client.LAMP_EDIT_SUCCESS:
                    goIntoLampActivity();
                    break;
                case Client.LAMP_ALREADY_EXISTS:
                    mNameView.setError(getString(R.string.error_lamp_already_exists));
                    break;
                case Client.LAMP_EDIT_GROUP_DOES_NOT_EXIST:
                    Log.e(TAG, "lamp edit, group does not exist, programming error");
                    break;
                default:
                    Toast.makeText(getApplicationContext(), R.string.unknown_error_check_server_console, Toast.LENGTH_SHORT).show();
                    break;
            }
        }

        @Override
        protected void onCancelled() {
            mEditTask = null;
            showProgress(false);
        }
    }

    private void goIntoLampActivity() {
        Intent i = new Intent();
        i.putExtra(EXTRA_LAMP, mLamp);
        i.putExtra(EXTRA_POSITION, mPosition);
        setResult(RESULT_OK, i);
        finish();
    }
}