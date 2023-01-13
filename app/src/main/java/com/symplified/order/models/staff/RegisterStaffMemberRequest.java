package com.symplified.order.models.staff;

import java.io.Serializable;

public class RegisterStaffMemberRequest implements Serializable {
    public String storeId;
    public String username;
    public String password;

    public RegisterStaffMemberRequest(
            String storeId,
            String username,
            String password
    ) {
        this.storeId = storeId;
        this.username = username;
        this.password = password;
    }
}
