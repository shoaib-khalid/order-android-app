package com.symplified.order.ui.orders;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.textfield.TextInputLayout;
import com.symplified.order.R;

public class ConfirmProcessOrderDialog extends DialogFragment {

    public static final String TAG = "ConfirmProcessOrderDialog";
    public interface OnConfirmProcessOrderListener {
        void onProcessConfirmed();
    }

    private final OnConfirmProcessOrderListener listener;
    private final String paymentType;

    public ConfirmProcessOrderDialog(
            String paymentType,
            OnConfirmProcessOrderListener listener
    ) {
        this.paymentType = paymentType;
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.dialog_process_consolidated_order, container, false);
        ((TextView) view.findViewById(R.id.heading))
                .setText(getString(R.string.payment_type_heading, paymentType));
        view.findViewById(R.id.cancel_button).setOnClickListener(v -> dismiss());
        view.findViewById(R.id.confirm_button).setOnClickListener(v -> {
            listener.onProcessConfirmed();
            dismiss();
        });

        return view;
    }
}
