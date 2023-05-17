package com.symplified.order.models.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

public class PairedDevice {
    public final String name;
    public boolean isEnabled = true;

    public PairedDevice(String name, boolean isEnabled) {
        this.name = name;
        this.isEnabled = isEnabled;
    }
}
