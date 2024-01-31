package com.ekedai.merchant.ui.voucher.tabs;

import static com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate.InstallState.STATE_CANCELED;
import static com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate.InstallState.STATE_COMPLETED;
import static com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate.InstallState.STATE_FAILED;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.ekedai.merchant.App;
import com.ekedai.merchant.R;
import com.ekedai.merchant.databinding.FragmentVoucherScanBinding;
import com.ekedai.merchant.models.store.Store;
import com.ekedai.merchant.models.store.StoreResponse;
import com.ekedai.merchant.models.voucher.VoucherQrCodeDetails;
import com.ekedai.merchant.networking.ServiceGenerator;
import com.ekedai.merchant.networking.apis.StoreApi;
import com.ekedai.merchant.ui.voucher.StoreSelectionDialogFragment;
import com.ekedai.merchant.ui.voucher.VoucherDetailsActivity;
import com.ekedai.merchant.utils.SharedPrefsKey;
import com.google.android.gms.common.api.OptionalModuleApi;
import com.google.android.gms.common.moduleinstall.InstallStatusListener;
import com.google.android.gms.common.moduleinstall.ModuleInstall;
import com.google.android.gms.common.moduleinstall.ModuleInstallClient;
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest;
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VoucherScanFragment extends Fragment implements InstallStatusListener, StoreSelectionDialogFragment.OnStoreSelectedListener {

    private FragmentVoucherScanBinding binding;
    final String TAG = "voucher-fragment";
    String[] storeIds = {};

    private StoreApi storeApiService;
    private String clientId;
    private String selectedStoreId;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentVoucherScanBinding.inflate(inflater, container, false);
        storeIds = App.getAppContext()
                .getSharedPreferences(App.SESSION, Context.MODE_PRIVATE)
                .getString(SharedPrefsKey.STORE_ID_LIST, "")
                .split(" ");
        clientId = requireActivity().getSharedPreferences(App.SESSION, Context.MODE_PRIVATE)
                .getString(SharedPrefsKey.CLIENT_ID, null);
        storeApiService = ServiceGenerator.createStoreService();

        return binding.getRoot();
    }

    ModuleInstallClient moduleInstallClient;
    OptionalModuleApi optionalModuleApi;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        moduleInstallClient = ModuleInstall.getClient(view.getContext());
        optionalModuleApi = GmsBarcodeScanning.getClient(view.getContext());
        moduleInstallClient.areModulesAvailable(optionalModuleApi)
                .addOnSuccessListener(response -> {
                    Log.d(TAG, "moduleInstallClient response.areModulesAvailable: " +
                            response.areModulesAvailable());
                    if (!response.areModulesAvailable()) {
                        binding.scanVoucherCodeButton.setEnabled(false);
                        installScannerModule();
                    } else {
                        binding.scanVoucherCodeButton.setEnabled(true);
                    }
                }).addOnFailureListener(e -> Toast.makeText(view.getContext(),
                        "Failed to check availability of scanner module. " + e.getLocalizedMessage(),
                        Toast.LENGTH_SHORT).show());

        binding.scanVoucherCodeButton.setOnClickListener(v -> {
            moduleInstallClient.areModulesAvailable(optionalModuleApi)
                    .addOnSuccessListener(response -> {
                        Log.d(TAG, "moduleInstallClient response.areModulesAvailable: " +
                                response.areModulesAvailable());
                        if (!response.areModulesAvailable()) {
                            binding.scanVoucherCodeButton.setEnabled(false);
                            installScannerModule();
                        } else {
                            // If it only has 1 store, do not show the store selection dialog
                            if (storeIds.length == 1) {
                                this.selectedStoreId = storeIds[0];
                                startScan();
                            }
                            else {
                                showStoreSelectionDialog();
                            }
                        }
                    }).addOnFailureListener(e -> Toast.makeText(binding.getRoot().getContext(),
                            "Failed to check availability of scanner module. " + e.getLocalizedMessage(),
                            Toast.LENGTH_SHORT).show());
        });
    }

    private void startScan() {
        GmsBarcodeScanning.getClient(
                binding.getRoot().getContext(),
                new GmsBarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_QR_CODE, Barcode.FORMAT_AZTEC)
                        .enableAutoZoom()
                        .build()
        ).startScan().addOnSuccessListener(barcode -> {
            try {
                Log.d("voucher", "Scanned QR code: " + barcode.getRawValue());
                VoucherQrCodeDetails voucherDetails = new Gson().fromJson(barcode.getRawValue(), VoucherQrCodeDetails.class);

                if (voucherDetails.isGlobalStore != null) {
                    if (voucherDetails.isGlobalStore || this.selectedStoreId.equals(voucherDetails.storeId)) {
                        Intent intent = new Intent(requireActivity(), VoucherDetailsActivity.class);
                        intent.putExtra(VoucherDetailsActivity.VOUCHER_DETAILS_KEY, voucherDetails);
                        intent.putExtra(VoucherDetailsActivity.SELECTED_STORE_ID_KEY, this.selectedStoreId);
                        startActivity(intent);
                    } else {
                        Toast.makeText(requireActivity(), "QR Code is not valid for this store", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(requireActivity(), "Invalid QR Code", Toast.LENGTH_SHORT).show();
                }

            } catch (JsonSyntaxException ex) {
                Toast.makeText(requireActivity(), "Invalid QR Code", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            String errorMessage = "Failed to scan QR Code: " + e.getLocalizedMessage();
            Log.e(TAG, errorMessage);
            Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_SHORT).show();
        });
    }

    private void installScannerModule() {
        ModuleInstallRequest installRequest = ModuleInstallRequest.newBuilder()
                .addApi(optionalModuleApi)
                .setListener(this)
                .build();

        binding.installProgressBar.setVisibility(View.VISIBLE);
        binding.installText.setText(getString(R.string.installing_scanner_module));
        binding.installText.setVisibility(View.VISIBLE);

        moduleInstallClient.installModules(installRequest)
                .addOnSuccessListener(response -> {
                    if (response.areModulesAlreadyInstalled()) {
                        binding.scanVoucherCodeButton.setEnabled(true);
                    }
                }).addOnFailureListener(e -> {
                    Toast.makeText(binding.getRoot().getContext(),
                            "Failed to install scanner module. " + e.getLocalizedMessage(),
                            Toast.LENGTH_SHORT
                    ).show();
                    binding.scanVoucherCodeButton.setEnabled(true);
                    binding.installText.setText("Module installation failed");

                });
    }

    @Override
    public void onInstallStatusUpdated(@NonNull ModuleInstallStatusUpdate update) {
        ModuleInstallStatusUpdate.ProgressInfo progressInfo = update.getProgressInfo();
        // Progress info is only set when modules are in the progress of downloading.
        if (progressInfo != null) {
            int progress =
                    (int) (progressInfo.getBytesDownloaded() * 100 / progressInfo.getTotalBytesToDownload());
            binding.installProgressBar.setProgress(progress);
        }

        if (isTerminateState(update.getInstallState())) {
            moduleInstallClient.unregisterListener(this);
            if (update.getInstallState() == STATE_COMPLETED) {
                binding.installText.setText("Module installed");
            } else {
                binding.installText.setText("Module installation failed");
            }
            binding.scanVoucherCodeButton.setEnabled(true);
        }
    }

    public boolean isTerminateState(@ModuleInstallStatusUpdate.InstallState int state) {
        return state == STATE_CANCELED || state == STATE_COMPLETED || state == STATE_FAILED;
    }

    private void showStoreSelectionDialog() {
        // Show a loading spinner on the dialog
        ProgressDialog progressDialog = ProgressDialog.show(requireContext(), "Loading", "Fetching store data...", true);

        // Start the asynchronous request to get stores
        storeApiService.getStores(clientId).clone().enqueue(new Callback<StoreResponse>() {
            @Override
            public void onResponse(@NonNull Call<StoreResponse> call,
                                   @NonNull Response<StoreResponse> response) {
                // Dismiss the loading spinner
                progressDialog.dismiss();
                if (response.isSuccessful() && response.body() != null) {
                    List<Store> stores = response.body().data.content;
                    showStoreSelectionDialogWithData(stores);
                } else {
                    Toast.makeText(requireContext(), "Failed to retrieve stores", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<StoreResponse> call, @NonNull Throwable t) {
                // Dismiss the loading spinner
                progressDialog.dismiss();

                Toast.makeText(requireContext(), "Network error: " + t.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showStoreSelectionDialogWithData(List<Store> storeList) {
        // Check if the fragment is still added to the activity
        // and the activity is not destroyed before showing the dialog
        if (isAdded() && !requireActivity().isDestroyed()) {
            StoreSelectionDialogFragment storeSelectionDialogFragment = new StoreSelectionDialogFragment(storeList);
            storeSelectionDialogFragment.setOnStoreSelectedListener(this);
            storeSelectionDialogFragment.show(getChildFragmentManager(), "StoreSelectionDialogFragment");
        }
    }

    @Override
    public void onStoreSelected(String selectedStoreId) {
        Log.d("VoucherScanFragment", "Selected store ID: " + selectedStoreId);
        this.selectedStoreId = selectedStoreId;
        startScan();
    }
}