package com.symplified.order.adapters;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.symplified.order.App;
import com.symplified.order.R;
import com.symplified.order.apis.StoreApi;
import com.symplified.order.dialogs.SettingsBottomSheet;
import com.symplified.order.models.Store.Store;
import com.symplified.order.networking.ServiceGenerator;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class StoreAdapter extends RecyclerView.Adapter<StoreAdapter.ViewHolder> {
    private static final String TAG = "StoreAdapter";
    public List<Store> items;
    public Context context;
    private Dialog progressDialog;
    private SharedPreferences sharedPreferences;
    private StoreApi storeApiService;

    public StoreAdapter(List<Store> items, Context context, Dialog progressDialog, SharedPreferences sharedPreferences){
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
        View listItem= layoutInflater.inflate(R.layout.store_list_item, parent, false);
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
            bottomSheetDialogFragment.show(((FragmentActivity) context).getSupportFragmentManager(),"bottomSheetDialog");
        });
    }


    @Override
    public int getItemCount() {
        return items.size();
    }

    public boolean isEmpty(){return items.isEmpty();}

    public void getStoreStatus(String storeId, ViewHolder holder){

        Map<String, String> headers = new HashMap<>();

        Call<ResponseBody> getStoreStatusCall = storeApiService.getStoreStatus(headers, storeId);
        getStoreStatusCall.clone().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Log.d(TAG, "onResponse: "+response.raw());
                if(response.isSuccessful()){
                    try {
                        JSONObject responseJson = new JSONObject(response.body().string().toString());
                        if(responseJson.getJSONObject("data").getBoolean("isSnooze")){
                            setStoreStatus(responseJson, holder.status);
                        }
                        else
                            holder.status.setText("Open");
                    } catch (JSONException | IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "onFailure: ", t);
            }
        });

    }

    public void setStoreStatus(JSONObject responseJson, TextView status) {
        SimpleDateFormat dtf = new SimpleDateFormat("hh:mm a");
        Calendar calendar = new GregorianCalendar();
        String closedUntil = null;
        try {
            closedUntil = responseJson.getJSONObject("data").getString("snoozeEndTime");
            calendar.setTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(closedUntil));
        } catch (JSONException | ParseException e) {
            e.printStackTrace();
        }
        status.setText("Closed Until : "+ dtf.format(calendar.getTime()));
    }

}