package io.lighty.core.controller.springboot.services;

import io.lighty.core.controller.springboot.services.dto.LoginRequest;
import io.lighty.core.controller.springboot.services.dto.UserData;

import java.util.Optional;

public interface UserAccessService {

    Optional<UserData> login(String sessionId, LoginRequest loginRequest);

    Optional<UserData> isAuthenticated(String sessionId);

    void logout(String sessionId);

}
