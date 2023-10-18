package com.ekedai.merchant.ui.voucher.tabs;

import static com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate.InstallState.STATE_CANCELED;
import static com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate.InstallState.STATE_COMPLETED;
import static com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate.InstallState.STATE_FAILED;

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

import com.ekedai.merchant.R;
import com.ekedai.merchant.databinding.FragmentVoucherScanBinding;
import com.ekedai.merchant.models.voucher.VoucherQrCodeDetails;
import com.ekedai.merchant.ui.voucher.VoucherDetailsActivity;
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

public class VoucherScanFragment extends Fragment implements InstallStatusListener {

    private FragmentVoucherScanBinding binding;
    final String TAG = "voucher-fragment";


    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentVoucherScanBinding.inflate(inflater, container, false);
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
                            startScan();
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
                Intent intent = new Intent(requireActivity(), VoucherDetailsActivity.class);
                intent.putExtra(VoucherDetailsActivity.VOUCHER_DETAILS_KEY, voucherDetails);
                startActivity(intent);
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

}