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
    private List<EditText> textInputs = new ArrayList<>();

    public AddStaffMemberDialogFragment(Store[] stores, OnAddStaffMemberListener listener) {
        super();
        this.listener = listener;
        this.stores = stores;
    }

    public interface OnAddStaffMemberListener {
        void onStaffMemberAdded(String name, String username, String password);
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

        StoreAdapter adapter = new StoreAdapter(getContext(), android.R.layout.simple_spinner_item, stores);
        Spinner spinner = view.findViewById(R.id.stores_spinner);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Store store = adapter.getItem(position);
                Toast.makeText(getContext(), "Selected " + store.name, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        EditText nameEditText = view.findViewById(R.id.name_edit_text);
        EditText usernameEditText = view.findViewById(R.id.username_edit_text);
        EditText passwordEditText = view.findViewById(R.id.password_edit_text);
        textInputs.add(nameEditText);
        textInputs.add(usernameEditText);
        textInputs.add(passwordEditText);

        nameEditText.addTextChangedListener(new DialogTextWatcher("Name", nameEditText));
        usernameEditText.addTextChangedListener(new DialogTextWatcher("Username", usernameEditText));
        passwordEditText.addTextChangedListener(new DialogTextWatcher("Password", passwordEditText));

        submitButton.setOnClickListener(v -> {
            listener.onStaffMemberAdded(
                    nameEditText.getText().toString(),
                    usernameEditText.getText().toString(),
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

        if (validInputs == 3)
            submitButton.setEnabled(true);
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
        private Context context;
        private Store[] stores;

        public StoreAdapter(
                Context context,
                int textViewResourceId,
                Store[] stores
        ) {
            super(context, textViewResourceId, stores);
            this.context = context;
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
            label.setTextColor(Color.BLACK);
            label.setText(stores[position].name);
            return label;
        }

        @Override
        public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            TextView label = (TextView) super.getDropDownView(position, convertView, parent);
            label.setTextColor(Color.BLACK);
            label.setText(stores[position].name);
            return label;
        }
    }
}
