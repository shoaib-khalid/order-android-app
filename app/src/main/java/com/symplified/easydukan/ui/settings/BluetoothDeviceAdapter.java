package com.symplified.easydukan.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.symplified.easydukan.App;
import com.symplified.easydukan.R;
import com.symplified.easydukan.models.bluetooth.PairedDevice;
import com.symplified.easydukan.utils.SharedPrefsKey;

import java.util.ArrayList;
import java.util.List;

public class BluetoothDeviceAdapter
        extends RecyclerView.Adapter<BluetoothDeviceAdapter.ViewHolder> {

    private final List<PairedDevice> pairedDevices = new ArrayList<>(App.btPrinters);

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView deviceName, deviceStatusText;
        private final SwitchCompat printToggle;

        public ViewHolder(@NonNull View view) {
            super(view);

            deviceName = view.findViewById(R.id.device_name);
            deviceStatusText = view.findViewById(R.id.device_status_text);
            printToggle = view.findViewById(R.id.device_status_switch);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        return new ViewHolder(LayoutInflater.from(viewGroup.getContext())
                .inflate(
                        R.layout.row_bluetooth_device,
                        viewGroup,
                        false
                ));

    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        holder.deviceName.setText(pairedDevices.get(position).deviceName);

        holder.deviceStatusText.setText(
                pairedDevices.get(position).isEnabled
                        ? "Printing enabled"
                        : "Printing disabled."
        );

        SharedPreferences btSharedPrefs = holder.itemView.getContext().getSharedPreferences(
                SharedPrefsKey.BT_DEVICE_PREFS_FILE_NAME, Context.MODE_PRIVATE);

        holder.printToggle.setChecked(btSharedPrefs.getBoolean(pairedDevices.get(position).deviceName, true));
        holder.printToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {

            PairedDevice device = pairedDevices.get(holder.getAdapterPosition());
            device.isEnabled = isChecked;
            holder.deviceStatusText.setText(isChecked ? "Printing enabled" : "Printing disabled");
            PairedDevice selectedDevice = pairedDevices.get(holder.getAdapterPosition());
            btSharedPrefs.edit().putBoolean(device.deviceName, isChecked).apply();
        });
    }

    @Override
    public int getItemCount() {
        return pairedDevices.size();
    }

    public void addDevice(PairedDevice device) {
        pairedDevices.add(device);
        notifyItemInserted(pairedDevices.indexOf(device));
    }
}
