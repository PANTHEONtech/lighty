package io.lighty.core.controller.springboot.services.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class LoginRequest {

    private final String userName;
    private final String password;

    @JsonCreator
    public LoginRequest(@JsonProperty("userName") String userName,
                        @JsonProperty("password") String password) {
        this.userName = userName;
        this.password = password;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

}
