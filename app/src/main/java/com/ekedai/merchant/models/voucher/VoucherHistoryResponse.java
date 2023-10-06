package com.ekedai.merchant.models.voucher;

import com.ekedai.merchant.models.HttpResponse;

import java.util.List;

public class VoucherHistoryResponse extends HttpResponse {
    public List<VoucherDetails> data;
}
