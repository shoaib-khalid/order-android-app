package com.symplified.order.enums;

import com.symplified.order.R;

public enum ProductStatus {
    ACTIVE("Active", R.color.sf_primary),
    INACTIVE("Inactive", R.color.sf_cancel_button),
    OUTOFSTOCK("Out of Stock", R.color.dark_grey);

    public final String text;
    public final int color;

    ProductStatus(String text, int color) {
        this.text = text;
        this.color = color;
    }
}
