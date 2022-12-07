package com.symplified.order.models.login;

import java.io.Serializable;
import java.util.Date;

public class Session implements Serializable {

    public String username;
    public Date expiry;
    public Date created;
    public String accessToken;
    public String refreshToken;
    public String ownerId;

    @Override
    public String toString() {
        return "Session{" +
                "username='" + username + '\'' +
                ", expiry=" + expiry +
                ", created=" + created +
                ", accessToken='" + accessToken + '\'' +
                ", refreshToken='" + refreshToken + '\'' +
                ", ownerId='" + ownerId + '\'' +
                '}';
    }
}
