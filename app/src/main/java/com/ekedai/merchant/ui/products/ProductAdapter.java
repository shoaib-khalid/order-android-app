package com.ekedai.merchant.ui.products;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.ekedai.merchant.R;
import com.ekedai.merchant.databinding.ProductRowBinding;
import com.ekedai.merchant.models.product.Product;
import com.ekedai.merchant.utils.Utilities;

public class ProductAdapter extends ListAdapter<Product, ProductAdapter.ProductViewHolder> {

    private final String currencySymbol;
    private final OnProductClickListener clickListener;

    public interface OnProductClickListener {
        void onProductClicked(Product p);
    }

    public ProductAdapter(
            String currencySymbol,
            OnProductClickListener clickListener
    ) {
        super(DIFF_CALLBACK);
        this.currencySymbol = currencySymbol;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ProductViewHolder productViewHolder = new ProductViewHolder(
                ProductRowBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false),
                currencySymbol
        );
        productViewHolder.itemView.setOnClickListener(v ->
                clickListener.onProductClicked(getItem(productViewHolder.getAdapterPosition()))
        );
        return productViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    public static class ProductViewHolder extends RecyclerView.ViewHolder {

        private final ProductRowBinding binding;
        private final String currencySymbol;

        public ProductViewHolder(ProductRowBinding binding, String currencySymbol) {
            super(binding.getRoot());
            this.binding = binding;
            this.currencySymbol = currencySymbol;
        }

        public void bind(Product product) {
            binding.productName.setText(product.name);

            if (product.store != null && product.productInventories != null
                    && product.productInventories.size() > 0) {
                Product.ProductInventory inv = product.productInventories.get(0);
                binding.productPrice.setText(binding.getRoot().getContext().getString(
                        R.string.monetary_amount,
                        currencySymbol,
                        Utilities.formatPrice(product.store.isDineIn ? inv.dineInPrice : inv.price)
                ));
            }

            binding.productStatus.setText(product.status.text);
            binding.productStatus.setTextColor(product.status.color);
            binding.productStatusIcon.setColorFilter(product.status.color);

            if (product.thumbnailUrl != null) {
                Glide.with(binding.getRoot().getContext())
                        .load(product.thumbnailUrl)
                        .into(binding.productImage);
            }
        }
    }

    public static final DiffUtil.ItemCallback<Product> DIFF_CALLBACK = new DiffUtil.ItemCallback<Product>() {
        @Override
        public boolean areItemsTheSame(@NonNull Product oldProduct, @NonNull Product newProduct) {
            return oldProduct.id.equals(newProduct.id);
        }

        @Override
        public boolean areContentsTheSame(@NonNull Product oldProduct, @NonNull Product newProduct) {
            return oldProduct.name.equals(newProduct.name)
                    && oldProduct.status.name().equals(newProduct.status.name());
        }
    };
}
