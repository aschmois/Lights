package com.android305.lights;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import com.android305.lights.service.ClientResponse;
import com.android305.lights.service.ClientService;
import com.android305.lights.service.TimerUtils;
import com.android305.lights.util.Timer;
import com.android305.lights.util.client.Client;
import com.codetroopers.betterpickers.radialtimepicker.RadialTimePickerDialogFragment;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;

import java.sql.Time;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

@EActivity(R.layout.activity_timer_edit)
public class TimerEditActivity extends MyAppCompatActivity {
    private static final String TAG = TimerEditActivity.class.getSimpleName();
    private static final String SUNDAY = "sunday";
    private static final String MONDAY = "monday";
    private static final String TUESDAY = "tuesday";
    private static final String WEDNESDAY = "wednesday";
    private static final String THURSDAY = "thursday";
    private static final String FRIDAY = "friday";
    private static final String SATURDAY = "saturday";

    @Extra
    Timer mTimer = null;

    @Extra
    int mGroupId = -1;

    @Extra
    int mPosition = -1;

    @ViewById(R.id.start)
    EditText mStartView;

    @ViewById(R.id.end)
    EditText mEndView;

    @ViewById(R.id.edit_progress)
    View mProgressView;

    @ViewById(R.id.edit_form)
    View mFormView;

    @ViewById(R.id.sunday)
    CheckBox mSunday;

    @ViewById(R.id.monday)
    CheckBox mMonday;

    @ViewById(R.id.tuesday)
    CheckBox mTuesday;

    @ViewById(R.id.wednesday)
    CheckBox mWednesday;

    @ViewById(R.id.thursday)
    CheckBox mThursday;

    @ViewById(R.id.friday)
    CheckBox mFriday;

    @ViewById(R.id.saturday)
    CheckBox mSaturday;

    @ViewById(R.id.everyday)
    RadioButton mEveryday;

    @ViewById(R.id.weekend)
    RadioButton mWeekend;

    @ViewById(R.id.weekdays)
    RadioButton mWeekdays;

    @ViewById(R.id.custom)
    RadioButton mCustom;

    private boolean editing = false;
    private boolean dontUpdateCheckboxes = false;
    private MenuItem submitItem;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_timer_edit, menu);
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
        setTitle(R.string.title_activity_timer_edit);
        mStartView.setKeyListener(null);
        mEndView.setKeyListener(null);
        if (mTimer != null) {
            mStartView.setText(mTimer.getStart().toString());
            mEndView.setText(mTimer.getEnd().toString());

            if (!mTimer.isSunday())
                mSunday.setChecked(false);
            if (!mTimer.isMonday())
                mMonday.setChecked(false);
            if (!mTimer.isTuesday())
                mTuesday.setChecked(false);
            if (!mTimer.isWednesday())
                mWednesday.setChecked(false);
            if (!mTimer.isThursday())
                mThursday.setChecked(false);
            if (!mTimer.isFriday())
                mFriday.setChecked(false);
            if (!mTimer.isSaturday())
                mSaturday.setChecked(false);

            checkDays();
        } else {
            mTimer = new Timer();
            mTimer.setSunday(true);
            mTimer.setMonday(true);
            mTimer.setTuesday(true);
            mTimer.setWednesday(true);
            mTimer.setThursday(true);
            mTimer.setFriday(true);
            mTimer.setSaturday(true);
            mTimer.setInternalGroupId(mGroupId);
        }
        mSunday.setOnCheckedChangeListener(mDateChangedListener);
        mMonday.setOnCheckedChangeListener(mDateChangedListener);
        mTuesday.setOnCheckedChangeListener(mDateChangedListener);
        mWednesday.setOnCheckedChangeListener(mDateChangedListener);
        mThursday.setOnCheckedChangeListener(mDateChangedListener);
        mFriday.setOnCheckedChangeListener(mDateChangedListener);
        mSaturday.setOnCheckedChangeListener(mDateChangedListener);

        mEveryday.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked && !dontUpdateCheckboxes) {
                    mSunday.setChecked(true);
                    mMonday.setChecked(true);
                    mTuesday.setChecked(true);
                    mWednesday.setChecked(true);
                    mThursday.setChecked(true);
                    mFriday.setChecked(true);
                    mSaturday.setChecked(true);
                }
            }
        });

        mWeekend.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked && !dontUpdateCheckboxes) {
                    mSunday.setChecked(true);
                    mMonday.setChecked(false);
                    mTuesday.setChecked(false);
                    mWednesday.setChecked(false);
                    mThursday.setChecked(false);
                    mFriday.setChecked(false);
                    mSaturday.setChecked(true);
                }
            }
        });

        mWeekdays.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked && !dontUpdateCheckboxes) {
                    mSunday.setChecked(false);
                    mMonday.setChecked(true);
                    mTuesday.setChecked(true);
                    mWednesday.setChecked(true);
                    mThursday.setChecked(true);
                    mFriday.setChecked(true);
                    mSaturday.setChecked(false);
                }
            }
        });
    }

    private CompoundButton.OnCheckedChangeListener mDateChangedListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            switch ((String) buttonView.getTag()) {
                case SUNDAY:
                    mTimer.setSunday(isChecked);
                    break;
                case MONDAY:
                    mTimer.setMonday(isChecked);
                    break;
                case TUESDAY:
                    mTimer.setTuesday(isChecked);
                    break;
                case WEDNESDAY:
                    mTimer.setWednesday(isChecked);
                    break;
                case THURSDAY:
                    mTimer.setThursday(isChecked);
                    break;
                case FRIDAY:
                    mTimer.setFriday(isChecked);
                    break;
                case SATURDAY:
                    mTimer.setSaturday(isChecked);
                    break;
            }
            checkDays();
        }
    };

    private void checkDays() {
        dontUpdateCheckboxes = true;
        if (mTimer.isEveryday())
            mEveryday.setChecked(true);
        else if (mTimer.isWeekend())
            mWeekend.setChecked(true);
        else if (mTimer.isWeekdays())
            mWeekdays.setChecked(true);
        else
            mCustom.setChecked(true);
        dontUpdateCheckboxes = false;
    }

    public void attemptSubmit() {
        if (editing) {
            return;
        }
        mStartView.setError(null);
        mEndView.setError(null);
        boolean cancel = false;
        if (mStartView.getText().toString().trim().equals("")) {
            mStartView.setError(getString(R.string.set_time));
            cancel = true;
        }
        if (mEndView.getText().toString().trim().equals("")) {
            mEndView.setError(getString(R.string.set_time));
            cancel = true;
        }
        if (!cancel) {
            showProgress(true);
            editing = true;
            doTimerEdit();
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
    void doTimerEdit() {
        ClientResponse response;
        boolean newTimer = mTimer.getId() == 0;
        if (newTimer)
            response = TimerUtils.addTimer(mService, mTimer);
        else {
            response = TimerUtils.editTimer(mService, mTimer);
        }
        onTimerEdit(response);
    }

    @UiThread
    void onTimerEdit(ClientResponse response) {
        submitItem.setEnabled(true);
        showProgress(false);
        editing = false;
        mTimer = response.getTimer();
        switch (response.getResponse()) {
            case Client.TIMER_ADD_SUCCESS:
            case Client.TIMER_EDIT_SUCCESS:
                addResultsAndFinish();
                break;
            case Client.TIMER_EDIT_GROUP_DOES_NOT_EXIST:
                Log.e(TAG, "timer edit, group does not exist, programming error");
                break;
            default:
                Toast.makeText(getApplicationContext(), R.string.unknown_error_check_server_console, Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void addResultsAndFinish() {
        Intent i = new Intent();
        i.putExtra("timer", mTimer);
        i.putExtra("pos", mPosition);
        setResult(RESULT_OK, i);
        finish();
    }

    @Click
    void start() {
        RadialTimePickerDialogFragment rtpd = new RadialTimePickerDialogFragment().setOnTimeSetListener(new RadialTimePickerDialogFragment.OnTimeSetListener() {
            @Override
            public void onTimeSet(RadialTimePickerDialogFragment dialog, int hourOfDay, int minute) {
                Time time = Time.valueOf(String.format(Locale.US, "%d:%d:00", hourOfDay, minute));
                Date date = new Date(time.getTime());
                DateFormat dateFormat = android.text.format.DateFormat.getTimeFormat(getApplicationContext());
                mTimer.setStart(time);
                mStartView.setText(dateFormat.format(date));
            }
        }).setThemeLight();
        Time start = mTimer.getStart();
        if (start != null) {
            rtpd.setStartTime(start.getHours(), start.getMinutes());
        }
        rtpd.show(getSupportFragmentManager(), "start_time_picker");
    }

    @Click
    void end() {
        RadialTimePickerDialogFragment rtpd = new RadialTimePickerDialogFragment().setOnTimeSetListener(new RadialTimePickerDialogFragment.OnTimeSetListener() {
            @Override
            public void onTimeSet(RadialTimePickerDialogFragment dialog, int hourOfDay, int minute) {
                Time time = Time.valueOf(String.format(Locale.US, "%d:%d:00", hourOfDay, minute));
                Date date = new Date(time.getTime());
                DateFormat dateFormat = android.text.format.DateFormat.getTimeFormat(getApplicationContext());
                mTimer.setEnd(time);
                mEndView.setText(dateFormat.format(date));
            }
        }).setThemeLight();
        Time end = mTimer.getEnd();
        if (end != null) {
            rtpd.setStartTime(end.getHours(), end.getMinutes());
        }
        rtpd.show(getSupportFragmentManager(), "end_time_picker");
    }
}