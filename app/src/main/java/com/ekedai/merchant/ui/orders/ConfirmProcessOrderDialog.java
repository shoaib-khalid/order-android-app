package com.ekedai.merchant.ui.orders;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.ekedai.merchant.R;
import com.ekedai.merchant.models.qrorders.ConsolidatedOrder;
import com.ekedai.merchant.utils.Utilities;
import com.google.android.material.textfield.TextInputLayout;

import java.text.DecimalFormat;

public class ConfirmProcessOrderDialog extends DialogFragment {

    public static final String TAG = "ConfirmProcessOrderDialog";
    public interface OnConfirmProcessOrderListener {
        void onProcessConfirmed(ConsolidatedOrder order);
    }

    private final String currency;
    private final ConsolidatedOrder order;
    private final OnConfirmProcessOrderListener listener;
    private final DecimalFormat formatter;
    private TextView changeDueText;
    private Button confirmButton;
    private Double changeDue = 0.00, amountPaid = 0.0;

    public ConfirmProcessOrderDialog(
            String currency,
            ConsolidatedOrder order,
            OnConfirmProcessOrderListener listener
    ) {
        this.currency = currency;
        this.order = order;
        this.listener = listener;
        this.formatter = Utilities.getMonetaryAmountFormat();
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.dialog_process_consolidated_order, container, false);
        if (getDialog() != null) {
            getDialog().setCancelable(false);
        }
        view.findViewById(R.id.cancel_button).setOnClickListener(v -> dismiss());

        TextView totalSalesAmountText = view.findViewById(R.id.total_sales_amount);
        totalSalesAmountText.setText(getString(R.string.monetary_amount, currency, formatter.format(order.totalOrderAmount)));

        changeDueText = view.findViewById(R.id.change_due_text);
        changeDueText.setText(getString(R.string.monetary_amount, currency, formatter.format(0.0)));

        TextInputLayout layoutAmountPaid = view.findViewById(R.id.amount_text_input);
        EditText editTextAmountPaid = view.findViewById(R.id.amount_edit_text);
        confirmButton = view.findViewById(R.id.confirm_button);

        editTextAmountPaid.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                amountPaid = Double.parseDouble(!Utilities.isBlank(s.toString()) ? s.toString() : "0.00");
                changeDue = amountPaid - order.totalOrderAmount;
                changeDueText.setText(getString(R.string.monetary_amount, currency, changeDue >= 0 ? formatter.format(changeDue) : "0.00"));
                confirmButton.setEnabled(changeDue >= 0);
            }
        });

        confirmButton.setOnClickListener(v -> {
            order.localCashPaymentAmount = amountPaid;
            order.changeDue = changeDue;
            listener.onProcessConfirmed(order);
            dismiss();
        });

        return view;
    }
}
