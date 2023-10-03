package com.ekedai.merchant.models.ping;

import java.io.Serializable;

public class PingRequest implements Serializable {
    public String deviceModel;

    public PingRequest(String deviceModel) {
        this.deviceModel = deviceModel;
    }
}
