package com.symplified.order.dialogs;

import static android.content.Context.MODE_PRIVATE;

import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.symplified.order.App;
import com.symplified.order.R;
import com.symplified.order.adapters.StoreAdapter;
import com.symplified.order.apis.StoreApi;
import com.symplified.order.services.StoreBroadcastReceiver;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SettingsBottomSheet extends BottomSheetDialogFragment {

    private String storeId;
    private TextView status;
    private final String TAG = SettingsBottomSheet.class.getName();
    TimePicker timePicker;
    StoreAdapter storeAdapter;
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
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.store_status_dialog, container, false);

        RadioGroup radioGroup = view.findViewById(R.id.store_status_options);
        RadioButton normalStatus = view.findViewById(R.id.store_status_normal);
        RadioButton pausedStatus = view.findViewById(R.id.store_status_paused);
        timePicker = view.findViewById(R.id.status_timePicker);
        Button confirm = view.findViewById(R.id.confirm_status);
        confirm.setOnClickListener(v -> {
            if(timePicker.getVisibility() == View.VISIBLE){
                AlarmManager alarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(System.currentTimeMillis());
                calendar.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
                calendar.set(Calendar.MINUTE, timePicker.getMinute());
                calendar.set(Calendar.SECOND,0);

                int minutes = (int) ((calendar.getTimeInMillis()/60000) - (System.currentTimeMillis()/60000));
                snoozeStore(minutes, true);
//                Intent job = new Intent(getContext(), StoreBroadcastReceiver.class);
//                PendingIntent pendingIntent = PendingIntent.getBroadcast(getContext(), 0, job, 0);
//                alarmManager.setExact(AlarmManager.RTC, calendar.getTimeInMillis(), pendingIntent);
                Toast.makeText(getContext(), "Closed Until "+timePicker.getHour()+":"+timePicker.getMinute(), Toast.LENGTH_SHORT).show();
            }
            else{
                snoozeStore(0, false);
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

    private void snoozeStore(int minutes, boolean isClosed) {

        SharedPreferences sharedPreferences = getActivity().getSharedPreferences(App.SESSION_DETAILS_TITLE, MODE_PRIVATE);
        String BASE_URL = sharedPreferences.getString("base_url", App.BASE_URL_STAGING);

        Retrofit retrofit = new Retrofit.Builder()
                .client(new OkHttpClient())
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(BASE_URL + App.PRODUCT_SERVICE_URL)
                .build();
        StoreApi storeApiService = retrofit.create(StoreApi.class);

        Map<String,String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer Bearer accessToken");

        Call<ResponseBody> storeSnoozeCall = storeApiService.updateStoreStatus(headers, storeId, isClosed, minutes);

        storeSnoozeCall.clone().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Log.e(TAG, "onResponse: "+call.request().toString(), new Error() );
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
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(getContext(), "Failed", Toast.LENGTH_SHORT).show();
            }
        });

    }


}
