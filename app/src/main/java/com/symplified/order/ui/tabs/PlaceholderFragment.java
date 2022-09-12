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
import com.symplified.order.models.order.Order;
import com.symplified.order.models.order.OrderDetailsResponse;
import com.symplified.order.networking.ServiceGenerator;
import com.symplified.order.observers.OrderObserver;
import com.symplified.order.observers.OrderMediator;
import com.symplified.order.observers.PrinterObserver;
import com.symplified.order.services.AlertService;
import com.symplified.order.services.OrderNotificationService;

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
public class PlaceholderFragment extends Fragment
        implements PrinterObserver, OrderObserver, OrderMediator {

    private static final String ARG_SECTION = "section";

    private PageViewModel pageViewModel;
    private NewOrdersBinding binding;
    private OrderAdapter orderAdapter;

    private List<Order.OrderDetails> orders = new ArrayList<>();

    private Call<OrderDetailsResponse> orderResponse;
    private RecyclerView recyclerView;
    private String section;
    private BroadcastReceiver ordersReceiver;

    private ProgressBar progressBar;
    private SwipeRefreshLayout mainLayout, emptyLayout;
    private TextView emptyOrdersTextView;
    private OrderMediator orderMediator;

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

        SharedPreferences sharedPreferences = getActivity().getSharedPreferences(App.SESSION_DETAILS_TITLE, Context.MODE_PRIVATE);

        String clientId = sharedPreferences.getString("ownerId", null);

        if (clientId == null) {
            Toast.makeText(getActivity(), "Client id is null", Toast.LENGTH_SHORT).show();
        }

        orders = new ArrayList<>();

        Map<String, String> headers = new HashMap<>();
        OrderApi orderApiService = ServiceGenerator.createOrderService();

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
                OrderNotificationService.addObserver(this);
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
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        binding = NewOrdersBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        recyclerView = root.findViewById(R.id.order_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

//        updateOrdersEveryFiveMinutes();

        progressBar = binding.ordersProgressBar;
        emptyOrdersTextView = binding.emptyOrdersTextView;

        mainLayout = binding.fullSwipeRefreshLayout;
        emptyLayout = binding.emptySwipeRefreshLayout;

        mainLayout.setOnRefreshListener(this::getOrders);
        emptyLayout.setOnRefreshListener(this::getOrders);

        getOrders();

        return root;
    }


    @Override
    public void onResume() {
        super.onResume();

//        getActivity().getSupportFragmentManager().beginTransaction().detach(this).attach(this).commit();

//        getOrders();

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
        OrderNotificationService.removeObserver(this);
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
        OrderMediator orderMediator = this;
        startLoading();
        orderResponse.clone().enqueue(new Callback<OrderDetailsResponse>() {
            @Override
            public void onResponse(Call<OrderDetailsResponse> call, Response<OrderDetailsResponse> response) {
                if (response.isSuccessful()) {
                    orders = response.body().data.content;
                    orderAdapter = new OrderAdapter(orders, section, getContext(), orderMediator);
                    recyclerView.setAdapter(orderAdapter);
                    orderAdapter.notifyDataSetChanged();
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
                stopLoading();
                showErrorMessage();
                Log.e("order-activity", "onFailure error getting orders: " + t.getLocalizedMessage());
            }
        });
    }

    private void startLoading() {
        mainLayout.setRefreshing(true);
        emptyLayout.setRefreshing(true);
    }

    private void stopLoading() {
        mainLayout.setRefreshing(false);
        emptyLayout.setRefreshing(false);
        progressBar.setVisibility(View.GONE);
    }

    private void showOrders() {
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

    @Override
    public void onOrderReceived(Order.OrderDetails orderDetails) {
        if (orderAdapter != null && orders.add(orderDetails)) {
            orderAdapter.notifyItemInserted(orders.indexOf(orderDetails));
            showOrders();
        }
    }

    @Override
    public void addOrderToOngoingTab(Order.OrderDetails orderDetails) {
        if (orderMediator != null) {
            orderMediator.addOrderToOngoingTab(orderDetails);
        }
    }

    public void setOrderMediator(OrderMediator mediator) {
        this.orderMediator = mediator;
    }

    public void addOrder(Order.OrderDetails orderDetails) {

    }
}