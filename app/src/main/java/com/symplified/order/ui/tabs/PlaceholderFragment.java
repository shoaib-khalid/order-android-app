package com.symplified.order.ui.tabs;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.gson.Gson;
import com.symplified.order.App;
import com.symplified.order.R;
import com.symplified.order.adapters.OrderAdapter;
import com.symplified.order.apis.OrderApi;
import com.symplified.order.databinding.NewOrdersBinding;
import com.symplified.order.models.OrderDetailsModel;
import com.symplified.order.models.order.OrderResponse;
import com.symplified.order.services.AlertService;
import com.symplified.order.services.StoreBroadcastReceiver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
/**
 * A placeholder fragment containing a simple view.
 */
public class PlaceholderFragment extends Fragment {

    private static final String ARG_SECTION = "section";

    private PageViewModel pageViewModel;
    private NewOrdersBinding binding;
    private OrderAdapter orderAdapter;

    private Retrofit retrofit;
    private List<OrderDetailsModel> orders;

    private Map<String, String> headers;
    private OrderApi orderApiService;
    private Call<ResponseBody> orderResponse;
    private String storeId;
    private RecyclerView recyclerView;
    private String section;
    private Dialog progressDialog;
    private BroadcastReceiver ordersReceiver;
    String BASE_URL;

    public static PlaceholderFragment newInstance(String type) {
        PlaceholderFragment fragment = new PlaceholderFragment();
        Bundle bundle = new Bundle();
        bundle.putString(ARG_SECTION, type);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        pageViewModel = new ViewModelProvider(this).get(PageViewModel.class);
        progressDialog = new Dialog(getContext());
        progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        progressDialog.setContentView(R.layout.progress_dialog);
        progressDialog.setCancelable(false);
        CircularProgressIndicator progressIndicator = progressDialog.findViewById(R.id.progress);
        progressIndicator.setIndeterminate(true);

        SharedPreferences sharedPreferences = getActivity().getSharedPreferences(App.SESSION_DETAILS_TITLE, Context.MODE_PRIVATE);
        storeId = sharedPreferences.getString("storeId", null);

        String clientId = sharedPreferences.getString("ownerId", null);

        if(clientId == null)
            Toast.makeText(getActivity(), "Client id is null", Toast.LENGTH_SHORT).show();

        Log.i("CHECKCLIENTID", "onCreate: clientId = " + clientId );

        BASE_URL = sharedPreferences.getString("base_url", App.BASE_URL);

        retrofit = new Retrofit.Builder().client(new OkHttpClient()).baseUrl(BASE_URL+App.ORDER_SERVICE_URL)
                .addConverterFactory(GsonConverterFactory.create()).build();

        orders = new ArrayList<>();

        headers = new HashMap<>();
        headers.put("Authorization", "Bearer Bearer accessToken");

        orderApiService = retrofit.create(OrderApi.class);


        section = null;
        if (getArguments() != null) {
            section = getArguments().getString(ARG_SECTION);
        }

        pageViewModel.setIndex(0);


        switch (section){
            case "processed":
            {
                pageViewModel.setIndex(1);
                orderResponse = orderApiService.getProcessedOrdersByClientId(headers, clientId);
                break;
            }
            case "sent":{
                pageViewModel.setIndex(3);
                orderResponse = orderApiService.getSentOrdersByClientId(headers, clientId);
                break;
            }
            case "new" :{
                pageViewModel.setIndex(0);
                orderResponse = orderApiService.getNewOrdersByClientId(headers, clientId);
                if(AlertService.isPlaying()){
                    getActivity().stopService(new Intent(getContext(), AlertService.class));
                }
                NotificationManager notificationManager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.cancelAll();
                break;
            }
            case "pickup": {
                pageViewModel.setIndex(2);
                orderResponse = orderApiService.getPickupOrdersByClientId(headers, clientId);
                break;
            }
        }

        ordersReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
//                if(section.equals("new")){
                    Toast.makeText(getContext(), "Updating orders", Toast.LENGTH_SHORT).show();
                    onResume();
//                }
            }
        };
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        binding = NewOrdersBinding.inflate(inflater, container, false);
        View root = binding.getRoot();


        recyclerView = root.findViewById(R.id.order_recycler);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        DividerItemDecoration mDividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(mDividerItemDecoration);

        Log.e("TAG", "URL : "+orderResponse.request().url(), new Error() );

        updateOrdersEveryFiveMinutes();

//        getOrders();
        return root;
    }


    @Override
    public void onResume() {
        super.onResume();
        getActivity().getSupportFragmentManager().beginTransaction().detach(this).attach(this).commit();

        getOrders();

        if(AlertService.isPlaying()){
            getActivity().stopService(new Intent(getContext(), AlertService.class));
        }
        NotificationManager notificationManager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();

    }

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter("com.symplified.order.GET_ORDERS");
        if(getContext() != null){
            getContext().registerReceiver(ordersReceiver, filter);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if(getContext() != null){
            getContext().unregisterReceiver(ordersReceiver);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public void updateOrdersEveryFiveMinutes(){
        AlarmManager alarmManager = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
        Intent fetchOrdersIntent = new Intent("com.symplified.order.GET_ORDERS");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getActivity(), 999, fetchOrdersIntent, PendingIntent.FLAG_UPDATE_CURRENT);

//        getActivity().sendBroadcast(fetchOrdersIntent);
        alarmManager.setRepeating(
                AlarmManager.RTC,
                System.currentTimeMillis(),
                5 * 60 * 1000,
                pendingIntent
        );
    }

    public void getOrders(){
        progressDialog.show();
        orderResponse.clone().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                if(response.isSuccessful())
                {
                    try {
                        OrderResponse orderResponse = new Gson().fromJson(response.body().string(), OrderResponse.class);
                        orderAdapter = new OrderAdapter(orderResponse.data.content, section, getActivity());
                        recyclerView.setAdapter(orderAdapter);
                        orderAdapter.notifyDataSetChanged();
                        progressDialog.dismiss();
                        Log.e("TAG", "Size: "+ orderResponse.data.content.size(),  new Error());

                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                }

            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                progressDialog.dismiss();
            }
        });
    }

}