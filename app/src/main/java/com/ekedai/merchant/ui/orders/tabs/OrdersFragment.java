package com.ekedai.merchant.ui.orders.tabs;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.ekedai.merchant.App;
import com.ekedai.merchant.R;
import com.ekedai.merchant.databinding.FragmentOrdersBinding;
import com.ekedai.merchant.models.interfaces.OrderManager;
import com.ekedai.merchant.models.interfaces.OrderObserver;
import com.ekedai.merchant.models.interfaces.Printer;
import com.ekedai.merchant.models.interfaces.PrinterObserver;
import com.ekedai.merchant.models.order.Order;
import com.ekedai.merchant.models.order.OrderDetailsResponse;
import com.ekedai.merchant.networking.ServiceGenerator;
import com.ekedai.merchant.networking.apis.OrderApi;
import com.ekedai.merchant.services.OrderNotificationService;
import com.ekedai.merchant.ui.orders.EditOrderActivity;
import com.ekedai.merchant.ui.orders.OrderAdapter;
import com.ekedai.merchant.utils.SharedPrefsKey;
import com.ekedai.merchant.utils.SunmiPrintHelper;
import com.ekedai.merchant.utils.Utility;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * A placeholder fragment containing a simple view.
 */
public class OrdersFragment extends Fragment
        implements PrinterObserver, OrderObserver, OrderManager {

    private static final String ARG_SECTION = "section";

    private final Handler handler = new Handler();
    private Boolean isActive = false;

    private OrderAdapter orderAdapter;

    private List<Order.OrderDetails> orders = new ArrayList<>();

    private Call<OrderDetailsResponse> orderResponse;
    private RecyclerView recyclerView;
    private String section;

    private ProgressBar progressBar;
    private SwipeRefreshLayout mainLayout, emptyLayout;
    private TextView emptyOrdersTextView;
    private OrderManager orderManager;

    private ActivityResultLauncher<Intent> editOrderActivityResultLauncher;

    public static OrdersFragment newInstance(String type) {
        OrdersFragment fragment = new OrdersFragment();
        Bundle bundle = new Bundle();
        bundle.putString(ARG_SECTION, type);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        String clientId = requireActivity().getSharedPreferences(App.SESSION, Context.MODE_PRIVATE)
                .getString(SharedPrefsKey.CLIENT_ID, null);

        orders = new ArrayList<>();

        OrderApi orderApiService = ServiceGenerator.createOrderService(getContext());

        section = requireArguments().getString(ARG_SECTION);

        switch (section) {
            case "new": {
                orderResponse = orderApiService.searchNewOrdersByClientId(clientId);
                OrderNotificationService.addNewOrderObserver(this);
                break;
            }
            case "ongoing": {
                orderResponse = orderApiService.searchOngoingOrdersByClientId(clientId);
                OrderNotificationService.addOngoingOrderObserver(this);
                break;
            }
            case "past": {
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

                String currentDate = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O
                ? LocalDate.now(TimeZone.getDefault().toZoneId()).toString()
                : formatter.format(new Date());

                orderResponse = orderApiService.searchSentOrdersByClientId(clientId, currentDate, currentDate);
                OrderNotificationService.addPastOrderObserver(this);

                break;
            }
        }

        editOrderActivityResultLauncher
                = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK
                            && orderAdapter != null
                            && result.getData() != null) {
                        Intent data = result.getData();
                        Order.OrderDetails updatedOrderDetails
                                = (Order.OrderDetails) data.getSerializableExtra(SharedPrefsKey.ORDER_DETAILS);
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

        SunmiPrintHelper.getInstance().addObserver(this);
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {

        FragmentOrdersBinding binding = FragmentOrdersBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        recyclerView = binding.orderRecycler;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        progressBar = binding.ordersProgressBar;
        emptyOrdersTextView = binding.emptyOrdersTextView;

        mainLayout = binding.fullSwipeRefreshLayout;
        emptyLayout = binding.emptySwipeRefreshLayout;

        mainLayout.setOnRefreshListener(this::getOrders);
        emptyLayout.setOnRefreshListener(this::getOrders);

        getOrders();

        if (section.equals("past")) {
            isActive = true;
            setClearHistoryHandler();
        }

        return root;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        OrderNotificationService.removeNewOrderObserver(this);
        OrderNotificationService.removeOngoingOrderObserver(this);
        OrderNotificationService.removePastOrderObserver(this);
        isActive = false;
        handler.removeCallbacksAndMessages(null);
    }

    private void getOrders() {
        OrderManager orderManager = this;
        startLoading();
        orderResponse.clone().enqueue(new Callback<OrderDetailsResponse>() {
            @Override
            public void onResponse(@NonNull Call<OrderDetailsResponse> call,
                                   @NonNull Response<OrderDetailsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    orders = response.body().data.content;
                    orderAdapter = new OrderAdapter(orders, section, requireContext(), orderManager);
                    recyclerView.setAdapter(orderAdapter);
                    orderAdapter.notifyDataSetChanged();
                    if (orders.size() > 0) {
                        showOrders();
                    } else {
                        showEmptyOrdersMessage();
                    }
                } else {
                    Log.e("placeholder-fragment", response.raw().toString());
                    showErrorMessage();
                }
                stopLoading();
            }

            @Override
            public void onFailure(@NonNull Call<OrderDetailsResponse> call, @NonNull Throwable t) {
                Log.e("placeholder-fragment", "onFailure: " + t.getLocalizedMessage());
                stopLoading();
                showErrorMessage();
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
    public void onOrderReceived(Order.OrderDetails orderDetails) {
        if (orderAdapter != null) {
            if (Utility.isOrderCompleted(orderDetails.currentCompletionStatus)) {
                orders.add(0, orderDetails);
                orderAdapter.notifyItemInserted(0);
                recyclerView.smoothScrollToPosition(0);
            } else {
                orders.add(orderDetails);
                orderAdapter.notifyItemInserted(orders.indexOf(orderDetails));
            }
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

    @Override
    public void onPrinterConnected(Printer printer) {
        if (orderAdapter != null) {
            orderAdapter.notifyDataSetChanged();
        }
    }

    private void setClearHistoryHandler() {
        Calendar currentTime = Calendar.getInstance();
        Calendar midnight = Calendar.getInstance();
        midnight.add(Calendar.DAY_OF_MONTH, 1);
        midnight.set(Calendar.HOUR_OF_DAY, 0);
        midnight.set(Calendar.MINUTE, 0);
        midnight.set(Calendar.SECOND, 0);
        long initialDelay =  midnight.getTimeInMillis() - currentTime.getTimeInMillis();

        handler.postDelayed(clearHistoryTask, initialDelay);
    }

    // Clears orders
    private final Runnable clearHistoryTask = new Runnable() {
        @Override
        public void run() {
            if (isActive) {
                orderAdapter.clear();
                handler.postDelayed(this, 86400000);
            }
        }
    };
}