package com.ekedai.merchant.ui.voucher;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.ekedai.merchant.R;
import com.ekedai.merchant.models.store.Store;
import com.google.android.material.radiobutton.MaterialRadioButton;

import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the  factory method to
 * create an instance of this fragment.
 */
public class StoreSelectionDialogFragment extends DialogFragment {
    private final List<Store> stores;
    private OnStoreSelectedListener onStoreSelectedListener;

    // Constructor to pass the list of store names
    public StoreSelectionDialogFragment(List<Store> stores) {
        this.stores = stores;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = getLayoutInflater().inflate(R.layout.fragment_store_selection_dialog, null);

        RadioGroup radioGroup = view.findViewById(R.id.storeRadioGroup);
        Button continueButton = view.findViewById(R.id.continueButton);

        for (Store store : stores) {
            MaterialRadioButton materialRadioButton = new MaterialRadioButton(view.getContext());
            materialRadioButton.setId(View.generateViewId());
            materialRadioButton.setText(store.name);
            materialRadioButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            materialRadioButton.setButtonTintList(ColorStateList.valueOf(ContextCompat.getColor(view.getContext(), R.color.sf_primary)));
            radioGroup.addView(materialRadioButton);
        }

        continueButton.setOnClickListener(v -> {
            // Get the selected store from the checked RadioButton
            int checkedRadioButtonId = radioGroup.getCheckedRadioButtonId();
            if (checkedRadioButtonId != -1) {
                RadioButton checkedRadioButton = view.findViewById(checkedRadioButtonId);
                String selectedStoreName = checkedRadioButton.getText().toString();

                // Find the corresponding Store object from the list
                String selectedStoreId = findStoreIdByName(selectedStoreName);

                if (selectedStoreId != null) {
                    onStoreSelectedListener.onStoreSelected(selectedStoreId);
                    dismiss();
                } else {
                    // Handle the case where the selected store is not found
                    Toast.makeText(requireContext(), "Selected store not found", Toast.LENGTH_SHORT).show();
                }
            } else {
                // No RadioButton selected
                Toast.makeText(requireContext(), "Please select a store", Toast.LENGTH_SHORT).show();
            }
        });

        // Enable or disable the button based on the selection status
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            boolean isStoreSelected = checkedId != -1;
            continueButton.setEnabled(isStoreSelected);
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setView(view);

        return builder.create();
    }

    // Interface to communicate the selected store back to the calling fragment/activity
    public interface OnStoreSelectedListener {
        void onStoreSelected(String selectedStoreId);
    }

    // Setter for the listener
    public void setOnStoreSelectedListener(OnStoreSelectedListener listener) {
        this.onStoreSelectedListener = listener;
    }

    // Helper method to find the Store object by name
    private String findStoreIdByName(String storeName) {
        for (Store store : stores) {
            if (store.name.equals(storeName)) {
                return store.id;
            }
        }
        return null; // Return null if store is not found
    }

}
