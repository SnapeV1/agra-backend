package org.agra.agra_backend.payload;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.agra.agra_backend.model.User;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private User user;
    private Boolean existingAccount;
    private String passwordResetToken;

    @JsonProperty("profileCompleted")
    public boolean isProfileCompleted() {
        if (user == null) return false;
        // Consider profile incomplete if password is not set
        return notBlank(user.getPassword())
                && notBlank(user.getName())
                && notBlank(user.getEmail())
                && notBlank(user.getPicture())
                && notBlank(user.getPhone())
                && notBlank(user.getCountry());
    }

    private boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    @JsonProperty("resetToken")
    public String getResetTokenAlias() { return passwordResetToken; }

    @JsonProperty("passwordToken")
    public String getPasswordTokenAlias() { return passwordResetToken; }

    @JsonProperty("verificationToken")
    public String getVerificationTokenAlias() { return passwordResetToken; }
}
