package com.symplified.order.models.staff;

import java.io.Serializable;

public class RegisterStaffMemberRequest implements Serializable {
    public String storeId;
    public String name;
    public String username;
    public String password;

    public RegisterStaffMemberRequest(
            String storeId,
            String name,
            String username,
            String password
    ) {
        this.storeId = storeId;
        this.name = name;
        this.username = username;
        this.password = password;
    }
}
