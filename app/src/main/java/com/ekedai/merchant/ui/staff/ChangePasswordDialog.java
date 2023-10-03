package com.ekedai.merchant.ui.staff;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.ekedai.merchant.R;
import com.ekedai.merchant.models.staff.StaffMember;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

public class ChangePasswordDialog extends DialogFragment {

    public interface OnChangePasswordSubmitListener {
        void onChangePasswordSubmitted(StaffMember staffMember, String newPassword);
    }

    public static final String TAG = "ChangePasswordDialog";

    private final OnChangePasswordSubmitListener listener;
    private final StaffMember staffMember;
    private Button submitButton;
    private TextInputLayout layoutNewPassword, layoutConfirmPassword;
    private EditText editTextNewPassword, editTextConfirmPassword;
    private List<EditText> textInputs = new ArrayList<>();

    public ChangePasswordDialog(
            StaffMember staffMember,
            OnChangePasswordSubmitListener listener
    ) {
        this.staffMember = staffMember;
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.dialog_change_password, container, false);

        view.findViewById(R.id.cancel_button).setOnClickListener(v -> dismiss());

        layoutNewPassword = view.findViewById(R.id.new_password_text_input);
        layoutConfirmPassword = view.findViewById(R.id.confirm_password_text_input);

        editTextNewPassword = view.findViewById(R.id.new_password_edit_text);
        editTextConfirmPassword = view.findViewById(R.id.confirm_password_edit_text);

        editTextNewPassword.addTextChangedListener(new DialogTextWatcher());
        editTextConfirmPassword.addTextChangedListener(new DialogTextWatcher());

        submitButton = view.findViewById(R.id.confirm_change_password_button);
        submitButton.setOnClickListener(v -> {
            listener.onChangePasswordSubmitted(staffMember, editTextNewPassword.getText().toString());
            dismiss();
        });

        return view;
    }

    private void validate() {
        boolean doPasswordsMatch
                =
                !editTextNewPassword.getText().toString().isEmpty()
                        && !editTextConfirmPassword.getText().toString().isEmpty()
                        && editTextNewPassword.getText().toString().equals(
                                editTextConfirmPassword.getText().toString());
        submitButton.setEnabled(doPasswordsMatch);
        layoutConfirmPassword.setErrorEnabled(doPasswordsMatch);
        layoutNewPassword.setErrorEnabled(doPasswordsMatch);
        if (!doPasswordsMatch) {
            Log.d("change-password-dialog", "getError: " + layoutNewPassword.getError());
//            editTextNewPassword.setError("Passwords do not match");
            if (layoutNewPassword.getError() == null)
                layoutNewPassword.setError("Passwords do not match");
            if (layoutConfirmPassword.getError() == null)
                layoutConfirmPassword.setError("Passwords do not match");
        } else {
            Log.d("change-password-dialog", "Setting null");
            layoutNewPassword.setError(null);
            layoutConfirmPassword.setError(null);
        }
    }

    private class DialogTextWatcher implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            validate();
        }
    }
}
