package com.symplified.order.ui.orders.tabs;

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
import androidx.recyclerview.widget.GridLayoutManager;

import com.symplified.order.App;
import com.symplified.order.R;
import com.symplified.order.networking.apis.OrderApi;
import com.symplified.order.databinding.FragmentTablesBinding;
import com.symplified.order.models.qrorders.ConsolidatedOrder;
import com.symplified.order.models.qrorders.ConsolidatedOrdersResponse;
import com.symplified.order.networking.ServiceGenerator;
import com.symplified.order.ui.orders.ConsolidateOrderActivity;
import com.symplified.order.utils.SharedPrefsKey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class UnpaidOrdersFragment extends Fragment implements TablesAdapter.OnTableClickListener {

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
        binding.tablesList.setLayoutManager(new GridLayoutManager(view.getContext(), 4));
        tablesAdapter = new TablesAdapter(this);
        binding.tablesList.setAdapter(tablesAdapter);
        fetchPendingOrders();

        consolidateOrderActivityResultLauncher
                = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.d("consolidate", "UnpaidOrdersFragment result: " + result);
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        ConsolidatedOrder order = (ConsolidatedOrder) data.getSerializableExtra(ConsolidateOrderActivity.CONSOLIDATED_ORDER_KEY);
                        tablesAdapter.removeOrder(order);
                    }
                });
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
                            if (response.status == 200) {
                                pendingOrders.addAll(response.data.content);
                            }
                        }

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
                        Log.d("consolidate", "onComplete");
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
}
