package com.symplified.order.ui.settings;

import static android.Manifest.permission.BLUETOOTH_CONNECT;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.symplified.order.App;
import com.symplified.order.databinding.FragmentSettingsBinding;
import com.symplified.order.models.bluetooth.PairedDevice;
import com.symplified.order.ui.NavbarActivity;

import java.util.Set;

public class SettingsFragment extends Fragment
        implements App.OnBluetoothDeviceAddedListener {

    private FragmentSettingsBinding binding;
    private BluetoothDeviceAdapter deviceAdapter;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);

        App.addBluetoothDeviceListener(this);

        return binding.getRoot();
    }


    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {

        binding.progressBar.setVisibility(App.isAddingBluetoothDevice && App.btPrinters.isEmpty() ? View.VISIBLE : View.GONE);
        deviceAdapter = new BluetoothDeviceAdapter();
        binding.deviceRecyclerView.setAdapter(deviceAdapter);
        binding.deviceRecyclerView.setLayoutManager(new LinearLayoutManager(requireActivity()));
        binding.emptyDeviceText.setVisibility(App.btPrinters.isEmpty() && !App.isAddingBluetoothDevice ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        App.removeBluetoothDeviceListener(this);
    }


    @Override
    public void onIsAddingBluetoothDevice(boolean isAdding) {
        requireActivity().runOnUiThread(() -> {
            binding.progressBar.setVisibility(isAdding && App.btPrinters.isEmpty() ? View.VISIBLE : View.GONE);
            binding.emptyDeviceText.setVisibility(!isAdding && App.btPrinters.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    public void onBluetoothDeviceAdded(PairedDevice device) {
        requireActivity().runOnUiThread(() -> {
            deviceAdapter.addDevice(device);
            binding.emptyDeviceText.setVisibility(View.GONE);
        });
    }
}