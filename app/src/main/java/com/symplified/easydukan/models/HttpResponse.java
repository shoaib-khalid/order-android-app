package com.symplified.easydukan.models;

import java.io.Serializable;

public class HttpResponse implements Serializable {

    public String timestamp;
    public int status;
    public String message;
    public String path;

}