package com.ekedai.merchant.ui.voucher;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.ekedai.merchant.R;
import com.ekedai.merchant.databinding.FragmentVoucherBinding;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class VoucherFragment extends Fragment {

    FragmentVoucherBinding binding;
    final String TAG = "voucher-fragment";
    final TabDetails[] tabDetails = {
            new TabDetails(R.drawable.ic_scan_qr_code, R.string.scan_code),
            new TabDetails(R.drawable.ic_history, R.string.history),
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentVoucherBinding.inflate(inflater, container, false);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        VoucherTabAdapter tabAdapter = new VoucherTabAdapter(this);
        binding.viewPager.setAdapter(tabAdapter);

        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                View v = tab.getCustomView();
                ((TextView) v.findViewById(R.id.text_view)).setTextColor(ContextCompat.getColor(tab.view.getContext(), R.color.white));
                ((ImageView) v.findViewById(R.id.icon)).setColorFilter(ContextCompat.getColor(tab.view.getContext(), R.color.white));
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                View v = tab.getCustomView();
                ((TextView) v.findViewById(R.id.text_view)).setTextColor(ContextCompat.getColor(tab.view.getContext(), R.color.black));
                ((ImageView) v.findViewById(R.id.icon)).setColorFilter(ContextCompat.getColor(tab.view.getContext(), R.color.black));

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        new TabLayoutMediator(binding.tabLayout, binding.viewPager,
                (tab, position) -> {
                    View v = LayoutInflater.from(getContext()).inflate(R.layout.icon_with_text, null);
                    TextView textView = v.findViewById(R.id.text_view);
                    textView.setText(getString(tabDetails[position].text));
                    ImageView iconView = v.findViewById(R.id.icon);
                    iconView.setImageResource(tabDetails[position].icon);

                    tab.setCustomView(v);
                }
        ).attach();

    }

    static class TabDetails {
        public final int icon;
        public final int text;

        public TabDetails(int icon, int text) {
            this.icon = icon;
            this.text = text;
        }
    }
}