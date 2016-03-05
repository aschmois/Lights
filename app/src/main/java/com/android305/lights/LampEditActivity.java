package com.android305.lights;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
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

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;

@EActivity(R.layout.activity_lamp_edit)
public class LampEditActivity extends MyAppCompatActivity {
    private static final String TAG = LampEditActivity.class.getSimpleName();
    private static final String hostRegex = "(?:(\\d+\\.\\d+\\.\\d+\\.\\d+)(?::(\\d+))?)?(?:([A-z0-9.]+)(?::(\\d+))?)?";

    @ViewById(R.id.name_txt)
    TextView mNameTextView;

    @ViewById(R.id.ip_address_txt)
    TextView mHostTextView;

    @ViewById(R.id.name)
    EditText mNameView;

    @ViewById(R.id.ip_address)
    EditText mHostView;

    @ViewById(R.id.edit_progress)
    View mProgressView;

    @ViewById(R.id.edit_form)
    View mFormView;

    @ViewById(R.id.lamp_invert)
    CheckBox mInvertRelay;

    @Extra
    Lamp mLamp;

    @Extra
    int mGroupId = -1;

    @Extra
    int mPosition = -1;

    private boolean editing = false;

    @Override
    public void onServiceBind(ClientService mService) {
        Button mSubmitButton = (Button) findViewById(R.id.submit);
        mSubmitButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptSubmit();
            }
        });

        if (mLamp != null) {
            mGroupId = mLamp.getInternalGroupId();
            mNameTextView.setVisibility(View.VISIBLE);
            mHostTextView.setVisibility(View.VISIBLE);
            mNameView.setText(mLamp.getName());
            mHostView.setText(mLamp.getIpAddress());
            mInvertRelay.setChecked(mLamp.isInvert());
        }
    }

    public void attemptSubmit() {
        if (editing) {
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
            editing = true;
            doLampEdit(name, host, invert);
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

    @Background
    void doLampEdit(String name, String host, boolean invert) {
        ClientResponse response;
        boolean newLamp = mLamp == null;
        Lamp lamp = mLamp;
        if (newLamp) {
            lamp = new Lamp();
        }
        lamp.setName(name);
        lamp.setIpAddress(host);
        lamp.setInvert(invert);
        lamp.setInternalGroupId(mGroupId);
        if (newLamp)
            response = LampUtils.addLamp(mService, lamp);
        else {
            response = LampUtils.editLamp(mService, lamp);
        }
        onLampEdit(response);
    }

    @UiThread
    void onLampEdit(ClientResponse response) {
        showProgress(false);
        editing = false;
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

    private void goIntoLampActivity() {
        Intent i = new Intent();
        i.putExtra("lamp", mLamp);
        i.putExtra("pos", mPosition);
        setResult(RESULT_OK, i);
        finish();
    }
}