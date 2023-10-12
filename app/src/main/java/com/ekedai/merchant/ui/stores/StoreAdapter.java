package com.ekedai.merchant.ui.stores;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.ekedai.merchant.R;
import com.ekedai.merchant.enums.NavIntentStore;
import com.ekedai.merchant.models.store.Store;
import com.ekedai.merchant.models.store.StoreStatusResponse;
import com.ekedai.merchant.networking.ServiceGenerator;
import com.ekedai.merchant.networking.apis.StoreApi;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class StoreAdapter extends RecyclerView.Adapter<StoreAdapter.ViewHolder> {

    public interface StoreSelectionListener {
        void onStoreSelected(String storeId);
    }

    public List<Store> items;
    public Context context;

    private final StoreSelectionListener selectionListener;
    private final NavIntentStore action;
    private final StoreApi storeApiService;
    private static final String TAG = "store-adapter";

    public StoreAdapter(List<Store> items,
                        NavIntentStore action,
                        StoreSelectionListener selectionListener,
                        Context context) {
        this.items = items;
        this.action = action;
        this.selectionListener = selectionListener;
        this.context = context;

        storeApiService = ServiceGenerator.createStoreService();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView name, status;
        private final ProgressBar progressBar;
        private final AppCompatImageView qrCodeImage;
        private boolean isLoading;

        public ViewHolder(View view) {
            super(view);
            isLoading = false;
            name = view.findViewById(R.id.store_name);
            status = view.findViewById(R.id.store_status);
            progressBar = view.findViewById(R.id.circular_progress_bar);
            qrCodeImage = view.findViewById(R.id.qr_code_button);
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
        if (action == NavIntentStore.DISPLAY_QR_CODE) {
            holder.qrCodeImage.setVisibility(View.VISIBLE);
            holder.itemView.setOnClickListener(view ->
                    selectionListener.onStoreSelected(storeId));
        } else {
            getStoreStatus(storeId, holder);
            holder.itemView.setOnClickListener(view -> {
                if (!holder.isLoading()) {
                    BottomSheetDialogFragment storeScheduleDialog
                            = new StoreSettingsBottomSheet(storeId, position, holder, StoreAdapter.this, context);
                    storeScheduleDialog.show(((FragmentActivity) context).getSupportFragmentManager(),
                            "bottomSheetDialog");
                }
            });
        }

        holder.name.setText(items.get(position).name);
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

        storeApiService.getStoreStatusById(storeId).clone().enqueue(new Callback<StoreStatusResponse>() {
            @Override
            public void onResponse(@NonNull Call<StoreStatusResponse> call, @NonNull Response<StoreStatusResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
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
        SimpleDateFormat dtFormatter = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        SimpleDateFormat dtParser = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Calendar calendar = new GregorianCalendar();
        String closedText = "Closed";
        try {
            calendar.setTime(dtParser.parse(closedUntil));
            closedText += " Until: " + dtFormatter.format(calendar.getTime());
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