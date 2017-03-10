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
import com.android305.lights.util.Group;

public class DeleteGroupConfirmationDialog extends DialogFragment {
    public final static String TAG = DeleteGroupConfirmationDialog.class.getSimpleName();
    private final static String GROUP_EXTRA = "group";

    public interface DeleteGroupConfirmationListener {
        void onDeleteGroup(Group group);
    }

    public static DeleteGroupConfirmationDialog newInstance(@NonNull Group group) {
        DeleteGroupConfirmationDialog dialog = new DeleteGroupConfirmationDialog();
        Bundle args = new Bundle();
        args.putSerializable(GROUP_EXTRA, group);
        dialog.setArguments(args);
        return dialog;
    }

    private DeleteGroupConfirmationListener mListener;
    private Group mGroup;

    @SuppressWarnings("deprecation")
    @TargetApi(22)
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (DeleteGroupConfirmationListener) activity;
        } catch (ClassCastException e) {
            throw new RuntimeException("Class " + getTargetFragment().toString() + " must implement DeleteGroupConfirmationListener");
        }
    }

    @TargetApi(23)
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (DeleteGroupConfirmationListener) context;
        } catch (ClassCastException e) {
            throw new RuntimeException("Context " + getTargetFragment().toString() + " must implement DeleteGroupConfirmationListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        mGroup = (Group) args.getSerializable(GROUP_EXTRA);
        assert mGroup != null;
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.confirmation);
        builder.setMessage(String.format(getContext().getString(R.string.delete_confirm), mGroup.getName()));
        builder.setNegativeButton(android.R.string.no, null);
        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mListener.onDeleteGroup(mGroup);
            }
        });
        return builder.create();
    }
}
