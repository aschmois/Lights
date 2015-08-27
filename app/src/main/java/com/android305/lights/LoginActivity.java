package com.android305.lights;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.jasypt.util.text.BasicTextEncryptor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A login screen
 */
public class LoginActivity extends AppCompatActivity {
    public static final boolean DEBUG = true;
    private static final String TAG = "LoginActivity";
    private static final String hostRegex = "(?:(\\d+\\.\\d+\\.\\d+\\.\\d+)(?::(\\d+))?)?(?:([A-z0-9.]+)(?::(\\d+))?)?";
    private static final Pattern CONNECT_STRING_PATTERN = Pattern.compile("([^:|\\n]+)(?::(\\d+))(?:\\|([^\\|\\n]+))(?:\\|([^\\|\\n]+))", Pattern.CASE_INSENSITIVE);
    private static final String PREF_HOST = "prefHost";
    private static final String PREF_SECRET_KEY = "prefSecretKey";
    private static final String PREF_PASSWORD = "prefPassword";
    public static final String EXTRA_ASK_SETTINGS = "extraAskSettings";
    public static final String EXTRA_PASSWORD_INVALID = "extraPasswordInvalid";
    public static final String EXTRA_KEY_INVALID = "extraKeyInvalid";

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    // UI references.
    private EditText mHostView;
    private EditText mSecretKeyView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;

    ClientService mService;
    boolean mBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (getSupportActionBar() == null)
            this.setSupportActionBar(toolbar);
        // Set up the login form.
        mHostView = (EditText) findViewById(R.id.ip_address);
        mSecretKeyView = (EditText) findViewById(R.id.secret_key);

        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin(false);
                    return true;
                }
                return false;
            }
        });

        CheckBox mSecretKeyShow = (CheckBox) findViewById(R.id.secrey_key_show);
        mSecretKeyShow.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mSecretKeyView.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
                } else {
                    mSecretKeyView.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                }
            }
        });

        CheckBox mPasswordShow = (CheckBox) findViewById(R.id.password_show);
        mPasswordShow.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mPasswordView.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
                } else {
                    mPasswordView.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                }
            }
        });

        Button mHostSignInButton = (Button) findViewById(R.id.sign_in_button);
        mHostSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin(false);
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);

        SharedPreferences pref = getSharedPreferences(getPackageName(), CONTEXT_RESTRICTED);
        if (pref.contains(PREF_HOST)) {
            mHostView.setText(pref.getString(PREF_HOST, ""));
            mPasswordView.setText(pref.getString(PREF_PASSWORD, ""));
            mSecretKeyView.setText(pref.getString(PREF_SECRET_KEY, ""));
            if (getIntent() == null || !getIntent().getBooleanExtra(EXTRA_ASK_SETTINGS, false)) {
                attemptLogin(true);
            } else if (getIntent() != null && getIntent().getBooleanExtra(EXTRA_PASSWORD_INVALID, false)) {
                mPasswordView.setError(getString(R.string.error_incorrect_password));
                mPasswordView.requestFocus();
            } else if (getIntent() != null && getIntent().getBooleanExtra(EXTRA_KEY_INVALID, false)) {
                mSecretKeyView.setError(getString(R.string.error_incorrect_secret_key));
                mSecretKeyView.requestFocus();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mAuthTask != null) {
            return super.onOptionsItemSelected(item);
        }
        switch (item.getItemId()) {
            case R.id.action_qr_code:
                IntentIntegrator integrator = new IntentIntegrator(this);
                integrator.initiateScan();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (scanResult != null) {
            BasicTextEncryptor textEncryptor = new BasicTextEncryptor();
            textEncryptor.setPassword("deadpoolisawesome");
            String data = textEncryptor.decrypt(scanResult.getContents());
            Matcher m = CONNECT_STRING_PATTERN.matcher(data);
            if (m.matches()) {
                mHostView.setText(m.group(1) + ":" + m.group(2));
                mPasswordView.setText(m.group(3));
                mSecretKeyView.setText(m.group(4));
            } else {
                Toast.makeText(this, getString(R.string.broken_qr_code), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to LocalService
        Intent intent = new Intent(this, ClientService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    public void attemptLogin(boolean start) {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mHostView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String host = mHostView.getText().toString();
        String sKey = mSecretKeyView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid secret key
        if (TextUtils.isEmpty(sKey)) {
            mSecretKeyView.setError(getString(R.string.error_field_required));
            focusView = mSecretKeyView;
            cancel = true;
        }

        // Check for a valid password
        if (TextUtils.isEmpty(password)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
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
            mAuthTask = new UserLoginTask(host, sKey, password, start);
            mAuthTask.execute((Void) null);
        }
    }

    private boolean isHostValid(String host) {
        return host.matches(hostRegex);
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
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
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Represents an asynchronous login task used to authenticate the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Integer> {

        private final String mHost;
        private final String mSecretKey;
        private final String mPassword;
        private final boolean mStart;

        UserLoginTask(String host, String secretKey, String password, boolean start) {
            mHost = host;
            mSecretKey = secretKey;
            mPassword = password;
            mStart = start;
        }

        @Override
        protected Integer doInBackground(Void... params) {
            while (mService == null)
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                }
            return mService.authenticate(mHost, mSecretKey, mPassword);
        }

        @Override
        protected void onPostExecute(final Integer code) {
            mAuthTask = null;
            showProgress(false);
            switch (code) {
                case ClientService.ERROR_PASSWORD_INVALID:
                    mPasswordView.setError(getString(R.string.error_incorrect_password));
                    mPasswordView.requestFocus();
                    break;
                case ClientService.ERROR_KEY_INVALID:
                    mSecretKeyView.setError(getString(R.string.error_incorrect_secret_key));
                    mSecretKeyView.requestFocus();
                    break;
                case ClientService.ERROR_HOST_INVALID:
                    mHostView.setError(getString(R.string.error_cant_reach_host));
                    mHostView.requestFocus();
                    break;
                case ClientService.SUCCESS:
                    if (DEBUG)
                        Log.d(TAG, "Authenticated");
                    SharedPreferences pref = getSharedPreferences(getPackageName(), CONTEXT_RESTRICTED);
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putString(PREF_HOST, mHost);
                    editor.putString(PREF_SECRET_KEY, mSecretKey);
                    editor.putString(PREF_PASSWORD, mPassword);
                    editor.apply();
                    goIntoLampActivity(!mStart);
                    break;
                case ClientService.ERROR_UNKNOWN:
                    Toast.makeText(getApplicationContext(), "Unknown error. Check server console.", Toast.LENGTH_SHORT).show();
                    break;
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            ClientService.LocalBinder binder = (ClientService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            if (mService.isConnected()) {
                goIntoLampActivity(false);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    private void goIntoLampActivity(boolean animation) {
        Intent i = new Intent(LoginActivity.this, LampActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        if (!animation)
            i.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(i);
        finish();
        if (!animation)
            overridePendingTransition(0, 0);
    }
}