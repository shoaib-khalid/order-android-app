package com.symplified.order.ui.staff;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.symplified.order.App;
import com.symplified.order.R;
import com.symplified.order.apis.StaffApi;
import com.symplified.order.apis.StoreApi;
import com.symplified.order.databinding.FragmentShiftManagementBinding;
import com.symplified.order.models.staff.StaffMember;
import com.symplified.order.models.staff.StaffMemberListResponse;
import com.symplified.order.models.staff.shift.EndShiftRequest;
import com.symplified.order.models.staff.shift.SummaryDetails;
import com.symplified.order.models.staff.shift.SummaryDetailsResponse;
import com.symplified.order.models.store.Store;
import com.symplified.order.models.store.StoreResponse;
import com.symplified.order.networking.ServiceGenerator;
import com.symplified.order.utils.SharedPrefsKey;
import com.symplified.order.utils.Utility;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ShiftManagementFragment extends Fragment {

    private FragmentShiftManagementBinding binding;
    private StaffApi staffApi;
    private StoreApi storeApi;
    private String clientId;
    private SalesAdapter salesAdapter;
    private String currency = "RM";
    private StaffMember currentStaffMember;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentShiftManagementBinding.inflate(
                inflater, container, false
        );
        salesAdapter = new SalesAdapter();
        binding.salesList.setAdapter(salesAdapter);
        binding.salesList.setLayoutManager(new LinearLayoutManager(binding.getRoot().getContext()));

        staffApi = ServiceGenerator.createStaffService(getActivity().getApplicationContext());
        storeApi = ServiceGenerator.createStoreService(getActivity().getApplicationContext());

        Bundle bundle = requireArguments();
        clientId = bundle.getString(SharedPrefsKey.CLIENT_ID, "");
        fetchStoresAndStaffMembers();

        SimpleDateFormat dateFormatter
                = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        binding.dateTextView.setText(dateFormatter.format(new Date()));

        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            if (currentStaffMember != null) {
                fetchSalesData(currentStaffMember);
            } else {
                stopLoading();
                Toast.makeText(getContext(), "No staff member selected", Toast.LENGTH_SHORT).show();
            }
        });

        return binding.getRoot();
    }

    private void fetchStoresAndStaffMembers() {
        storeApi.getStores(clientId)
                .clone()
                .enqueue(new Callback<StoreResponse>() {
                    @Override
                    public void onResponse(
                            @NonNull Call<StoreResponse> call,
                            @NonNull Response<StoreResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<Store> stores = response.body().data.content;
                            fetchStaffMembers(stores);
                            for (Store store : stores) {
                                currency = store.regionCountry.currencySymbol;
                                salesAdapter.setCurrency(currency);
                                break;
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<StoreResponse> call, @NonNull Throwable t) {
                        Toast.makeText(getContext(), "An error occurred. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    @SuppressLint("CheckResult")
    private void fetchStaffMembers(List<Store> stores) {

        List<Observable<StaffMemberListResponse>> requests = new ArrayList<>();

        for (Store store : stores) {
            requests.add(staffApi.getStaffMembersByStoreIdObservable(store.id));
        }

        Observable.zip(requests, objects -> {
                    List<StaffMemberListResponse> responses = new ArrayList<>();
                    for (Object o : objects) {
                        responses.add((StaffMemberListResponse) o);
                    }
                    return responses;
                }).subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new Observer<List<StaffMemberListResponse>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                    }

                    @Override
                    public void onNext(List<StaffMemberListResponse> staffMemberListResponses) {
                        List<StaffMember> staffMembersList = new ArrayList<>();
                        for (StaffMemberListResponse response : staffMemberListResponses) {
                            if (response.status == 200) {
                                staffMembersList.addAll(response.data.content);
                            }
                        }
                        if (staffMembersList.isEmpty()) {
                            if (getContext() != null) {
                                binding.emptySalesTextView.setText(getContext().getApplicationContext().getString(R.string.empty_staff_text));
                            }
                            binding.emptySalesTextView.setVisibility(View.VISIBLE);
                        }
                        StaffMember[] staffMembers = new StaffMember[staffMembersList.size()];
                        staffMembersList.toArray(staffMembers);
                        StaffSpinnerAdapter adapter = new StaffSpinnerAdapter(getContext(), android.R.layout.simple_spinner_dropdown_item, staffMembers);
                        binding.staffSpinner.setAdapter(adapter);
                        binding.staffSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                currentStaffMember = adapter.getItem(position);
                                if (currentStaffMember != null) {
                                    fetchSalesData(currentStaffMember);
                                }
//                                Toast.makeText(getContext(), "selected " + adapter.getItem(position).name, Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onNothingSelected(AdapterView<?> parent) {
                            }
                        });
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

    private void fetchSalesData(StaffMember selectedStaffMember) {
        salesAdapter.clear();
        startLoading();
        staffApi.getShiftSummary(selectedStaffMember.storeId, selectedStaffMember.id)
                .clone()
                .enqueue(new Callback<SummaryDetailsResponse>() {
                    @Override
                    public void onResponse(
                            @NonNull Call<SummaryDetailsResponse> call,
                            @NonNull Response<SummaryDetailsResponse> response) {
                        stopLoading();

                        Double totalSales = 0.0;

                        if (response.isSuccessful()) {
                            if (response.body() != null
                                    && response.body().data != null
                                    && response.body().data.summaryDetails != null
                                    && !response.body().data.summaryDetails.isEmpty()) {
                                List<SummaryDetails> summaryDetails = response.body().data.summaryDetails;
                                salesAdapter.setSummaryDetails(summaryDetails);

                                for (SummaryDetails summaryDetail : summaryDetails) {
                                    totalSales += summaryDetail.saleAmount;
                                }

                                binding.endShiftButton.setOnClickListener(v -> {
                                    if (App.isPrinterConnected()) {
                                        App.getPrinter().printSalesSummary(selectedStaffMember, summaryDetails, currency);
                                    }
                                    endShift(selectedStaffMember);
                                });
                                binding.endShiftButton.setEnabled(true);
                            } else {
                                binding.emptySalesTextView.setVisibility(View.VISIBLE);
                                binding.endShiftButton.setEnabled(false);
                            }
                        } else {
                            Toast.makeText(getContext(), "An error occurred. Please try again.", Toast.LENGTH_SHORT).show();
                        }
                        binding.totalSalesTextView.setText(currency
                                + Utility.getMonetaryAmountFormat().format(totalSales));
                    }

                    @Override
                    public void onFailure(@NonNull Call<SummaryDetailsResponse> call, @NonNull Throwable t) {
                        stopLoading();
                        Toast.makeText(getContext(), "An error occurred. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void endShift(StaffMember selectedStaffMember) {
        startLoading();

        staffApi.endShift(selectedStaffMember.storeId, new EndShiftRequest(selectedStaffMember.id))
                .clone()
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(
                            @NonNull Call<Void> call,
                            @NonNull Response<Void> response) {

                        stopLoading();
                        if (response.isSuccessful()) {
                            salesAdapter.clear();
                        } else {
                            if (currentStaffMember.id.equals(selectedStaffMember.id)) {
                                binding.endShiftButton.setEnabled(true);
                            }
                            Toast.makeText(getContext(), "Failed to end shift for " + selectedStaffMember.name + ". Please try again.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                        stopLoading();
                        if (currentStaffMember.id.equals(selectedStaffMember.id)) {
                            binding.endShiftButton.setEnabled(true);
                        }
                        Toast.makeText(getContext(), "Failed to end shift. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void startLoading() {
        binding.endShiftButton.setEnabled(false);
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.emptySalesTextView.setVisibility(View.GONE);
    }

    private void stopLoading() {
        binding.progressBar.setVisibility(View.GONE);
        binding.swipeRefreshLayout.setRefreshing(false);
    }

    private static class StaffSpinnerAdapter extends ArrayAdapter<StaffMember> {
        private final StaffMember[] staffMembers;

        public StaffSpinnerAdapter(
                Context context,
                int textViewResourceId,
                StaffMember[] staffMembers
        ) {
            super(context, textViewResourceId, staffMembers);
            this.staffMembers = staffMembers;
        }

        @Override
        public int getCount() {
            return staffMembers.length;
        }

        @Nullable
        @Override
        public StaffMember getItem(int position) {
            return staffMembers[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            TextView label = (TextView) super.getView(position, convertView, parent);
            label.setText(staffMembers[position].name);
            return label;
        }

        @Override
        public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            TextView label = (TextView) super.getDropDownView(position, convertView, parent);
            label.setText(staffMembers[position].name);
            return label;
        }
    }
}