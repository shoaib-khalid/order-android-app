package com.symplified.order.models;

import java.io.Serializable;

public class HttpResponse implements Serializable {

    public String timestamp;
    public int status;
    public String message;
    public String path;

}
