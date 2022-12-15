package com.symplified.order.models.qrcode;

import java.io.Serializable;

public class QrCodeRequest implements Serializable {
    public String storeId;

    public QrCodeRequest(String storeId) { this.storeId = storeId; }
}
