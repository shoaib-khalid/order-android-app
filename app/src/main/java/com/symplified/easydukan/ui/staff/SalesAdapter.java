package com.symplified.easydukan.ui.staff;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.symplified.easydukan.R;
import com.symplified.easydukan.models.staff.shift.SummaryDetails;
import com.symplified.easydukan.utils.Utility;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class SalesAdapter extends RecyclerView.Adapter<SalesAdapter.ViewHolder> {

    private List<SummaryDetails> summaryDetails = new ArrayList<>();
    private String currency = "RM";
    private final DecimalFormat formatter = Utility.getMonetaryAmountFormat();

    public SalesAdapter() {
    }

    public SalesAdapter(List<SummaryDetails> summaryDetails) {
        this.summaryDetails = summaryDetails;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView paymentTypeTextView, salesAmountTextView;

        public ViewHolder(View view) {
            super(view);

            paymentTypeTextView = view.findViewById(R.id.payment_type_text_view);
            salesAmountTextView = view.findViewById(R.id.sales_amount_text_view);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        return new ViewHolder(LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.row_sales_summary, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        viewHolder.paymentTypeTextView.setText(summaryDetails.get(position).paymentChannel);
        viewHolder.salesAmountTextView.setText(
                currency + formatter.format(summaryDetails.get(position).saleAmount)
        );
    }

    @Override
    public int getItemCount() { return summaryDetails.size(); }

    public void setCurrency(String currency) { this.currency = currency; }
    public void setSummaryDetails(List<SummaryDetails> summaryDetails) {
        clear();
        this.summaryDetails = summaryDetails;
        notifyItemRangeInserted(0, summaryDetails.size());
    }
    public List<SummaryDetails> getSummaryDetails() {
        return summaryDetails;
    }
    public void clear() {
        int originalSize = summaryDetails.size();
        summaryDetails.clear();
        notifyItemRangeRemoved(0, originalSize);
    }
}
