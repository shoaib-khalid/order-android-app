package com.ekedai.merchant.models.voucher;

import java.io.Serializable;

public class VoucherQrCodeDetails implements Serializable {
    public String customerName;
    public String invoiceId;
    public String productName;
    public Double productPrice;
    public String date;
    public String storeName;
    public String storeId;
    public String status;
    public String phoneNumber;
    public String voucherCode;
    public String productImageUrl;
}
