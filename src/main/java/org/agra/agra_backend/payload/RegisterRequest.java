package org.agra.agra_backend.payload;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor

public class RegisterRequest {
    private String name;
    private String email;
    private String phone;
    private String password;
    private String country;
    private String language;
    private String domain;
    private String role;
    private String picture;

}
