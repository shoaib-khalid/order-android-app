package com.symplified.order.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.symplified.order.R;
import com.symplified.order.adapters.StoreAdapter;
import com.symplified.order.apis.StoreApi;
import com.symplified.order.models.HttpResponse;
import com.symplified.order.networking.ServiceGenerator;

import java.time.Instant;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingsBottomSheet extends BottomSheetDialogFragment {

    private String storeId;
    private TextView status;
    private final String TAG = SettingsBottomSheet.class.getName();
    TimePicker timePicker;
    StoreAdapter storeAdapter;
    StoreApi storeApiService;
    public SettingsBottomSheet (){
        super();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return super.onCreateDialog(savedInstanceState);
    }

    public SettingsBottomSheet(String storeId, TextView status, StoreAdapter storeAdapter){
        super();
        this.storeId = storeId;
        this.status = status;
        this.storeAdapter = storeAdapter;

        storeApiService = ServiceGenerator.createStoreService();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.store_status_dialog, container, false);

        RadioGroup radioGroup = view.findViewById(R.id.store_status_options);
        timePicker = view.findViewById(R.id.status_timePicker);
        Button confirm = view.findViewById(R.id.confirm_status);
        confirm.setOnClickListener(v -> {
            if(timePicker.getVisibility() == View.VISIBLE){
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(System.currentTimeMillis());
                calendar.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
                calendar.set(Calendar.MINUTE, timePicker.getMinute());
                calendar.set(Calendar.SECOND,0);

                int minutes = (int) ((calendar.getTimeInMillis()/60000) - (System.currentTimeMillis()/60000));
                snoozeStore(minutes, true);
                Toast.makeText(getContext(), "Closed Until "+timePicker.getHour()+":"+timePicker.getMinute(), Toast.LENGTH_SHORT).show();
            }
            else{
                snoozeStore(0, false);
                Toast.makeText(getContext(), "Store Opened", Toast.LENGTH_SHORT).show();
            }
            dismiss();
        });
        radioGroup.setOnCheckedChangeListener((radioGroup1, i) -> {
            switch (i) {
                case R.id.store_status_paused: {
                    timePicker.setVisibility(View.VISIBLE);
                    break;
                }
                default: {
                    timePicker.setVisibility(View.GONE);
                    break;
                }
            }
        });

        // Set timepicker default to PM
        Calendar now = Calendar.getInstance();
        if (now.get(Calendar.AM_PM) == Calendar.AM) {
            now.set(Calendar.AM_PM, Calendar.PM);
            timePicker.setHour(now.get(Calendar.HOUR_OF_DAY));
        }

        return view;
    }

    private void snoozeStore(int minutes, boolean isClosed) {

        Call<HttpResponse> storeSnoozeCall = storeApiService.updateStoreStatus(storeId, isClosed, minutes);

        storeSnoozeCall.clone().enqueue(new Callback<HttpResponse>() {
            @Override
            public void onResponse(Call<HttpResponse> call, Response<HttpResponse> response) {
                Log.i(TAG, "onResponse: " + response.raw());
                Log.i(TAG, "Response body: " + response.body().status + ", " + response.body().message);

                if(response.isSuccessful()){
                    if(!isClosed){
                        Log.i(TAG, "onResponse: "+ response.raw());
                        status.setText("Open");
                    }
                    if(minutes > 0){
                        Toast.makeText(status.getContext(), "Closed for "+minutes+" minutes", Toast.LENGTH_SHORT).show();
                    }
                    storeAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<HttpResponse> call, Throwable t) {
                Toast.makeText(getContext(), "Failed", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
