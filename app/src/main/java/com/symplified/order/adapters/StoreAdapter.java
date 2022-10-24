package com.symplified.order.adapters;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.symplified.order.App;
import com.symplified.order.R;
import com.symplified.order.apis.StoreApi;
import com.symplified.order.dialogs.SettingsBottomSheet;
import com.symplified.order.models.store.StoreStatusResponse;
import com.symplified.order.models.store.Store;
import com.symplified.order.networking.ServiceGenerator;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StoreAdapter extends RecyclerView.Adapter<StoreAdapter.ViewHolder> {
    private static final String TAG = "StoreAdapter";
    public List<Store> items;
    public Context context;
    private Dialog progressDialog;
    private SharedPreferences sharedPreferences;
    private StoreApi storeApiService;

    public StoreAdapter(List<Store> items, Context context, Dialog progressDialog, SharedPreferences sharedPreferences) {
        this.items = items;
        this.context = context;
        this.progressDialog = progressDialog;
        this.sharedPreferences = sharedPreferences;

        String BASE_URL = sharedPreferences.getString("base_url", App.BASE_URL);

//        Retrofit retrofitLogo = new Retrofit.Builder()
//                .client(new OkHttpClient())
//                .baseUrl(BASE_URL+App.PRODUCT_SERVICE_URL)
//                .addConverterFactory(GsonConverterFactory.create())
//                .build();
//
//        storeApiService = retrofitLogo.create(StoreApi.class);
        storeApiService = ServiceGenerator.createStoreService();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView name;
        private final TextView status;

        public ViewHolder(View view) {
            super(view);
            name = (TextView) view.findViewById(R.id.store_name);
            status = (TextView) view.findViewById(R.id.store_status);
        }
    }


    @NonNull
    @Override
    public StoreAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View listItem = layoutInflater.inflate(R.layout.store_list_item, parent, false);
        return new StoreAdapter.ViewHolder(listItem);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        progressDialog.setCancelable(false);
        CircularProgressIndicator progressIndicator = progressDialog.findViewById(R.id.progress);
        progressIndicator.setIndeterminate(true);
        String storeId = items.get(holder.getAdapterPosition()).id;
        getStoreStatus(storeId, holder);

        holder.name.setText(items.get(position).name);

        holder.itemView.setOnClickListener(view -> {
            BottomSheetDialogFragment bottomSheetDialogFragment = new SettingsBottomSheet(storeId, holder.status, StoreAdapter.this);
            bottomSheetDialogFragment.show(((FragmentActivity) context).getSupportFragmentManager(), "bottomSheetDialog");
        });
    }


    @Override
    public int getItemCount() {
        return items.size();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public void getStoreStatus(String storeId, ViewHolder holder) {

        Call<StoreStatusResponse> getStoreStatusCall = storeApiService.getStoreStatusById(storeId);
        getStoreStatusCall.clone().enqueue(new Callback<StoreStatusResponse>() {
            @Override
            public void onResponse(Call<StoreStatusResponse> call, Response<StoreStatusResponse> response) {
                Log.d(TAG, "onResponse: " + response.raw());
                if (response.isSuccessful()) {
                    StoreStatusResponse.StoreStatus storeStatus = response.body().data;
                    Log.d(TAG, "Response body: " + response.body().data.toString());
                    if (storeStatus.isSnooze) {
                        setStoreStatus(storeStatus.snoozeEndTime, holder.status);
                    } else {
                        holder.status.setText("Open");
                    }
                }
            }

            @Override
            public void onFailure(Call<StoreStatusResponse> call, Throwable t) {
                Log.e(TAG, "onFailure: ", t);
            }
        });

    }

    public void setStoreStatus(String closedUntil, TextView status) {
        SimpleDateFormat dtf = new SimpleDateFormat("hh:mm a");
        Calendar calendar = new GregorianCalendar();
        String closedText = "Closed";
        try {
            calendar.setTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(closedUntil));
            closedText += " Until: " + dtf.format(calendar.getTime());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        status.setText(closedText);
    }

}