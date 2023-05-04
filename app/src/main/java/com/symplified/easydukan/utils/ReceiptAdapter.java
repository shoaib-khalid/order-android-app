package com.symplified.easydukan.utils;

import android.content.Context;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;

public class ReceiptAdapter extends PrintDocumentAdapter {

    Context context;

    public ReceiptAdapter(Context context) {
        this.context = context;
    }

    @Override
    public void onLayout(
            PrintAttributes oldAttributes,
            PrintAttributes newAttributes,
            CancellationSignal cancellationSignal,
            LayoutResultCallback callback,
            Bundle extras
    ) {

    }

    @Override
    public void onWrite(
            PageRange[] pageRanges,
            ParcelFileDescriptor destination,
            CancellationSignal cancellationSignal,
            WriteResultCallback callback
    ) {


    }
}
