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

public class DeleteLampConfirmationDialog extends DialogFragment {
    public final static String TAG = DeleteLampConfirmationDialog.class.getSimpleName();
    private final static String LAMP_NAME_EXTRA = "lamp";
    private final static String POSITION_EXTRA = "position";

    public interface DeleteLampConfirmationListener {
        void onDeleteLamp(int position);
    }

    public static DeleteLampConfirmationDialog newInstance(@NonNull String lampName, int position) {
        DeleteLampConfirmationDialog dialog = new DeleteLampConfirmationDialog();
        Bundle args = new Bundle();
        args.putString(LAMP_NAME_EXTRA, lampName);
        args.putInt(POSITION_EXTRA, position);
        dialog.setArguments(args);
        return dialog;
    }

    private DeleteLampConfirmationListener mListener;
    private String mLampName;
    private int mPosition;

    @TargetApi(22)
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (DeleteLampConfirmationListener) getTargetFragment();
        } catch (ClassCastException e) {
            throw new RuntimeException("Class " + getTargetFragment().toString() + " must implement DeleteLampConfirmationListener");
        }
    }

    @TargetApi(23)
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (DeleteLampConfirmationListener) getTargetFragment();
        } catch (ClassCastException e) {
            throw new RuntimeException("Context " + getTargetFragment().toString() + " must implement DeleteLampConfirmationListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        mLampName = args.getString(LAMP_NAME_EXTRA);
        mPosition = args.getInt(POSITION_EXTRA);
        assert mLampName != null;
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.confirmation);
        builder.setMessage(String.format(getContext().getString(R.string.delete_confirm), mLampName));
        builder.setNegativeButton(android.R.string.no, null);
        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mListener.onDeleteLamp(mPosition);
            }
        });
        return builder.create();
    }
}
