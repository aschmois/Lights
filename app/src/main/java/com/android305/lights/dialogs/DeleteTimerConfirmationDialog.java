package com.android305.lights.dialogs;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

import com.android305.lights.R;

public class DeleteTimerConfirmationDialog extends DialogFragment {
    public final static String TAG = DeleteTimerConfirmationDialog.class.getSimpleName();
    private final static String TIMER_NAME_EXTRA = "timer";
    private final static String POSITION_EXTRA = "position";

    public interface DeleteTimerConfirmationListener {
        void onDeleteTimer(int position);
    }

    public static DeleteTimerConfirmationDialog newInstance(@NonNull String timerName, int position) {
        DeleteTimerConfirmationDialog dialog = new DeleteTimerConfirmationDialog();
        Bundle args = new Bundle();
        args.putString(TIMER_NAME_EXTRA, timerName);
        args.putInt(POSITION_EXTRA, position);
        dialog.setArguments(args);
        return dialog;
    }

    private DeleteTimerConfirmationListener mListener;
    private String mTimerName;
    private int mPosition;

    @TargetApi(22)
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (DeleteTimerConfirmationListener) getTargetFragment();
        } catch (ClassCastException e) {
            throw new RuntimeException("Class " + getTargetFragment().toString() + " must implement DeleteTimerConfirmationListener");
        }
    }

    @TargetApi(23)
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (DeleteTimerConfirmationListener) getTargetFragment();
        } catch (ClassCastException e) {
            throw new RuntimeException("Context " + getTargetFragment().toString() + " must implement DeleteTimerConfirmationListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        mTimerName = args.getString(TIMER_NAME_EXTRA);
        mPosition = args.getInt(POSITION_EXTRA);
        assert mTimerName != null;
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.confirmation);
        builder.setMessage(String.format(getContext().getString(R.string.delete_confirm), mTimerName));
        builder.setNegativeButton(android.R.string.no, null);
        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mListener.onDeleteTimer(mPosition);
            }
        });
        return builder.create();
    }
}
