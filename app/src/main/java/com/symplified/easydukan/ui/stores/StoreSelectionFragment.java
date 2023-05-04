package com.symplified.easydukan.ui.stores;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.symplified.easydukan.R;
import com.symplified.easydukan.enums.NavIntentStore;
import com.symplified.easydukan.models.store.Store;
import com.symplified.easydukan.models.store.StoreResponse;
import com.symplified.easydukan.networking.ServiceGenerator;
import com.symplified.easydukan.networking.apis.OrderApi;
import com.symplified.easydukan.networking.apis.StoreApi;
import com.symplified.easydukan.utils.SharedPrefsKey;

import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StoreSelectionFragment extends Fragment
        implements StoreAdapter.StoreSelectionListener {

    public interface StoreFragmentListener {
        void onStoreSelected(Fragment fragment, String storeId);
        void onStoreListReopened();
    }

    private RecyclerView recyclerView;
    private String clientId;
    private NavIntentStore action;
    private List<Store> stores = new ArrayList<>();

    private StoreAdapter storeAdapter;
    private ConstraintLayout progressBarLayout, emptyLayout;
    private RelativeLayout storesLayout;
    private SwipeRefreshLayout refreshLayout;
    private TextView emptyStoresTextView;

    private final String TAG = StoreSelectionFragment.class.getName();

    private OrderApi orderApiService;
    private StoreApi storeApiService;

    private StoreFragmentListener mSelectionListener;

    public StoreSelectionFragment() {
        super(R.layout.fragment_store_selection);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            mSelectionListener.onStoreListReopened();
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mSelectionListener = (StoreFragmentListener) context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        storeApiService = ServiceGenerator.createStoreService(getContext());
        orderApiService = ServiceGenerator.createOrderService(getContext());
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        clientId = requireArguments().getString(SharedPrefsKey.CLIENT_ID);
        action = (NavIntentStore) requireArguments().getSerializable("action");

        // Inflate the layout for this fragment
        recyclerView = view.findViewById(R.id.store_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        progressBarLayout = view.findViewById(R.id.layout_store_progress);
        emptyLayout = view.findViewById(R.id.empty_store_layout);
        storesLayout = view.findViewById(R.id.layout_stores);
        refreshLayout = view.findViewById(R.id.layout_store_refresh);
        emptyStoresTextView = view.findViewById(R.id.text_no_store);
        refreshLayout.setOnRefreshListener(this::getStores);

        getStores();
    }

    private void getStores() {
        startLoading();

        StoreAdapter.StoreSelectionListener listener = this;
        storeApiService.getStores(clientId).clone().enqueue(new Callback<StoreResponse>() {
            @Override
            public void onResponse(@NonNull Call<StoreResponse> call,
                                   @NonNull Response<StoreResponse> response) {
                Log.d("login-activity", call.request().url().toString());
                Log.d("login-activity", "Response:" + response.raw());
                if (response.isSuccessful() && response.body() != null) {
                    stores = response.body().data.content;
                    if (stores.isEmpty()) {
                        stopLoadingWithText(getString(R.string.no_store_msg));
                    } else if (action == NavIntentStore.DISPLAY_QR_CODE) {
                        checkQrCodeAvailabilityForStores();
                    } else {
                        storeAdapter = new StoreAdapter(stores, action, listener, getContext());
                        recyclerView.setAdapter(storeAdapter);
                        storeAdapter.notifyDataSetChanged();
                        stopLoading();
                    }
                } else {
                    stopLoadingWithText(getString(R.string.error_text_pull_to_refresh));
                }
            }

            @Override
            public void onFailure(@NonNull Call<StoreResponse> call, @NonNull Throwable t) {
                stopLoadingWithText(getString(R.string.error_text_pull_to_refresh));
            }
        });
    }

    // TODO: Use RxJava to callback from multiple requests concurrently.
    int storesChecked = 0;

    private void checkQrCodeAvailabilityForStores() {

        storesChecked = 0;
        List<Store> storesWithQrCodeAvailability = new ArrayList<>();
        StoreAdapter.StoreSelectionListener listener = this;
        for (Store store : stores) {
            orderApiService.verifyQrCodeAvailability(store.id).clone().enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(@NonNull Call<ResponseBody> call,
                                       @NonNull Response<ResponseBody> response) {
                    storesChecked++;
                    if (response.isSuccessful()) {
                        storesWithQrCodeAvailability.add(store);
                    }
                    if (storesChecked >= stores.size()) {
                        Log.d("store-selection", "All stores checked");
                        if (!storesWithQrCodeAvailability.isEmpty()) {
                            storeAdapter = new StoreAdapter(storesWithQrCodeAvailability, action, listener, getContext());
                            recyclerView.setAdapter(storeAdapter);
                            storeAdapter.notifyDataSetChanged();
                            stopLoading();
                        } else {
                            stopLoadingWithText(getString(R.string.no_store_with_qr_code_msg));
                        }
                    }
                }

                @Override
                public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                    storesChecked++;
                }
            });
        }
    }

    private void startLoading() {
        refreshLayout.setRefreshing(true);
        storesLayout.setVisibility(View.GONE);
        progressBarLayout.setVisibility(View.VISIBLE);
    }

    private void stopLoading() {
        refreshLayout.setRefreshing(false);
        progressBarLayout.setVisibility(View.GONE);
        storesLayout.setVisibility(View.VISIBLE);
    }

    private void stopLoadingWithText(String message) {
        refreshLayout.setRefreshing(false);
        progressBarLayout.setVisibility(View.GONE);
        emptyStoresTextView.setText(message);

        emptyLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void onStoreSelected(String storeId) {
        mSelectionListener.onStoreSelected(this, storeId);
    }
}