package com.symplified.order.ui.tabs;

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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.gson.Gson;
import com.symplified.order.App;
import com.symplified.order.R;
import com.symplified.order.adapters.OrderAdapter;
import com.symplified.order.apis.OrderApi;
import com.symplified.order.databinding.NewOrdersBinding;
import com.symplified.order.models.OrderDetailsModel;
import com.symplified.order.models.order.OrderDetailsResponse;
import com.symplified.order.models.order.OrderResponse;
import com.symplified.order.services.AlertService;

import java.io.IOException;
import java.util.ArrayList;
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
/**
 * A placeholder fragment containing a simple view.
 */
public class PlaceholderFragment extends Fragment {

    private static final String ARG_SECTION = "section";

    private PageViewModel pageViewModel;
    private NewOrdersBinding binding;
    private OrderAdapter orderAdapter;

    private List<OrderDetailsModel> orders;

    private Map<String, String> headers;
    private OrderApi orderApiService;
    private Call<OrderDetailsResponse> orderResponse;
    private String storeId;
    private RecyclerView recyclerView;
    private String section;
    private Dialog progressDialog;
    private BroadcastReceiver ordersReceiver;
    private String BASE_URL;

    private ProgressBar progressBar;
    private SwipeRefreshLayout mainLayout, emptyLayout;
    private TextView emptyOrdersTextView;

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

        if(clientId == null) {
            Toast.makeText(getActivity(), "Client id is null", Toast.LENGTH_SHORT).show();
        }

        BASE_URL = sharedPreferences.getString("base_url", App.BASE_URL);

        Retrofit retrofit = new Retrofit.Builder().client(new OkHttpClient()).baseUrl(BASE_URL + App.ORDER_SERVICE_URL)
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
            case "ongoing": {
                pageViewModel.setIndex(1);
                orderResponse = orderApiService.getOngoingOrdersByClientId(headers, clientId);
                break;
            }
            case "past":{
                pageViewModel.setIndex(2);
                orderResponse = orderApiService.getSentOrdersByClientId(headers, clientId);
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

        updateOrdersEveryFiveMinutes();

        progressBar = binding.ordersProgressBar;
        emptyOrdersTextView = binding.emptyOrdersTextView;

        mainLayout = binding.fullSwipeRefreshLayout;
        emptyLayout = binding.emptySwipeRefreshLayout;

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
        PendingIntent pendingIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            pendingIntent = PendingIntent.getBroadcast(getActivity(), 999, fetchOrdersIntent, PendingIntent.FLAG_IMMUTABLE);
        }
        else
        {
            pendingIntent = PendingIntent.getBroadcast(getActivity(), 999, fetchOrdersIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
//        getActivity().sendBroadcast(fetchOrdersIntent);
        alarmManager.setRepeating(
                AlarmManager.RTC,
                System.currentTimeMillis(),
                5 * 60 * 1000,
                pendingIntent
        );

    }

    public void getOrders(){
//        progressDialog.show();
        startLoading();
        orderResponse.clone().enqueue(new Callback<OrderDetailsResponse>() {
            @Override
            public void onResponse(Call<OrderDetailsResponse> call, Response<OrderDetailsResponse> response) {
                Log.d("ORDERSS: ", "Testing");
                if(response.isSuccessful())
                {
                    orderAdapter = new OrderAdapter(response.body().data.content, section, getActivity());
                    recyclerView.setAdapter(orderAdapter);
                    orderAdapter.notifyDataSetChanged();
                    progressDialog.dismiss();
                    showOrders();
                } else {
                    showErrorMessage();
                }
                stopLoading();

            }

            @Override
            public void onFailure(Call<OrderDetailsResponse> call, Throwable t) {
                progressDialog.dismiss();
                stopLoading();
                showErrorMessage();
            }
        });
    }

    public void startLoading() {
        mainLayout.setRefreshing(true);
        emptyLayout.setRefreshing(true);

        mainLayout.setVisibility(View.GONE);
        emptyLayout.setVisibility(View.GONE);

        progressBar.setVisibility(View.VISIBLE);
    }

    public void stopLoading() {
        mainLayout.setRefreshing(false);
        emptyLayout.setRefreshing(false);
        progressBar.setVisibility(View.GONE);
    }

    public void showOrders() {
        mainLayout.setVisibility(View.VISIBLE);
    }

    private void showEmptyOrdersMessage() {
        emptyOrdersTextView.setText(R.string.empty_orders_text);
        emptyLayout.setVisibility(View.VISIBLE);
    }

    private void showErrorMessage() {
        emptyOrdersTextView.setText(R.string.error_text_pull_to_refresh);
        emptyLayout.setVisibility(View.VISIBLE);
    }
}