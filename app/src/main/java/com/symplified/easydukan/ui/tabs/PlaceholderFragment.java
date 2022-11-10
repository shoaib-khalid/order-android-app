package com.symplified.easydukan.ui.tabs;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.symplified.easydukan.App;
import com.symplified.easydukan.EditOrderActivity;
import com.symplified.easydukan.R;
import com.symplified.easydukan.adapters.OrderAdapter;
import com.symplified.easydukan.apis.OrderApi;
import com.symplified.easydukan.databinding.NewOrdersBinding;
import com.symplified.easydukan.interfaces.OrderManager;
import com.symplified.easydukan.interfaces.OrderObserver;
import com.symplified.easydukan.interfaces.Printer;
import com.symplified.easydukan.interfaces.PrinterObserver;
import com.symplified.easydukan.models.order.Order;
import com.symplified.easydukan.models.order.OrderDetailsResponse;
import com.symplified.easydukan.networking.ServiceGenerator;
import com.symplified.easydukan.services.AlertService;
import com.symplified.easydukan.services.OrderNotificationService;
import com.symplified.easydukan.utils.Key;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * A placeholder fragment containing a simple view.
 */
public class PlaceholderFragment extends Fragment
        implements PrinterObserver, OrderObserver, OrderManager {

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
    private OrderManager orderManager;

    private ActivityResultLauncher<Intent> editOrderActivityResultLauncher;

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
                OrderNotificationService.addNewOrderObserver(this);
                break;
            }
            case "ongoing": {
                pageViewModel.setIndex(1);
                orderResponse = orderApiService.getOngoingOrdersByClientId(clientId);
                OrderNotificationService.addOngoingOrderObserver(this);
                break;
            }
            case "past": {
                pageViewModel.setIndex(2);
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

                String currentDate = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O
                ? LocalDate.now(TimeZone.getDefault().toZoneId()).toString()
                : formatter.format(new Date());

                orderResponse = orderApiService.getSentOrdersByClientId(clientId, currentDate, currentDate);
                OrderNotificationService.addPastOrderObserver(this);
                break;
            }
        }

        ordersReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                onResume();
            }
        };

        App.getPrinter().addObserver(this);

        editOrderActivityResultLauncher
                = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.d("activity-result", "Activity result called");

                    if (result.getResultCode() == Activity.RESULT_OK && orderAdapter != null) {

                        Log.d("activity-result", "Activity result ok");

                        Intent data = result.getData();
                        Order.OrderDetails updatedOrderDetails
                                = (Order.OrderDetails) data.getSerializableExtra(Key.ORDER_DETAILS);
                        int indexOfUpdatedOrderDetails = -1;
                        for (Order.OrderDetails element : orders) {
                            if (element.order.id
                                    .equals(updatedOrderDetails.order.id)) {
                                indexOfUpdatedOrderDetails = orders.indexOf(element);
                            }
                        }
                        if (indexOfUpdatedOrderDetails != -1) {
                            orders.set(indexOfUpdatedOrderDetails, updatedOrderDetails);
                            orderAdapter.notifyItemChanged(indexOfUpdatedOrderDetails);
                        }
                    }
                });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        binding = NewOrdersBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        recyclerView = root.findViewById(R.id.order_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

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
        App.getPrinter().removeObserver(this);
        OrderNotificationService.removeNewOrderObserver(this);
        OrderNotificationService.removeOngoingOrderObserver(this);
        OrderNotificationService.removePastOrderObserver(this);
    }

    private void getOrders() {
        OrderManager orderManager = this;
        startLoading();
        orderResponse.clone().enqueue(new Callback<OrderDetailsResponse>() {
            @Override
            public void onResponse(Call<OrderDetailsResponse> call, Response<OrderDetailsResponse> response) {
                if (response.isSuccessful()) {
                    orders = response.body().data.content;
                    orderAdapter = new OrderAdapter(orders, section, getContext(), orderManager);
                    recyclerView.setAdapter(orderAdapter);
                    orderAdapter.notifyDataSetChanged();
                    if (orders.size() > 0) {
                        showOrders();
                    } else {
//                        showEmptyOrdersMessage();
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
    public void onPrinterConnected(Printer printer) {
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
        if (orderManager != null) {
            orderManager.addOrderToOngoingTab(orderDetails);
        }
    }

    @Override
    public void addOrderToHistoryTab(Order.OrderDetails orderDetails) {
        if (orderManager != null) {
            orderManager.addOrderToHistoryTab(orderDetails);
        }
    }

    @Override
    public void editOrder(Order order) {
        Intent intent = new Intent(getActivity(), EditOrderActivity.class);
        intent.putExtra("order", order);
        editOrderActivityResultLauncher.launch(intent);
    }

    @Override
    public void setOrderManager(OrderManager mediator) {
        this.orderManager = mediator;
    }
}