package com.symplified.order.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.symplified.order.R;
import com.symplified.order.apis.StoreApi;
import com.symplified.order.dialogs.SettingsBottomSheet;
import com.symplified.order.models.store.Store;
import com.symplified.order.models.store.StoreStatusResponse;
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
    public List<Store> items;
    public Context context;

    private StoreApi storeApiService;
    private static final String TAG = "StoreAdapter";

    public StoreAdapter(List<Store> items, Context context) {
        this.items = items;
        this.context = context;

        storeApiService = ServiceGenerator.createStoreService();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView name;
        private final TextView status;
        private final ProgressBar progressBar;
        private boolean isLoading;

        public ViewHolder(View view) {
            super(view);
            isLoading = false;
            name = view.findViewById(R.id.store_name);
            status = view.findViewById(R.id.store_status);
            progressBar = view.findViewById(R.id.progress_bar);
        }

        public boolean isLoading() {
            return isLoading;
        }

        public void setIsLoading(boolean isLoading) {
            this.isLoading = isLoading;
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
        String storeId = items.get(holder.getAdapterPosition()).id;
        getStoreStatus(storeId, holder);

        holder.name.setText(items.get(position).name);

        holder.itemView.setOnClickListener(view -> {
            if (!holder.isLoading()) {
                BottomSheetDialogFragment bottomSheetDialogFragment
                        = new SettingsBottomSheet(storeId, holder.status, position, holder, StoreAdapter.this);
                bottomSheetDialogFragment.show(((FragmentActivity) context)
                        .getSupportFragmentManager(), "bottomSheetDialog");
            }
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
        startLoading(holder);
        holder.status.setText("");

        Call<StoreStatusResponse> getStoreStatusCall = storeApiService.getStoreStatusById(storeId);
        getStoreStatusCall.clone().enqueue(new Callback<StoreStatusResponse>() {
            @Override
            public void onResponse(@NonNull Call<StoreStatusResponse> call, @NonNull Response<StoreStatusResponse> response) {
                if (response.isSuccessful()) {
                    StoreStatusResponse.StoreStatus storeStatus = response.body().data;
                    if (storeStatus.isSnooze) {
                        setStoreStatus(storeStatus.snoozeEndTime, holder.status);
                    } else {
                        holder.status.setText("Open");
                    }
                } else {
                    holder.status.setText("Failed to get store status");
                }
                stopLoading(holder);
            }

            @Override
            public void onFailure(@NonNull Call<StoreStatusResponse> call, @NonNull Throwable t) {
                holder.status.setText("Failed to get store status");
                stopLoading(holder);
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

    public void startLoading(ViewHolder holder) {
        holder.progressBar.setVisibility(View.VISIBLE);
        holder.status.setVisibility(View.GONE);
        holder.setIsLoading(true);
    }

    public void stopLoading(ViewHolder holder) {
        holder.progressBar.setVisibility(View.GONE);
        holder.status.setVisibility(View.VISIBLE);
        holder.setIsLoading(false);
    }
}