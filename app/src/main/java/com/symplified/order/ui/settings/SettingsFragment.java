package com.symplified.order.ui.settings;

import static android.Manifest.permission.BLUETOOTH_CONNECT;
import static com.symplified.order.App.PRINT_TAG;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.symplified.order.App;
import com.symplified.order.R;
import com.symplified.order.databinding.FragmentSettingsBinding;
import com.symplified.order.models.bluetooth.PairedDevice;
import com.symplified.order.services.BluetoothReceiver;
import com.symplified.order.utils.SharedPrefsKey;

import java.util.ArrayList;
import java.util.List;

public class SettingsFragment extends Fragment
        implements BluetoothReceiver.OnBluetoothDeviceAddedListener,
        BluetoothDeviceAdapter.OnDeviceToggleListener {

    private FragmentSettingsBinding binding;
    private BluetoothDeviceAdapter deviceAdapter;
    private List<PairedDevice> deviceList = new ArrayList<>();

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
        deviceAdapter = new BluetoothDeviceAdapter(this);
        binding.deviceRecyclerView.setAdapter(deviceAdapter);
        binding.deviceRecyclerView.setLayoutManager(new LinearLayoutManager(requireActivity()));
//        binding.errorText.setVisibility(App.btDevices.isEmpty() && !App.isAddingBluetoothDevice ? View.VISIBLE : View.GONE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && ContextCompat.checkSelfPermission(
                requireContext(), BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED) {
            binding.progressBar.setVisibility(View.GONE);
            binding.errorText.setText(R.string.bluetooth_permission_error);
            binding.errorText.setVisibility(View.VISIBLE);
            return;
        }

        List<PairedDevice> pairedDevices = new ArrayList<>();
        SharedPreferences sharedPrefs = requireContext().getSharedPreferences(
                SharedPrefsKey.BT_DEVICE_PREFS_FILE_NAME, Context.MODE_PRIVATE
        );
        for (BluetoothDevice device : ((BluetoothManager) requireContext().getSystemService(Context.BLUETOOTH_SERVICE))
                .getAdapter().getBondedDevices()) {
            Log.d(PRINT_TAG, "Adding device " + device.getName());
            pairedDevices.add(new PairedDevice(device.getName(), sharedPrefs.getBoolean(device.getName(), false)));
        }
        deviceList.clear();
//        deviceAdapter.submitList(null);
        deviceList.addAll(pairedDevices);
        binding.progressBar.setVisibility(View.GONE);
        deviceAdapter.submitList(pairedDevices);

        if (pairedDevices.isEmpty()) {
            binding.errorText.setText(getString(R.string.empty_bluetooth_device_text));
            binding.errorText.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        BluetoothReceiver.removeDeviceListener(this);
    }

    @Override
    public void onBluetoothDeviceAdded(PairedDevice deviceToAdd) {
        binding.errorText.setVisibility(View.GONE);

        boolean deviceExists = false;
        for (PairedDevice device : deviceList) {
            if (device.name.equals(deviceToAdd.name)) {
                deviceExists = true;
                break;
            }
        }

        if (!deviceExists) {
            List<PairedDevice> newList = new ArrayList<>(deviceList);
            newList.add(deviceToAdd);
            deviceAdapter.submitList(newList);
            deviceList = newList;
        }
    }

    @Override
    public void onDeviceToggled(String name, boolean isEnabled) {
        int indexOf = -1;
        for (int i = 0; i < deviceList.size(); i++) {
            PairedDevice device = deviceList.get(i);
            if (name.equals(device.name)) {
                if (device.isEnabled == isEnabled) {
                    return;
                }

                device.isEnabled = isEnabled;
                indexOf = i;
                break;
            }
        }

        deviceAdapter.submitList(deviceList);
        deviceAdapter.notifyItemChanged(indexOf);

        requireContext().getSharedPreferences(
                SharedPrefsKey.BT_DEVICE_PREFS_FILE_NAME, Context.MODE_PRIVATE
        ).edit().putBoolean(name, isEnabled).apply();
    }
}