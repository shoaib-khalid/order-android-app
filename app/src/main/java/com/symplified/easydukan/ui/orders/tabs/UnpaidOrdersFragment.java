package com.symplified.easydukan.ui.orders.tabs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.flexbox.AlignItems;
import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexWrap;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.flexbox.JustifyContent;
import com.symplified.easydukan.App;
import com.symplified.easydukan.R;
import com.symplified.easydukan.databinding.FragmentTablesBinding;
import com.symplified.easydukan.interfaces.OrderManager;
import com.symplified.easydukan.interfaces.OrderObserver;
import com.symplified.easydukan.models.order.Order;
import com.symplified.easydukan.models.qrorders.ConsolidatedOrder;
import com.symplified.easydukan.models.qrorders.ConsolidatedOrdersResponse;
import com.symplified.easydukan.networking.ServiceGenerator;
import com.symplified.easydukan.networking.apis.OrderApi;
import com.symplified.easydukan.services.OrderNotificationService;
import com.symplified.easydukan.ui.orders.ConsolidateOrderActivity;
import com.symplified.easydukan.utils.SharedPrefsKey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class UnpaidOrdersFragment
        extends Fragment
        implements TablesAdapter.OnTableClickListener, OrderObserver {

    FragmentTablesBinding binding;
    private List<String> storeIds;
    private OrderApi orderApiService;
    private TablesAdapter tablesAdapter;
    String testStoreId = "e5bd2d2b-a8f6-429b-8baf-e90bb123f29a";
    private ActivityResultLauncher<Intent> consolidateOrderActivityResultLauncher;

    public UnpaidOrdersFragment() {
        super(R.layout.fragment_tables);
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentTablesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        orderApiService = ServiceGenerator.createOrderService(requireActivity().getApplicationContext());
        SharedPreferences sharedPrefs =
                requireActivity().getSharedPreferences(App.SESSION, Context.MODE_PRIVATE);
        storeIds = new ArrayList<>();
        storeIds.addAll(Arrays.asList(sharedPrefs.getString(SharedPrefsKey.STORE_ID_LIST, "")
                .split(" ")));

        binding.swipeRefreshLayout.setOnRefreshListener(this::fetchPendingOrders);

        FlexboxLayoutManager flexboxLayoutManager =
                new FlexboxLayoutManager(view.getContext(), FlexDirection.ROW, FlexWrap.WRAP);
        flexboxLayoutManager.setJustifyContent(JustifyContent.SPACE_EVENLY);
        flexboxLayoutManager.setAlignItems(AlignItems.CENTER);
        binding.tablesList.setLayoutManager(flexboxLayoutManager);
        tablesAdapter = new TablesAdapter(this);
        binding.tablesList.setAdapter(tablesAdapter);
        fetchPendingOrders();

        consolidateOrderActivityResultLauncher
                = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        ConsolidatedOrder order = (ConsolidatedOrder) data
                                .getSerializableExtra(ConsolidateOrderActivity.CONSOLIDATED_ORDER_KEY);
                        tablesAdapter.removeOrder(order);
                    }
                });

        OrderNotificationService.addNewOrderObserver(this);
        OrderNotificationService.addOngoingOrderObserver(this);
        OrderNotificationService.addPastOrderObserver(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        OrderNotificationService.removeNewOrderObserver(this);
        OrderNotificationService.removeOngoingOrderObserver(this);
        OrderNotificationService.removePastOrderObserver(this);
    }


    @SuppressLint("CheckResult")
    private void fetchPendingOrders() {
        List<Observable<ConsolidatedOrdersResponse>> requests = new ArrayList<>();
        for (String storeId: storeIds) {
            requests.add(orderApiService.getPendingConsolidatedOrders(storeId));
        }
        Observable.zip(requests, objects -> {
            List<ConsolidatedOrdersResponse> responses = new ArrayList<>();
            for (Object o : objects) {
                responses.add((ConsolidatedOrdersResponse) o);
            }
            return responses;
        }).subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new Observer<List<ConsolidatedOrdersResponse>>() {
                    @Override
                    public void onSubscribe(Disposable d) {}

                    @Override
                    public void onNext(List<ConsolidatedOrdersResponse> consolidatedOrdersResponses) {
                        List<ConsolidatedOrder> pendingOrders = new ArrayList<>();
                        for (ConsolidatedOrdersResponse response : consolidatedOrdersResponses) {
                            Log.d("consolidate-orders", "Response status: " + response.status);
                            if (response.status == 200) {
                                pendingOrders.addAll(response.data.content);
                            }
                        }
                        Log.d("consolidate-orders", "Pending orders size: " + pendingOrders.size());

                        Collections.sort(pendingOrders, (o1, o2) -> {
                            try {
                                return Integer.parseInt(o1.tableNo) - Integer.parseInt(o2.tableNo);
                            } catch (NumberFormatException ignored) {}
                            return o1.tableNo.compareTo(o2.tableNo);
                        });

                        tablesAdapter.setOrders(pendingOrders);
                    }

                    @Override
                    public void onError(Throwable e) {
                        stopLoading();
                        Toast.makeText(
                                getContext(),
                                "An error occurred. Please swipe down to retry.",
                                Toast.LENGTH_SHORT
                        ).show();
                    }

                    @Override
                    public void onComplete() {
                        stopLoading();
                    }
                });
    }

    private void stopLoading() {
        binding.progressBar.setVisibility(View.GONE);
        binding.swipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onTableClicked(ConsolidatedOrder order) {
        Log.d("consolidate", "onTableClicked: " + order.tableNo);

        Intent intent = new Intent(getActivity(), ConsolidateOrderActivity.class);
        intent.putExtra(ConsolidateOrderActivity.CONSOLIDATED_ORDER_KEY, order);
        consolidateOrderActivityResultLauncher.launch(intent);
    }

    @Override
    public void onOrderReceived(Order.OrderDetails orderDetails) {
        fetchPendingOrders();
    }

    @Override
    public void setOrderManager(OrderManager orderManager) {}
}