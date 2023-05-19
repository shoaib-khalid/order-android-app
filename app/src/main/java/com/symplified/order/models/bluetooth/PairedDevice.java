package com.symplified.order.models.bluetooth;

public class PairedDevice {
    public final String name;
    public boolean isEnabled = true;

    public PairedDevice(String name, boolean isEnabled) {
        this.name = name;
        this.isEnabled = isEnabled;
    }
}
