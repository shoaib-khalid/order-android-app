package com.ekedai.merchant.ui.voucher;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.ekedai.merchant.R;

public class VoucherSuccessDialog extends DialogFragment {

    public static final String TAG = "VoucherSuccessDialog";

    public interface OnDialogDismissListener {
        void onDialogDismissed();
    }

    final OnDialogDismissListener dismissListener;

    public VoucherSuccessDialog(OnDialogDismissListener dismissListener) {
        this.dismissListener = dismissListener;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.dialog_success, container, false);

        ((Button) view.findViewById(R.id.close_button)).setOnClickListener(v -> dismiss());

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        dismissListener.onDialogDismissed();
    }
}
