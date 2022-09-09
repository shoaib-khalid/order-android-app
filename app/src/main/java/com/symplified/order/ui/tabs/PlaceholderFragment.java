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
import com.symplified.order.App;
import com.symplified.order.R;
import com.symplified.order.adapters.OrderAdapter;
import com.symplified.order.apis.OrderApi;
import com.symplified.order.databinding.NewOrdersBinding;
import com.symplified.order.helpers.SunmiPrintHelper;
import com.symplified.order.models.OrderDetailsModel;
import com.symplified.order.models.order.Order;
import com.symplified.order.models.order.OrderDetailsResponse;
import com.symplified.order.networking.ServiceGenerator;
import com.symplified.order.observers.PrinterObserver;
import com.symplified.order.services.AlertService;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * A placeholder fragment containing a simple view.
 */
public class PlaceholderFragment extends Fragment implements PrinterObserver {

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

        if (clientId == null) {
            Toast.makeText(getActivity(), "Client id is null", Toast.LENGTH_SHORT).show();
        }

        orders = new ArrayList<>();

        headers = new HashMap<>();
        orderApiService = ServiceGenerator.createOrderService();

        section = null;
        if (getArguments() != null) {
            section = getArguments().getString(ARG_SECTION);
        }

        pageViewModel.setIndex(0);

        switch (section) {
            case "new": {
                pageViewModel.setIndex(0);
                orderResponse = orderApiService.getNewOrdersByClientId(clientId);
                if (AlertService.isPlaying()) {
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
            case "past": {
                pageViewModel.setIndex(2);
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                Date current = new Date();
                String formatDate = formatter.format(current);
                orderResponse = orderApiService.getSentOrdersByClientId(headers, clientId, formatDate, formatDate);
                break;
            }
        }

        ordersReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                onResume();
            }
        };

        SunmiPrintHelper.getInstance().addObserver(this);
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

        mainLayout.setOnRefreshListener(this::getOrders);
        emptyLayout.setOnRefreshListener(this::getOrders);

        return root;
    }


    @Override
    public void onResume() {
        super.onResume();
        getActivity().getSupportFragmentManager().beginTransaction().detach(this).attach(this).commit();

        getOrders();

        if (AlertService.isPlaying()) {
            getActivity().stopService(new Intent(getContext(), AlertService.class));
        }
        NotificationManager notificationManager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();

    }

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter("com.symplified.order.GET_ORDERS");
        if (getContext() != null) {
            getContext().registerReceiver(ordersReceiver, filter);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (getContext() != null) {
            getContext().unregisterReceiver(ordersReceiver);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        SunmiPrintHelper.getInstance().removeObserver(this);
    }

    public void updateOrdersEveryFiveMinutes() {
        AlarmManager alarmManager = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
        Intent fetchOrdersIntent = new Intent("com.symplified.order.GET_ORDERS");
        PendingIntent pendingIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            pendingIntent = PendingIntent.getBroadcast(getActivity(), 999, fetchOrdersIntent, PendingIntent.FLAG_IMMUTABLE);
        } else {
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

    public void getOrders() {
//        progressDialog.show();
        startLoading();
        orderResponse.clone().enqueue(new Callback<OrderDetailsResponse>() {
            @Override
            public void onResponse(Call<OrderDetailsResponse> call, Response<OrderDetailsResponse> response) {
                if (response.isSuccessful()) {
                    List<Order.OrderDetails> orders = response.body().data.content;
                    for (Order.OrderDetails order : orders) {
                        Log.d("order-activity", "Order id: " + order.order.id + ", Invoice Id: " + order.order.invoiceId);
                    }
                    orderAdapter = new OrderAdapter(orders, section, getActivity());
                    recyclerView.setAdapter(orderAdapter);
                    orderAdapter.notifyDataSetChanged();
                    progressDialog.dismiss();
                    if (orders.size() > 0) {
                        showOrders();
                    } else {
                        showEmptyOrdersMessage();
                    }
                } else {
                    showErrorMessage();
                    Log.e("order-activity", "onResponse error getting orders: " + response.raw());
                }
                stopLoading();

            }

            @Override
            public void onFailure(Call<OrderDetailsResponse> call, Throwable t) {
                progressDialog.dismiss();
                stopLoading();
                showErrorMessage();
                Log.e("order-activity", "onFailure error getting orders: " + t.getLocalizedMessage());
            }
        });

        orderResponse.clone().enqueue(new Callback<OrderDetailsResponse>() {
            @Override
            public void onResponse(Call<OrderDetailsResponse> call, Response<OrderDetailsResponse> response) {
                if (response.isSuccessful()) {
                    List<Order.OrderDetails> orders = response.body().data.content;
                    orderAdapter = new OrderAdapter(orders, section, getActivity());
                    recyclerView.setAdapter(orderAdapter);
                    orderAdapter.notifyDataSetChanged();
                    progressDialog.dismiss();
                    if (orders.size() > 0) {
                        showOrders();
                    } else {
                        showEmptyOrdersMessage();
                    }
                } else {
                    showErrorMessage();
                    Log.e("order-activity", "onResponse error getting orders: " + response.raw());
                }
                stopLoading();

            }

            @Override
            public void onFailure(Call<OrderDetailsResponse> call, Throwable t) {
                progressDialog.dismiss();
                stopLoading();
                showErrorMessage();
                Log.e("order-activity", "onFailure error getting orders: " + t.getLocalizedMessage());
            }
        });
    }

    public void startLoading() {
        mainLayout.setRefreshing(true);
        emptyLayout.setRefreshing(true);
    }

    public void stopLoading() {
        mainLayout.setRefreshing(false);
        emptyLayout.setRefreshing(false);
        progressBar.setVisibility(View.GONE);
    }

    public void showOrders() {
        emptyLayout.setVisibility(View.GONE);
        mainLayout.setVisibility(View.VISIBLE);
    }

    private void showEmptyOrdersMessage() {
        emptyOrdersTextView.setText(R.string.empty_orders_text);
        mainLayout.setVisibility(View.GONE);
        emptyLayout.setVisibility(View.VISIBLE);
    }

    private void showErrorMessage() {
        emptyOrdersTextView.setText(R.string.error_text_pull_to_refresh);
        mainLayout.setVisibility(View.GONE);
        emptyLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void onPrinterConnected() {
        if (orderAdapter != null) {
            orderAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onPrinterDisconnected() {
        if (orderAdapter != null) {
            orderAdapter.notifyDataSetChanged();
        }
    }
}