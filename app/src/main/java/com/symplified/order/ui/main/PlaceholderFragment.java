package com.symplified.order.ui.main;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.symplified.order.App;
import com.symplified.order.R;
import com.symplified.order.adapters.OrderAdapter;
import com.symplified.order.apis.OrderApi;
import com.symplified.order.apis.StoreApi;
import com.symplified.order.databinding.NewOrdersBinding;
import com.symplified.order.models.OrderDetailsModel;
import com.symplified.order.models.Store.StoreResponse;
import com.symplified.order.models.order.Order;
import com.symplified.order.models.order.OrderResponse;
import com.symplified.order.services.AlertService;
import com.symplified.order.services.DateParser;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
/**
 * A placeholder fragment containing a simple view.
 */
public class PlaceholderFragment extends Fragment {

    private static final String ARG_SECTION = "section";

    private PageViewModel pageViewModel;
    private NewOrdersBinding binding;
    private OrderAdapter orderAdapter;

    private Retrofit retrofit;
    private List<OrderDetailsModel> orders;

    private Map<String, String> headers;
    private OrderApi orderApiService;
    private Call<ResponseBody> orderResponse;
    private String storeId;
    private RecyclerView recyclerView;
    private String section;
    private Dialog progressDialog;
//    private FirebaseRemoteConfig mRemoteConfig;
    String BASE_URL;

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
        progressDialog.setContentView(R.layout.progress_dialog);
        progressDialog.setCancelable(false);
        CircularProgressIndicator progressIndicator = progressDialog.findViewById(R.id.progress);
        progressIndicator.setIndeterminate(true);

        SharedPreferences sharedPreferences = this.getActivity().getSharedPreferences(App.SESSION_DETAILS_TITLE, Context.MODE_PRIVATE);
        storeId = sharedPreferences.getString("storeId", null);

        String clientId = sharedPreferences.getString("ownerId", null);

        if(clientId == null)
            Toast.makeText(getActivity(), "Client id is null", Toast.LENGTH_SHORT).show();

        Log.e("CHECKCLIENTID", "onCreate: clientId = " + clientId, new Error() );

        BASE_URL = sharedPreferences.getString("base_url", App.BASE_URL);

        retrofit = new Retrofit.Builder().client(new OkHttpClient()).baseUrl(BASE_URL+App.ORDER_SERVICE_URL)
                .addConverterFactory(GsonConverterFactory.create()).build();

        orders = new ArrayList<>();

        headers = new HashMap<>();
        headers.put("Authorization", "Bearer Bearer accessToken");

        orderApiService = retrofit.create(OrderApi.class);



//        String storeId = "McD";
        Log.e("TAG", "onCreate: "+"storeId : "+storeId, new Error() );


        section = null;
        if (getArguments() != null) {
            section = getArguments().getString(ARG_SECTION);
        }

//        if(section == null)
//            Toast.makeText(getContext(), "Section is null", Toast.LENGTH_SHORT).show();

        pageViewModel.setIndex(0);


        switch (section){
            case "processed":
            {
                pageViewModel.setIndex(1);
                orderResponse = orderApiService.getProcessedOrdersByClientId(headers, clientId);
                break;
            }
            case "sent":{
                pageViewModel.setIndex(2);
                orderResponse = orderApiService.getSentOrdersByClientId(headers, clientId);
                break;
            }
            case "new" :{
                pageViewModel.setIndex(0);
                orderResponse = orderApiService.getNewOrdersByClientId(headers, clientId);
                if(AlertService.isPlaying()){
                    getActivity().stopService(new Intent(getContext(), AlertService.class));
                }
                NotificationManager notificationManager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.cancelAll();
                break;
            }
        }
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        binding = NewOrdersBinding.inflate(inflater, container, false);
        View root = binding.getRoot();


        recyclerView = root.findViewById(R.id.order_recycler);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        DividerItemDecoration mDividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(mDividerItemDecoration);

//        retrofit = new Retrofit.Builder().baseUrl(App.ORDER_SERVICE_URL)
//                .addConverterFactory(GsonConverterFactory.create()).build();

//        OrderApi orderApiService = retrofit.create(OrderApi.class);
//        SharedPreferences sharedPreferences = this.getActivity().getSharedPreferences(App.SESSION_DETAILS_TITLE, Context.MODE_PRIVATE);
//        String storeId = sharedPreferences.getString("ownerId", null);
//
//        if(null == storeId)
//        {
//            Toast.makeText(this.getActivity(), "Client id is null", Toast.LENGTH_SHORT).show();
//        }

//        List<OrderDetailsModel> orders = new ArrayList<>();
//
//        Map<String, String> headers = new HashMap<>();
//        headers.put("Authorization", "Bearer Bearer accessToken");

//        Call<ResponseBody> orderResponse = orderApiService.getNewOrders(headers, storeId);


        Log.e("TAG", "URL : "+orderResponse.request().url(), new Error() );

        progressDialog.show();
        orderResponse.clone().enqueue(new Callback<ResponseBody>() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {



                if(response.isSuccessful())
                {
                    try {
                        OrderResponse orderResponse = new Gson().fromJson(response.body().string(), OrderResponse.class);
                        orderAdapter = new OrderAdapter(orderResponse.data.content, section, getActivity());
                        recyclerView.setAdapter(orderAdapter);
                        orderAdapter.notifyDataSetChanged();
                        progressDialog.hide();
                        Log.e("TAG", "Size: "+ orderResponse.data.content.size(),  new Error());

                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                }

            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(getActivity(), "Failed to fetch orders, ", Toast.LENGTH_SHORT).show();
                Log.e("TAG", "onFailure: ",t.getCause() );
                progressDialog.hide();
            }
        });




//        RecyclerView recyclerView = root.findViewById(R.id.order_recycler);
//
//        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
//        recyclerView.setAdapter(orderAdapter);

//        final TextView textView = binding.sectionLabel;
//        pageViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
//            @Override
//            public void onChanged(@Nullable String s) {
//                textView.setText(s);
//            }
//        });
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        progressDialog.show();
        getActivity().getSupportFragmentManager().beginTransaction().detach(this).attach(this).commit();
        orderResponse.clone().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                if(response.isSuccessful())
                {
                    try {
//                        Toast.makeText(getActivity(), "onResumeCalled", Toast.LENGTH_SHORT).show();
                        OrderResponse orderResponse = new Gson().fromJson(response.body().string(), OrderResponse.class);
                        orderAdapter = new OrderAdapter(orderResponse.data.content, section, getActivity());
                        recyclerView.setAdapter(orderAdapter);
                        orderAdapter.notifyDataSetChanged();
                        progressDialog.hide();
                        Log.e("TAG", "Size: "+ orderResponse.data.content.size(),  new Error());

                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                }

            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                progressDialog.hide();
            }
        });

        if(AlertService.isPlaying()){
            getActivity().stopService(new Intent(getContext(), AlertService.class));
        }
        NotificationManager notificationManager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();

    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}