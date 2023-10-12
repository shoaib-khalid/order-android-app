package com.ekedai.merchant.ui.voucher.tabs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.ekedai.merchant.App;
import com.ekedai.merchant.R;
import com.ekedai.merchant.databinding.FragmentVoucherHistoryBinding;
import com.ekedai.merchant.models.voucher.VoucherDetails;
import com.ekedai.merchant.models.voucher.VoucherHistoryResponse;
import com.ekedai.merchant.networking.ServiceGenerator;
import com.ekedai.merchant.networking.apis.ProductApi;
import com.ekedai.merchant.ui.voucher.SearchViewModel;
import com.ekedai.merchant.ui.voucher.VoucherAdapter;
import com.ekedai.merchant.ui.voucher.VoucherDetailsDialog;
import com.ekedai.merchant.ui.voucher.VoucherSuccessDialog;
import com.ekedai.merchant.utils.SharedPrefsKey;
import com.ekedai.merchant.utils.Utilities;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class VoucherHistoryFragment
        extends Fragment
        implements VoucherAdapter.OnVoucherClickListener {

    private FragmentVoucherHistoryBinding binding;

    private final List<VoucherDetails> voucherHistory = new ArrayList<>();
    private List<VoucherDetails> filteredVoucherHistory = new ArrayList<>();
    private VoucherAdapter voucherAdapter;
    String[] storeIds = {};
    private final String TAG = "VoucherHistoryFragment";
    private ProductApi productApi;
    private SharedPreferences sharedPrefs;
    SearchViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentVoucherHistoryBinding.inflate(inflater, container, false);

        productApi = ServiceGenerator.createProductService();
        sharedPrefs = App.getAppContext().getSharedPreferences(App.SESSION, Context.MODE_PRIVATE);
        storeIds = sharedPrefs.getString(SharedPrefsKey.STORE_ID_LIST, "").split(" ");

        voucherAdapter = new VoucherAdapter(new ArrayList<>(), this);
        binding.voucherList.setAdapter(voucherAdapter);
        binding.voucherList.setLayoutManager(new LinearLayoutManager(binding.getRoot().getContext()));
        getVoucherHistory();

        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            voucherHistory.clear();
            getVoucherHistory();
        });

        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireActivity()).get(SearchViewModel.class);
        viewModel.getSearchTerm().observe(getViewLifecycleOwner(), searchTerm -> {
            Log.d(TAG, "searchTerm: " + searchTerm);
            filteredVoucherHistory.clear();
//            if (searchTerm.trim().isEmpty()) {
//                filteredVoucherHistory = voucherHistory;
//            } else {
                for (VoucherDetails voucher : voucherHistory) {
                    if (voucher.voucherCode == null) {
                        voucher.voucherCode = "";
                    }
                    boolean isContained = voucher.voucherCode.trim().toLowerCase().contains(
                            searchTerm.trim().toLowerCase());
                    Log.d(TAG, "voucherCode " + voucher.voucherCode + " contains search-term "
                            + searchTerm + ": " + isContained);
                    if (isContained) {
                        filteredVoucherHistory.add(voucher);
                    }
                }
//            }
            voucherAdapter.setVouchers(filteredVoucherHistory);
        });
    }

    @SuppressLint("CheckResult")
    private void getVoucherHistory() {
        binding.emptyVouchersTextView.setVisibility(View.GONE);

        List<Observable<VoucherHistoryResponse>> requests = new ArrayList<>();

        for (String storeId : storeIds) {
            requests.add(productApi.getRedeemedVouchersByStore(storeId));
        }

        Observable.zip(requests, objects -> {
                    List<VoucherHistoryResponse> responses = new ArrayList<>();
                    for (Object o : objects) {
                        responses.add((VoucherHistoryResponse) o);
                    }
                    return responses;
                }).subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new Observer<List<VoucherHistoryResponse>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                    }

                    @Override
                    public void onNext(List<VoucherHistoryResponse> voucherHistoryResponses) {
                        for (VoucherHistoryResponse response : voucherHistoryResponses) {
                            if (response.status == 200) {
                                for (VoucherDetails voucher : response.data) {
                                    voucher.redeemDate = Utilities.convertUtcTimeToLocalTimezone(
                                            voucher.redeemDate, TimeZone.getDefault());
                                    voucher.thumbnailUrl = sharedPrefs.getString(SharedPrefsKey.BASE_URL,
                                            App.BASE_URL_PRODUCTION) + "asset-service" + voucher.thumbnailUrl;
                                }

                                voucherHistory.addAll(response.data);
                            } else {
                                Log.e(TAG, "onNext error: " + response.status);
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
                        Log.e(TAG, e.getLocalizedMessage());
                        e.printStackTrace();

                        if (voucherHistory.isEmpty()) {
                            binding.emptyVouchersTextView.setText(getString(R.string.voucher_history_error_text));
                            binding.emptyVouchersTextView.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onComplete() {
                        stopLoading();
                        voucherAdapter.setVouchers(voucherHistory);
                        if (voucherHistory.isEmpty()) {
                            binding.emptyVouchersTextView.setText(getString(R.string.empty_vouchers_text));
                            binding.emptyVouchersTextView.setVisibility(View.VISIBLE);
                        }
                    }
                });
    }

    private void stopLoading() {
        binding.progressBars.setVisibility(View.GONE);
        binding.swipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onVoucherClicked(VoucherDetails voucher) {
        new VoucherDetailsDialog(voucher).show(getChildFragmentManager(), VoucherDetailsDialog.TAG);
    }
}