package com.symplified.order.ui.settings;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.symplified.order.App;
import com.symplified.order.databinding.FragmentSettingsBinding;
import com.symplified.order.services.BluetoothReceiver;

public class SettingsFragment extends Fragment
        implements BluetoothReceiver.OnBluetoothDeviceAddedListener {

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

        BluetoothReceiver.addDeviceListener(this);

        return binding.getRoot();
    }


    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {

        binding.progressBar.setVisibility(App.isAddingBluetoothDevice && App.btDevices.isEmpty() ? View.VISIBLE : View.GONE);
        deviceAdapter = new BluetoothDeviceAdapter();
        binding.deviceRecyclerView.setAdapter(deviceAdapter);
        binding.deviceRecyclerView.setLayoutManager(new LinearLayoutManager(requireActivity()));
        binding.emptyDeviceText.setVisibility(App.btDevices.isEmpty() && !App.isAddingBluetoothDevice ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        App.removeBluetoothDeviceListener(this);
    }


    @Override
    public void onBluetoothDeviceAdded(BluetoothDevice device) {
        requireActivity().runOnUiThread(() -> {
            deviceAdapter.addDevice(device);
            binding.emptyDeviceText.setVisibility(View.GONE);
        });
    }
}