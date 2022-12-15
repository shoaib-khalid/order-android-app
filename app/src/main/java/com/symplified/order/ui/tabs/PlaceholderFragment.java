package com.symplified.order.ui.tabs;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.symplified.order.App;
import com.symplified.order.EditOrderActivity;
import com.symplified.order.R;
import com.symplified.order.adapters.OrderAdapter;
import com.symplified.order.apis.OrderApi;
import com.symplified.order.databinding.NewOrdersBinding;
import com.symplified.order.helpers.GenericPrintHelper;
import com.symplified.order.helpers.SunmiPrintHelper;
import com.symplified.order.interfaces.OrderManager;
import com.symplified.order.interfaces.OrderObserver;
import com.symplified.order.interfaces.Printer;
import com.symplified.order.interfaces.PrinterObserver;
import com.symplified.order.models.order.Order;
import com.symplified.order.models.order.OrderDetailsResponse;
import com.symplified.order.networking.ServiceGenerator;
import com.symplified.order.services.AlertService;
import com.symplified.order.services.OrderNotificationService;
import com.symplified.order.utils.Key;
import com.symplified.order.utils.Utility;

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

        PageViewModel pageViewModel = new ViewModelProvider(this).get(PageViewModel.class);

        String clientId = getActivity().getSharedPreferences(App.SESSION, Context.MODE_PRIVATE)
                .getString("ownerId", null);

        orders = new ArrayList<>();

        OrderApi orderApiService = ServiceGenerator.createOrderService(getContext());

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

        editOrderActivityResultLauncher
                = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK
                            && orderAdapter != null
                            && result.getData() != null) {
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

        SunmiPrintHelper.getInstance().addObserver(this);
        GenericPrintHelper.getInstance().addObserver(this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        com.symplified.order.databinding.NewOrdersBinding binding = NewOrdersBinding.inflate(inflater, container, false);
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
    public void onDestroy() {
        super.onDestroy();
        OrderNotificationService.removeNewOrderObserver(this);
        OrderNotificationService.removeOngoingOrderObserver(this);
        OrderNotificationService.removePastOrderObserver(this);
    }

    private void getOrders() {
        OrderManager orderManager = this;
        startLoading();
        orderResponse.clone().enqueue(new Callback<OrderDetailsResponse>() {
            @Override
            public void onResponse(@NonNull Call<OrderDetailsResponse> call,
                                   @NonNull Response<OrderDetailsResponse> response) {
                if (response.isSuccessful()) {
                    orders = response.body().data.content;
                    orderAdapter = new OrderAdapter(orders, section, getContext(), orderManager);
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
}