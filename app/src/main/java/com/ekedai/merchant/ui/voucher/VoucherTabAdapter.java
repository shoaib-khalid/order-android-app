package com.ekedai.merchant.ui.voucher;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.ekedai.merchant.ui.voucher.tabs.VoucherHistoryFragment;
import com.ekedai.merchant.ui.voucher.tabs.VoucherScanFragment;

public class VoucherTabAdapter extends FragmentStateAdapter {
    public VoucherTabAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) {
            return new VoucherScanFragment();
        }

        return new VoucherHistoryFragment();
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
