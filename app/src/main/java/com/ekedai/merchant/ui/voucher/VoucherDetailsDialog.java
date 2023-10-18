package com.ekedai.merchant.ui.voucher;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.ekedai.merchant.R;
import com.ekedai.merchant.models.voucher.VoucherDetails;
import com.ekedai.merchant.utils.Utilities;

public class VoucherDetailsDialog extends DialogFragment {

    public static final String TAG = "VoucherDetailsDialog";
    private final VoucherDetails voucher;

    public VoucherDetailsDialog(VoucherDetails voucher) {
        this.voucher = voucher;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View v = inflater.inflate(R.layout.dialog_voucher_details, container, false);

        v.findViewById(R.id.progress_bar).setVisibility(View.GONE);
        TextView orderSummaryText = v.findViewById(R.id.order_summary_text);
        orderSummaryText.setBackgroundColor(ContextCompat.getColor(v.getContext(), R.color.sf_primary));
        orderSummaryText.setTextColor(ContextCompat.getColor(v.getContext(), R.color.white));

        v.findViewById(R.id.redeem_button).setVisibility(View.GONE);

        AppCompatImageButton closeButton = v.findViewById(R.id.close_button);
        closeButton.setVisibility(View.VISIBLE);
        closeButton.setOnClickListener(b -> dismiss());

        ((TextView) v.findViewById(R.id.voucher_code_text)).setText(getString(R.string.voucher_code_template, voucher.voucherCode));
        ((TextView) v.findViewById(R.id.validity_period_text)).setText(getString(R.string.valid_until_template, voucher.redeemDate));
        ((TextView) v.findViewById(R.id.product_name_text)).setText(voucher.voucherName);
        ((TextView) v.findViewById(R.id.product_quantity_text)).setText(getString(R.string.quantity_template, "1"));
        ((TextView) v.findViewById(R.id.product_price_text)).setText(getString(
                R.string.price_template,
                voucher.currencyLabel,
                Utilities.formatPrice(voucher.discountValue)
        ));
        Glide.with(v.getContext()).load(voucher.thumbnailUrl).into(((ImageView) v.findViewById(R.id.product_image)));

        return v;
    }
}
