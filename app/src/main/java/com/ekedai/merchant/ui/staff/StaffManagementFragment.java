package com.ekedai.merchant.ui.staff;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.ekedai.merchant.R;
import com.ekedai.merchant.databinding.FragmentStaffManagementBinding;
import com.ekedai.merchant.models.staff.PasswordChangeRequest;
import com.ekedai.merchant.models.staff.RegisterStaffMemberRequest;
import com.ekedai.merchant.models.staff.RegisterStaffMemberResponse;
import com.ekedai.merchant.models.staff.StaffMember;
import com.ekedai.merchant.models.staff.StaffMemberListResponse;
import com.ekedai.merchant.models.store.Store;
import com.ekedai.merchant.models.store.StoreResponse;
import com.ekedai.merchant.networking.ServiceGenerator;
import com.ekedai.merchant.networking.apis.StaffApi;
import com.ekedai.merchant.networking.apis.StoreApi;
import com.ekedai.merchant.utils.SharedPrefsKey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StaffManagementFragment
        extends Fragment
        implements
        AddStaffMemberDialogFragment.OnAddStaffMemberListener,
        StaffAdapter.OnShowPasswordChangeDialogListener,
        ChangePasswordDialog.OnChangePasswordSubmitListener {

    private FragmentStaffManagementBinding binding;
    private StaffApi staffApi;
    private StoreApi storeApi;
    private Store[] stores = new Store[]{};
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
        staffAdapter = new StaffAdapter(this);
        binding.staffMemberList.setAdapter(staffAdapter);
        binding.staffMemberList.setLayoutManager(new LinearLayoutManager(binding.getRoot().getContext()));

        Bundle bundle = requireArguments();
        clientId = bundle.getString(SharedPrefsKey.CLIENT_ID, "");
        staffApi = ServiceGenerator.createStaffService(getActivity().getApplicationContext());
        storeApi = ServiceGenerator.createStoreService(getActivity().getApplicationContext());
//        fetchStoresAndStaffMembers();
        fetchStoresAndStaffMembers();

        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            fetchStoresAndStaffMembers();
        });

        return binding.getRoot();
    }

    private void fetchStoresAndStaffMembers() {
        AddStaffMemberDialogFragment.OnAddStaffMemberListener listener = this;
        storeApi.getStores(clientId)
                .clone()
                .enqueue(new Callback<StoreResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<StoreResponse> call,
                                           @NonNull Response<StoreResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<Store> content = response.body().data.content;
                            stores = new Store[content.size()];
                            content.toArray(stores);

                            Map<String, String> storesMap = new HashMap<>();
                            for (Store store : content) {
                                storesMap.put(store.id, store.name);
                            }
                            staffAdapter.setStores(storesMap);

                            binding.addMemberButton.setVisibility(View.VISIBLE);
                            binding.addMemberButton.setOnClickListener(v ->
                                    new AddStaffMemberDialogFragment(stores, listener)
                                            .show(getChildFragmentManager(), AddStaffMemberDialogFragment.TAG)
                            );
                            if (Build.VERSION.SDK_INT < 26) {
                                binding.addMemberButton.setOnLongClickListener(v -> {
                                    Toast.makeText(binding.getRoot().getContext(), getString(R.string.add_staff_member), Toast.LENGTH_SHORT).show();
                                    return true;
                                });
                            }
                            fetchStaffMembers();
                        } else {
                            Toast.makeText(getContext(), "An error occurred. Please swipe down to retry.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<StoreResponse> call, @NonNull Throwable t) {
                        Toast.makeText(getContext(), "An error occurred. Please swipe down to retry.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @SuppressLint("CheckResult")
    private void fetchStaffMembers() {
        binding.emptyStaffTextView.setVisibility(View.GONE);
        staffAdapter.clear();

        List<Observable<StaffMemberListResponse>> requests = new ArrayList<>();

        for (Store store : stores) {
            requests.add(staffApi.getStaffMembersByStoreIdObservable(store.id));
        }

        Observable<List<StaffMemberListResponse>> observableResult = Observable.zip(requests, objects -> {
            List<StaffMemberListResponse> responses = new ArrayList<>();
            for (Object o : objects) {
                responses.add((StaffMemberListResponse) o);
            }
            return responses;
        }).subscribeOn(Schedulers.newThread());
        observableResult
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new Observer<List<StaffMemberListResponse>>() {
                    @Override
                    public void onSubscribe(Disposable d) {}

                    @Override
                    public void onNext(List<StaffMemberListResponse> staffMemberListResponses) {
                        for (StaffMemberListResponse response : staffMemberListResponses) {
                            if (response.status == 200) {
                                staffAdapter.addStaffMembers(response.data.content);
                            }
                        }
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
                        if (staffAdapter.getItemCount() == 0) {
                            binding.emptyStaffTextView.setVisibility(View.VISIBLE);
                        }
                    }
                });
    }

    private void startLoading() {
        binding.circularProgressBar.setVisibility(View.VISIBLE);
    }

    private void stopLoading() {
        binding.circularProgressBar.setVisibility(View.GONE);
        binding.swipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onStaffMemberAdded(String storeId, String name, String username, String password) {
        binding.horizontalProgressBar.setVisibility(View.VISIBLE);
        staffApi.addStaffMember(
                storeId,
                new RegisterStaffMemberRequest(storeId, name, username, password)
        ).clone().enqueue(new Callback<RegisterStaffMemberResponse>() {
            @Override
            public void onResponse(@NonNull Call<RegisterStaffMemberResponse> call,
                                   @NonNull Response<RegisterStaffMemberResponse> response) {
                binding.horizontalProgressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    StaffMember addedStaffMember = response.body().data;
                    staffAdapter.addStaffMember(addedStaffMember);
                } else {
                    Toast.makeText(getContext(), "Failed to register staff member. Please try again.", Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void onFailure(@NonNull Call<RegisterStaffMemberResponse> call, @NonNull Throwable t) {
                binding.horizontalProgressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Failed to register staff member. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onShowChangePasswordDialog(StaffMember staffMember) {
        new ChangePasswordDialog(staffMember, this)
                .show(getChildFragmentManager(), ChangePasswordDialog.TAG);
    }

    @Override
    public void onChangePasswordSubmitted(StaffMember staffMember, String newPassword) {
        staffAdapter.setLoadingStatus(staffMember, true);

        staffApi.changePassword(staffMember.storeId, staffMember.id, new PasswordChangeRequest(newPassword))
                .clone()
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                        staffAdapter.setLoadingStatus(staffMember, false);

                        if (response.isSuccessful()) {
                            Toast.makeText(getContext(), "Password has been changed.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "Failed to change password. Please try again.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                        staffAdapter.setLoadingStatus(staffMember, false);
                        Toast.makeText(getContext(), "Failed to change password. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}