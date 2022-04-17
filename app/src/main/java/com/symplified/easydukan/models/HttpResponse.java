package com.symplified.easydukan.models;

import java.io.Serializable;
import java.util.Date;

public class HttpResponse implements Serializable {

    public transient Date timestamp;
    public int status;
    public String message;
//    public Object data;
    public String path;

}
