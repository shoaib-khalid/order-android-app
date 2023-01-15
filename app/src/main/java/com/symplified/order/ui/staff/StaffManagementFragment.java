package com.symplified.order.ui.staff;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.symplified.order.R;
import com.symplified.order.adapters.StaffAdapter;
import com.symplified.order.apis.StaffApi;
import com.symplified.order.apis.StoreApi;
import com.symplified.order.databinding.FragmentStaffManagementBinding;
import com.symplified.order.models.staff.StaffMember;
import com.symplified.order.models.staff.StaffMemberListResponse;
import com.symplified.order.models.store.Store;
import com.symplified.order.models.store.StoreResponse;
import com.symplified.order.networking.ServiceGenerator;
import com.symplified.order.utils.SharedPrefsKey;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StaffManagementFragment
        extends Fragment
        implements AddStaffMemberDialogFragment.OnAddStaffMemberListener {

    private FragmentStaffManagementBinding binding;
    private StaffApi staffApi;
    private StoreApi storeApi;
    private String[] storeIds;
    private String clientId;
    private StaffAdapter staffAdapter;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentStaffManagementBinding.inflate(
                inflater,
                container,
                false
        );
        staffAdapter = new StaffAdapter();
        binding.staffMemberList.setAdapter(staffAdapter);
        binding.staffMemberList.setLayoutManager(new LinearLayoutManager(binding.getRoot().getContext()));

        Bundle bundle = requireArguments();
        clientId = bundle.getString(SharedPrefsKey.CLIENT_ID, "");
        storeIds = bundle.getString(SharedPrefsKey.STORE_ID_LIST, "").split(" ");
        staffApi = ServiceGenerator.createStaffService(getActivity().getApplicationContext());
        storeApi = ServiceGenerator.createStoreService(getActivity().getApplicationContext());
        fetchStores();
        fetchStaffMembers();

        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            fetchStores();
            fetchStaffMembers();
        });

        return binding.getRoot();
    }

    private void fetchStores() {
        AddStaffMemberDialogFragment.OnAddStaffMemberListener listener = this;
        storeApi.getStores(clientId)
                .clone()
                .enqueue(new Callback<StoreResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<StoreResponse> call,
                                           @NonNull Response<StoreResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<Store> content = response.body().data.content;
                            Store[] stores = new Store[content.size()];
                            for (int i = 0; i < content.size(); i++) {
                                stores[i] = content.get(i);
                            }
                            binding.addMemberButton.setVisibility(View.VISIBLE);
                            binding.addMemberButton.setOnLongClickListener(v -> {
                                Toast.makeText(binding.getRoot().getContext(), getString(R.string.add_staff_member), Toast.LENGTH_SHORT).show();
                                return true;
                            });
                            binding.addMemberButton.setOnClickListener(v ->
                                    new AddStaffMemberDialogFragment(stores, listener)
                                            .show(getChildFragmentManager(), AddStaffMemberDialogFragment.TAG)
                            );
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<StoreResponse> call, @NonNull Throwable t) {

                    }
                });
    }

    private void fetchStaffMembers() {
        staffAdapter.clear();
        startLoading();

        for (String storeId : storeIds) {
            staffApi.getStaffMembersByStoreId(storeId)
                    .clone()
                    .enqueue(new Callback<StaffMemberListResponse>() {
                        @Override
                        public void onResponse(
                                @NonNull Call<StaffMemberListResponse> call,
                                @NonNull Response<StaffMemberListResponse> response
                        ) {
                            stopLoading();
                            Log.d("staff", response.raw().toString());
                            if (response.isSuccessful() && response.body() != null) {
                                staffAdapter.addStaffMembers(response.body().data.content);
                            }
                        }

                        @Override
                        public void onFailure(
                                @NonNull Call<StaffMemberListResponse> call,
                                @NonNull Throwable t
                        ) {
                            stopLoading();
                            Toast.makeText(getContext(), "An error occurred. Please try again.", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void startLoading() {
        binding.progressBar.setVisibility(View.VISIBLE);
    }

    private void stopLoading() {
        binding.progressBar.setVisibility(View.GONE);
        binding.swipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onStaffMemberAdded(String name, String username, String password) {
//        staffApi.addStaffMember(
//
//        )
    }
}