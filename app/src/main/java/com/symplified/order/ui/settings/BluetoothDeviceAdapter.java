package com.symplified.order.ui.settings;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.symplified.order.databinding.RowBluetoothDeviceBinding;
import com.symplified.order.models.bluetooth.PairedDevice;

public class BluetoothDeviceAdapter
        extends ListAdapter<PairedDevice, BluetoothDeviceAdapter.DeviceViewHolder> {

    private final OnDeviceToggleListener toggleListener;

    public interface OnDeviceToggleListener {
        void onDeviceToggled(String name, boolean isEnabled);
    }

    public BluetoothDeviceAdapter(OnDeviceToggleListener toggleListener) {
        super(DIFF_CALLBACK);
        this.toggleListener = toggleListener;
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        DeviceViewHolder holder = new DeviceViewHolder(RowBluetoothDeviceBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false)
        );
        holder.binding.deviceStatusSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                toggleListener.onDeviceToggled(getItem(holder.getAdapterPosition()).name, isChecked));
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    public static class DeviceViewHolder extends RecyclerView.ViewHolder {

        private final RowBluetoothDeviceBinding binding;

        public DeviceViewHolder(RowBluetoothDeviceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(PairedDevice device) {
            binding.deviceName.setText(device.name);
            binding.deviceStatusText.setText(device.isEnabled ? "Printing enabled." : "Printing disabled.");
            binding.deviceStatusSwitch.setChecked(device.isEnabled);
        }
    }

    public static final DiffUtil.ItemCallback<PairedDevice> DIFF_CALLBACK
            = new DiffUtil.ItemCallback<PairedDevice>() {
        @Override
        public boolean areItemsTheSame(
                @NonNull PairedDevice oldDevice,
                @NonNull PairedDevice newDevice
        ) {
            return oldDevice.name.equals(newDevice.name);
        }

        @Override
        public boolean areContentsTheSame(
                @NonNull PairedDevice oldDevice,
                @NonNull PairedDevice newDevice
        ) {
            return oldDevice.isEnabled == newDevice.isEnabled;
        }
    };
}
