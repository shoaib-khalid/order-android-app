package com.symplified.order.fragments.settings;

import static android.content.Context.MODE_PRIVATE;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.symplified.order.App;
import com.symplified.order.R;
import com.symplified.order.adapters.StoreAdapter;
import com.symplified.order.apis.StoreApi;
import com.symplified.order.models.store.StoreResponse;
import com.symplified.order.networking.ServiceGenerator;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StoreSelectionFragment extends Fragment {

    private RecyclerView recyclerView ;
    private TextView chooseStore;
    private TextView noStore;
    private SharedPreferences sharedPreferences;
    private String BASE_URL;
    private Dialog progressDialog;
    private CircularProgressIndicator progressIndicator;
    private StoreAdapter storeAdapter;
    private ConstraintLayout progressBarLayout;
    private RelativeLayout storesLayout;
    private SwipeRefreshLayout refreshLayout;
    private final String TAG = StoreSelectionFragment.class.getName();
    private StoreApi storeApiService;

    public StoreSelectionFragment() {
        // Required empty public constructor
    }

    public static StoreSelectionFragment newInstance() {
        StoreSelectionFragment fragment = new StoreSelectionFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = getActivity().getSharedPreferences(App.SESSION_DETAILS_TITLE, MODE_PRIVATE);
        BASE_URL = sharedPreferences.getString("base_url", App.BASE_URL);

        progressDialog = new Dialog(getActivity(), R.style.Theme_SymplifiedOrderUpdate);
        progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        progressDialog.setContentView(R.layout.progress_dialog);
        progressIndicator = progressDialog.findViewById(R.id.progress);
        progressIndicator.setIndeterminate(true);
        if(sharedPreferences.getBoolean("isStaging", false))
        {
            progressIndicator.setIndicatorColor(getContext().getResources().getColor(R.color.sf_b_800, getContext().getTheme()));
        }
        storeApiService = ServiceGenerator.createStoreService();
    }

    private void getStores(SharedPreferences sharedPreferences) {
        startLoading();

        String clientId = sharedPreferences.getString("ownerId", null);

        Call<StoreResponse> storeResponse = storeApiService.getStores(clientId);
        storeResponse.clone().enqueue(new Callback<StoreResponse>() {
            @Override
            public void onResponse(Call<StoreResponse> call, Response<StoreResponse> response) {
                if(response.isSuccessful()){
                    progressDialog.dismiss();
                    storeAdapter = new StoreAdapter(response.body().data.content, getContext(), progressDialog, sharedPreferences);
                    recyclerView.setAdapter(storeAdapter);
                    storeAdapter.notifyDataSetChanged();
                    stopLoading();
                }
            }

            @Override
            public void onFailure(Call<StoreResponse> call, Throwable t) {
                Log.e(TAG, "onFailure: ", t);
                stopLoading();
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_store_selection, container, false);
        recyclerView = view.findViewById(R.id.store_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        chooseStore = view.findViewById(R.id.choose_store);
        noStore = view.findViewById(R.id.no_store);

        progressBarLayout = view.findViewById(R.id.layout_store_progress);
        storesLayout = view.findViewById(R.id.layout_stores);
        refreshLayout = view.findViewById(R.id.layout_store_refresh);
        refreshLayout.setOnRefreshListener(() -> getStores(sharedPreferences));

        getStores(sharedPreferences);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        getStores(sharedPreferences);
    }

    @Override
    public void onResume() {
        super.onResume();
        getStores(sharedPreferences);
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
}