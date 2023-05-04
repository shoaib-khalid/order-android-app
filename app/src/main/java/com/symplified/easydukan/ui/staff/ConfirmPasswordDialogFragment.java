package com.symplified.easydukan.ui.staff;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.textfield.TextInputLayout;
import com.symplified.easydukan.R;
import com.symplified.easydukan.models.staff.StaffMember;
import com.symplified.easydukan.models.staff.shift.SummaryDetails;

import java.util.List;

public class ConfirmPasswordDialogFragment extends DialogFragment {

    public interface OnConfirmPasswordListener {
        void onPasswordConfirmed(StaffMember selectedStaffMember, List<SummaryDetails> summaryDetails, String password);
    }

    public static final String TAG = "ConfirmPasswordDialogFragment";

    private Button submitButton;
    private TextInputLayout layoutPassword;
    private final OnConfirmPasswordListener listener;
    private final StaffMember selectedStaffMember;
    private final List<SummaryDetails> summaryDetails;

    public ConfirmPasswordDialogFragment(
            StaffMember selectedStaffMember,
            List<SummaryDetails> summaryDetails,
            OnConfirmPasswordListener listener
    ) {
        this.selectedStaffMember = selectedStaffMember;
        this.summaryDetails = summaryDetails;
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
       View view = inflater.inflate(R.layout.dialog_password, container, false);
       view.findViewById(R.id.cancel_button).setOnClickListener(v -> dismiss());
       layoutPassword = view.findViewById(R.id.password_text_input);
        EditText editTextPassword = view.findViewById(R.id.password_edit_text);

       submitButton = view.findViewById(R.id.confirm_button);
       submitButton.setOnClickListener(v -> {
           listener.onPasswordConfirmed(selectedStaffMember, summaryDetails, editTextPassword.getText().toString());
           dismiss();
       });

       editTextPassword.addTextChangedListener(new TextWatcher() {
           @Override
           public void beforeTextChanged(CharSequence s, int start, int count, int after) {

           }

           @Override
           public void onTextChanged(CharSequence s, int start, int before, int count) {

           }

           @Override
           public void afterTextChanged(Editable s) {
               submitButton.setEnabled(s.toString().length() > 0);
               layoutPassword.setError(s.toString().length() > 0 ? null : "Password is required.");
           }
       });

       return view;
    }
}
