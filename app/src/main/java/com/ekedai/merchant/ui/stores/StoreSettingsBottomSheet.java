package com.ekedai.merchant.ui.stores;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ekedai.merchant.R;
import com.ekedai.merchant.models.HttpResponse;
import com.ekedai.merchant.networking.ServiceGenerator;
import com.ekedai.merchant.networking.apis.StoreApi;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.Calendar;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StoreSettingsBottomSheet extends BottomSheetDialogFragment {

    private String storeId;
    int storePosition;
    TimePicker timePicker;
    StoreAdapter storeAdapter;
    StoreApi storeApiService;
    StoreAdapter.ViewHolder viewHolder;

    public StoreSettingsBottomSheet() {
        super();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return super.onCreateDialog(savedInstanceState);
    }

    public StoreSettingsBottomSheet(
            String storeId,
            int position,
            StoreAdapter.ViewHolder holder,
            StoreAdapter storeAdapter,
            Context context
    ) {
        super();
        this.storeId = storeId;
        this.storePosition = position;
        this.viewHolder = holder;
        this.storeAdapter = storeAdapter;

        storeApiService = ServiceGenerator.createStoreService();
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.store_status_dialog, container, false);

        RadioGroup radioGroup = view.findViewById(R.id.store_status_options);
        timePicker = view.findViewById(R.id.status_timePicker);
        Button confirm = view.findViewById(R.id.confirm_status);
        confirm.setOnClickListener(v -> {
            storeAdapter.startLoading(viewHolder);

            if (timePicker.getVisibility() == View.VISIBLE) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(System.currentTimeMillis());
                calendar.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
                calendar.set(Calendar.MINUTE, timePicker.getMinute());
                calendar.set(Calendar.SECOND, 0);

                int minutes = (int) ((calendar.getTimeInMillis() / 60000) - (System.currentTimeMillis() / 60000));
                snoozeStore(minutes, true);
            } else {
                snoozeStore(0, false);
            }
            dismiss();
        });
        radioGroup.setOnCheckedChangeListener((radioGroup1, i) ->
                timePicker.setVisibility(i == R.id.store_status_paused ? View.VISIBLE : View.GONE));

        return view;
    }

    private void snoozeStore(
            int minutes,
            boolean isClosed
    ) {

        Call<HttpResponse> storeSnoozeCall = storeApiService.updateStoreStatus(storeId, isClosed, minutes);

        storeSnoozeCall.clone().enqueue(new Callback<HttpResponse>() {
            @Override
            public void onResponse(@NonNull Call<HttpResponse> call, @NonNull Response<HttpResponse> response) {
                if (response.isSuccessful()) {
//                    storeAdapter.notifyItemChanged(storePosition);
                    storeAdapter.getStoreStatus(storeId, viewHolder);
                } else {
                    storeAdapter.stopLoading(viewHolder);
                    Toast.makeText(getContext(), "An error occurred. Please try again.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<HttpResponse> call, @NonNull Throwable t) {
                storeAdapter.stopLoading(viewHolder);
                Toast.makeText(getContext(), "An error occurred. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
