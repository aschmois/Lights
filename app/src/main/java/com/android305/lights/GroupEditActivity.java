package com.android305.lights;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android305.lights.service.ClientResponse;
import com.android305.lights.service.ClientService;
import com.android305.lights.service.GroupUtils;
import com.android305.lights.util.Group;
import com.android305.lights.util.client.Client;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;

@EActivity(R.layout.activity_group_edit)
public class GroupEditActivity extends MyAppCompatActivity {
    private static final String TAG = GroupEditActivity.class.getSimpleName();

    @ViewById(R.id.name_txt)
    TextView mNameTextView;

    @ViewById(R.id.name)
    EditText mNameView;

    @ViewById(R.id.edit_progress)
    View mProgressView;

    @ViewById(R.id.edit_form)
    View mFormView;

    @Extra
    Group mGroup;

    private boolean editing = false;
    MenuItem submitItem;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_group_edit, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_submit:
                submitItem = item;
                attemptSubmit();
                item.setEnabled(false);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onServiceBind(ClientService mService) {
        if (mGroup != null) {
            mNameTextView.setVisibility(View.VISIBLE);
            mNameView.setText(mGroup.getName());
        }
    }

    public void attemptSubmit() {
        if (editing) {
            return;
        }

        // Reset errors.
        mNameView.setError(null);

        // Store values at the time of the login attempt.
        String name = mNameView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a name
        if (TextUtils.isEmpty(name)) {
            mNameView.setError(getString(R.string.error_field_required));
            focusView = mNameView;
            cancel = true;
        }

        if (cancel) {
            focusView.requestFocus();
        } else {
            showProgress(true);
            editing = true;
            doGroupEdit(name);
        }
    }

    /**
     * Shows the progress UI and hides the form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public void showProgress(final boolean show) {
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
    }

    @Background
    void doGroupEdit(String name) {
        ClientResponse response;
        boolean newGroup = mGroup == null;
        Group group = mGroup;
        if (newGroup) {
            group = new Group();
        }
        group.setName(name);
        if (newGroup)
            response = GroupUtils.addGroup(mService, group);
        else {
            response = GroupUtils.editGroup(mService, group);
        }
        onGroupEdit(response);
    }

    @UiThread
    void onGroupEdit(ClientResponse response) {
        submitItem.setEnabled(true);
        showProgress(false);
        editing = false;
        mGroup = response.getGroup();
        switch (response.getResponse()) {
            case Client.GROUP_ADD_SUCCESS:
            case Client.GROUP_EDIT_SUCCESS:
                goIntoLampActivity();
                break;
            case Client.GROUP_ALREADY_EXISTS:
                mNameView.setError(getString(R.string.error_group_already_exists));
                break;
            default:
                Toast.makeText(getApplicationContext(), R.string.unknown_error_check_server_console, Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void goIntoLampActivity() {
        Intent i = new Intent();
        i.putExtra("group", mGroup);
        setResult(RESULT_OK, i);
        finish();
    }
}