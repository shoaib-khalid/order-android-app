package com.symplified.order.models;

import com.symplified.order.models.login.LoginData;

import java.io.Serializable;
import java.util.Date;

public class HttpResponse implements Serializable {

    public String timestamp;
    public int status;
    public String message;
    public String path;

}
