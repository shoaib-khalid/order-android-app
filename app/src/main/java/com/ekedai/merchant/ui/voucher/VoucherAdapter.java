package com.ekedai.merchant.ui.voucher;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ekedai.merchant.R;
import com.ekedai.merchant.models.voucher.VoucherDetails;
import com.ekedai.merchant.utils.Utilities;

import java.util.ArrayList;
import java.util.List;

public class VoucherAdapter extends RecyclerView.Adapter<VoucherAdapter.ViewHolder> {

    public interface OnVoucherClickListener {
        void onVoucherClicked(VoucherDetails voucher);
    }

    private List<VoucherDetails> vouchers = new ArrayList<>();
    private final OnVoucherClickListener listener;

    public VoucherAdapter(List<VoucherDetails> vouchers, OnVoucherClickListener listener) {
        this.vouchers = vouchers;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView voucherCodeText, redemptionDateText, priceText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            voucherCodeText = itemView.findViewById(R.id.voucher_code_text);
            redemptionDateText = itemView.findViewById(R.id.redemption_date_text);
            priceText = itemView.findViewById(R.id.price_text);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        return new ViewHolder(LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.row_voucher_code, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.voucherCodeText.setText(
                holder.voucherCodeText.getContext().getString(
                        R.string.voucher_code_template,
                        vouchers.get(position).voucherCode
                )
        );

        holder.redemptionDateText.setText(
                holder.redemptionDateText.getContext().getString(
                        R.string.redemption_date_template,
                        vouchers.get(position).redeemDate
                )
        );

        holder.priceText.setText(
                holder.priceText.getContext().getString(
                        R.string.price_template,
                        vouchers.get(position).currencyLabel,
                        Utilities.formatPrice(vouchers.get(position).discountValue)
                )
        );

        holder.itemView.setOnClickListener(v -> listener.onVoucherClicked(vouchers.get(holder.getAdapterPosition())));
    }

    @Override
    public int getItemCount() { return vouchers.size(); }

    public void setVouchers(List<VoucherDetails> newVouchers) {
        int originalSize = vouchers.size();
        vouchers.clear();
        notifyItemRangeRemoved(0, originalSize);

        vouchers.addAll(newVouchers);
        notifyItemRangeInserted(0, newVouchers.size());

//        notifyDataSetChanged();
    }
}
