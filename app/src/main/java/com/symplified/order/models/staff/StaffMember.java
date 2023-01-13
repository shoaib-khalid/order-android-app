package com.symplified.order.models.staff;

import java.io.Serializable;

public class StaffMember implements Serializable {
    public String id;
    public String storeId;
    public String username;
    public String name;
    public Boolean locked;
    public Boolean deactivated;
}