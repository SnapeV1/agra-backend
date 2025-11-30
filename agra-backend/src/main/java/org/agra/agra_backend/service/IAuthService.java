package org.agra.agra_backend.service;

import org.agra.agra_backend.model.User;
import org.agra.agra_backend.payload.LoginRequest;
import org.agra.agra_backend.payload.LoginResponse;
import org.agra.agra_backend.payload.RegisterRequest;

public interface IAuthService {
    User registerUser(RegisterRequest request);
    LoginResponse login(LoginRequest request);

}
