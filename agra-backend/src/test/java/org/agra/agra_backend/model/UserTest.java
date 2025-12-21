package org.agra.agra_backend.model;

import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void gettersAndSettersWork() {
        User user = new User();
        user.setId("u1");
        user.setName("User");
        user.setEmail("user@example.com");
        user.setPhone("123");
        user.setPassword("pass");
        user.setCountry("GH");
        user.setLanguage("en");
        user.setDomain("agri");
        user.setRole("ADMIN");
        user.setPicture("pic");
        user.setBirthdate(new Date());
        user.setThemePreference("dark");
        user.setRegisteredAt(new Date());
        user.setIsArchived(true);
        user.setVerified(true);
        user.setTwoFactorEnabled(true);
        user.setTwoFactorSecret("secret");
        user.setTwoFactorRecoveryCodes(List.of("code1"));
        user.setTwoFactorVerifiedAt(new Date());

        assertThat(user.getId()).isEqualTo("u1");
        assertThat(user.getName()).isEqualTo("User");
        assertThat(user.getEmail()).isEqualTo("user@example.com");
        assertThat(user.getPhone()).isEqualTo("123");
        assertThat(user.getPassword()).isEqualTo("pass");
        assertThat(user.getCountry()).isEqualTo("GH");
        assertThat(user.getLanguage()).isEqualTo("en");
        assertThat(user.getDomain()).isEqualTo("agri");
        assertThat(user.getRole()).isEqualTo("ADMIN");
        assertThat(user.getPicture()).isEqualTo("pic");
        assertThat(user.getThemePreference()).isEqualTo("dark");
        assertThat(user.getIsArchived()).isTrue();
        assertThat(user.getVerified()).isTrue();
        assertThat(user.getTwoFactorEnabled()).isTrue();
        assertThat(user.getTwoFactorSecret()).isEqualTo("secret");
        assertThat(user.getTwoFactorRecoveryCodes()).containsExactly("code1");
        assertThat(user.getTwoFactorVerifiedAt()).isNotNull();
    }

    @Test
    void customConstructorIsCovered() {
        User user = new User("u2", "Name", "pic");
        assertThat(user).isNotNull();
    }
}
