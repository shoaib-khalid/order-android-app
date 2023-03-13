package com.symplified.order.services;

import static android.Manifest.permission.BLUETOOTH_CONNECT;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.content.ContextCompat;

import com.symplified.order.App;

public class BluetoothReceiver extends BroadcastReceiver {
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

            if (device != null
                    && device.getName() != null
                    && device.getName().startsWith("CloudPrint")) {
                switch (device.getBondState()) {
                    case BluetoothDevice.BOND_NONE:
                        device.createBond();
                        break;
                    case BluetoothDevice.BOND_BONDED:
                        App.addBtPrinter(device, context.getApplicationContext());
                        break;
                }
            }
        }
    }
}
