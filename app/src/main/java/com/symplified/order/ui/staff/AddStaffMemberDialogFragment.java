package com.symplified.order.ui.staff;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.textfield.TextInputLayout;
import com.symplified.order.R;
import com.symplified.order.models.staff.StaffMember;
import com.symplified.order.models.store.Store;

import java.util.ArrayList;
import java.util.List;

public class AddStaffMemberDialogFragment extends DialogFragment {

    public static final String TAG = "AddStaffMemberDialogFragment";

    private final OnAddStaffMemberListener listener;
    private final Store[] stores;
    private Button submitButton;
    private final List<EditText> textInputs = new ArrayList<>();
    private Store selectedStore;

    public AddStaffMemberDialogFragment(Store[] stores, OnAddStaffMemberListener listener) {
        super();
        this.listener = listener;
        this.stores = stores;
        if (stores.length > 0) {
            this.selectedStore = stores[0];
        }
    }

    public interface OnAddStaffMemberListener {
        void onStaffMemberAdded(String storeId, String name, String username, String password);
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.dialog_add_staff_member, container, false);

        view.findViewById(R.id.cancel_button).setOnClickListener(v -> dismiss());
        submitButton = view.findViewById(R.id.add_staff_member_button);

        TextView prefixTextView = view.findViewById(R.id.username_prefix_text);
        if (selectedStore != null) {
            prefixTextView.setText(selectedStore.id.substring(selectedStore.id.length() - 4));
        }

        EditText nameEditText = view.findViewById(R.id.name_edit_text);
        EditText usernameEditText = view.findViewById(R.id.username_edit_text);
        EditText passwordEditText = view.findViewById(R.id.password_edit_text);
        textInputs.add(nameEditText);
        textInputs.add(usernameEditText);
        textInputs.add(passwordEditText);

        nameEditText.addTextChangedListener(new DialogTextWatcher("Name", nameEditText));
        usernameEditText.addTextChangedListener(new DialogTextWatcher("Username", usernameEditText));
        passwordEditText.addTextChangedListener(new DialogTextWatcher("Password", passwordEditText));

        StoreAdapter adapter = new StoreAdapter(getContext(), android.R.layout.simple_spinner_dropdown_item, stores);
        Spinner spinner = view.findViewById(R.id.stores_spinner);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedStore = adapter.getItem(position);
                prefixTextView.setText(selectedStore != null ? selectedStore.storePrefix : "");
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        submitButton.setOnClickListener(v -> {
            listener.onStaffMemberAdded(
                    selectedStore.id,
                    nameEditText.getText().toString(),
                    selectedStore.storePrefix + usernameEditText.getText().toString(),
                    passwordEditText.getText().toString()
            );
            dismiss();
        });

        return view;
    }

    void evaluateSubmitButton() {
        int validInputs = 0;
        for (EditText editText : textInputs) {
            if (!editText.getText().toString().isEmpty()) {
                validInputs++;
            }
        }

        if (validInputs == 3) {
            submitButton.setEnabled(true);
        }
    }

    private class DialogTextWatcher implements TextWatcher {

        private final String editTextName;
        private final EditText editText;

        public DialogTextWatcher(String editTextName, EditText editText) {
            this.editTextName = editTextName;
            this.editText = editText;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (s.toString().isEmpty()) {
                editText.setError(editTextName + " is required.");
                submitButton.setEnabled(false);
            }
            evaluateSubmitButton();
        }
    }

    private class StoreAdapter extends ArrayAdapter<Store> {
        private final Store[] stores;

        public StoreAdapter(
                Context context,
                int textViewResourceId,
                Store[] stores
        ) {
            super(context, textViewResourceId, stores);
            this.stores = stores;
        }

        @Override
        public int getCount() {
            return stores.length;
        }

        @Nullable
        @Override
        public Store getItem(int position) {
            return stores[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            TextView label = (TextView) super.getView(position, convertView, parent);
            label.setText(stores[position].name);
            return label;
        }

        @Override
        public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            TextView label = (TextView) super.getDropDownView(position, convertView, parent);
            label.setText(stores[position].name);
            return label;
        }
    }
}
