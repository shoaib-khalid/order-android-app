package com.symplified.order.models.login;

import java.io.Serializable;
import java.util.List;

public class LoginData implements Serializable {
    public Session session;
    public String role;
    public List<String> authorities;

    @Override
    public String toString() {
        return "LoginData{" +
                "session=" + session +
                ", role='" + role + '\'' +
                ", authorities=" + authorities +
                '}';
    }
}
