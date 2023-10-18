package com.ekedai.merchant.ui.voucher.tabs;

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
import com.ekedai.merchant.models.voucher.VoucherDetails;
import com.ekedai.merchant.models.voucher.VoucherQrCodeDetails;
import com.ekedai.merchant.ui.voucher.VoucherDetailsActivity;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner;
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;

public class VoucherScanFragment extends Fragment {

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

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        binding.scanVoucherCodeButton.setOnClickListener(v -> {

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
        });
    }

//    public void
}