package com.symplified.order.models.staff;

import java.io.Serializable;

public class PasswordChangeRequest implements Serializable {
    public String password;

    public PasswordChangeRequest(String password) {this.password = password;}
}
