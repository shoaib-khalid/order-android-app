package com.symplified.order.dialogs;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.symplified.order.R;
import com.symplified.order.services.StoreBroadcastReceiver;

import java.util.Calendar;

public class SettingsBottomSheet extends BottomSheetDialogFragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.store_status_dialog, container, false);

        RadioGroup radioGroup = view.findViewById(R.id.store_status_options);
        RadioButton normalStatus = view.findViewById(R.id.store_status_normal);
        RadioButton pausedStatus = view.findViewById(R.id.store_status_paused);
        TimePicker timePicker = view.findViewById(R.id.status_timePicker);
        Button confirm = view.findViewById(R.id.confirm_status);
        confirm.setOnClickListener(v -> {
            if(timePicker.getVisibility() == View.VISIBLE){
                AlarmManager alarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(System.currentTimeMillis());
                calendar.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
                calendar.set(Calendar.MINUTE, timePicker.getMinute());
                calendar.set(Calendar.SECOND,0);

                Intent job = new Intent(getContext(), StoreBroadcastReceiver.class);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(getContext(), 0, job, 0);
                alarmManager.setExact(AlarmManager.RTC, calendar.getTimeInMillis(), pendingIntent);
                Toast.makeText(getContext(), "Scheduled Job at "+timePicker.getHour()+":"+timePicker.getMinute(), Toast.LENGTH_SHORT).show();
            }
            else{
                Toast.makeText(getContext(), "Store Opened", Toast.LENGTH_SHORT).show();
            }
            dismiss();
        });
//        RadioGroup pausedGroup = view.findViewById(R.id.paused_status);
        radioGroup.setOnCheckedChangeListener((radioGroup1, i) -> {
            switch (i) {
                case R.id.store_status_paused: {
//                    pausedGroup.setVisibility(View.VISIBLE);
                    timePicker.setVisibility(View.VISIBLE);
                    break;
                }
                default: {
//                    pausedGroup.setVisibility(View.GONE);
                    timePicker.setVisibility(View.GONE);
                    break;
                }
            }

        });

        return view;
    }
}
