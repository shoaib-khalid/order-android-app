package com.symplified.easydukan.models.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

public class PairedDevice {
    public final BluetoothDevice device;
    public BluetoothSocket socket;
    public boolean isEnabled = true;
    public final String deviceName;

    public PairedDevice(
            BluetoothDevice device,
            BluetoothSocket socket,
            String deviceName
    ) {
        this.device = device;
        this.socket = socket;
        this.deviceName = deviceName;
    }
}
