package com.symplified.order.models.error;

import java.io.Serializable;

public class ErrorRequest implements Serializable {
    public String clientId;
    public String errorMsg;
    public String severity;

    public ErrorRequest(String clientId, String errorMsg, String severity) {
        this.clientId = clientId;
        this.errorMsg = errorMsg;
        this.severity = severity;
    }
}
