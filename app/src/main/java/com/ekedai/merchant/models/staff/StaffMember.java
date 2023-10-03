package com.ekedai.merchant.models.staff;

import java.io.Serializable;

public class StaffMember implements Serializable {
    public String id;
    public String storeId;
    public String username;
    public String name;
    public Boolean isLoading = false;
}
