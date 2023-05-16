package com.symplified.order.services;

import static android.Manifest.permission.BLUETOOTH_CONNECT;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class BluetoothReceiver extends BroadcastReceiver {

    private static final List<OnBluetoothDeviceAddedListener> deviceListeners = new ArrayList<>();

    public interface OnBluetoothDeviceAddedListener {
        void onBluetoothDeviceAdded(BluetoothDevice device);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null
                && intent.getAction().equals(BluetoothDevice.ACTION_FOUND)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    ContextCompat.checkSelfPermission(
                            context.getApplicationContext(),
                            BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_DENIED
            ) {
                return;
            }

            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (device != null) {
                switch (device.getBondState()) {
                    case BluetoothDevice.BOND_NONE:
                        device.createBond();
                        break;
                    case BluetoothDevice.BOND_BONDED:
                        for (OnBluetoothDeviceAddedListener listener : deviceListeners) {
                            listener.onBluetoothDeviceAdded(device);
                        }
                        break;
                }
            }
        }
    }

    public static void addDeviceListener(OnBluetoothDeviceAddedListener listener) { deviceListeners.add(listener); }
    public static void removeDeviceListener(OnBluetoothDeviceAddedListener listener) { deviceListeners.remove(listener); }
}
