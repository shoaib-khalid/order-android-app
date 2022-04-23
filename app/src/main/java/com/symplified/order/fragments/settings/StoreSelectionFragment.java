package com.symplified.order.fragments.settings;

import static android.content.Context.MODE_PRIVATE;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.symplified.order.App;
import com.symplified.order.R;
import com.symplified.order.adapters.StoreAdapter;
import com.symplified.order.apis.StoreApi;
import com.symplified.order.models.Store.StoreResponse;

import java.util.HashMap;
import java.util.Map;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class StoreSelectionFragment extends Fragment {

    private RecyclerView recyclerView ;
    private TextView chooseStore;
    private TextView noStore;
    private SharedPreferences sharedPreferences;
    private String BASE_URL;
    private Dialog progressDialog;
    private CircularProgressIndicator progressIndicator;
    private StoreAdapter storeAdapter;
    private final String TAG = StoreSelectionFragment.class.getName();
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

        getStores(sharedPreferences);

        if (getArguments() != null) {

        }
    }

    private void getStores(SharedPreferences sharedPreferences) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer Bearer accessToken");

        Retrofit retrofit = new Retrofit.Builder().client(new OkHttpClient()).baseUrl(BASE_URL + App.PRODUCT_SERVICE_URL)
                .addConverterFactory(GsonConverterFactory.create()).build();

        StoreApi storeApiService = retrofit.create(StoreApi.class);
        String clientId = sharedPreferences.getString("ownerId", null);

        if (null == clientId) {
            Log.d("Client-ID", "onCreate: client id is null");
        }
        headers.put("Authorization", "Bearer Bearer accessToken");

        Call<StoreResponse> storeResponse = storeApiService.getStores(headers, clientId);
        progressDialog.show();
        storeResponse.clone().enqueue(new Callback<StoreResponse>() {
            @Override
            public void onResponse(Call<StoreResponse> call, Response<StoreResponse> response) {
                if(response.isSuccessful()){
                    progressDialog.dismiss();
                    storeAdapter = new StoreAdapter(response.body().data.content, getContext(), progressDialog, sharedPreferences);
                    recyclerView.setAdapter(storeAdapter);
                    storeAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<StoreResponse> call, Throwable t) {
                Log.e(TAG, "onFailure: ", t);
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
}