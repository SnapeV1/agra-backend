package org.agra.agra_backend.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.agra.agra_backend.model.User;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private User user;
}