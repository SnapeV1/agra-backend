package org.agra.agra_backend.model;

import java.time.Instant;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "admin_settings")
public class AdminSettings {
    @Id
    private String id;

    private String newsCron; // e.g. "0 0 9 ? * MON"
    private Integer newsFetchCooldownSeconds;
    private Instant lastNewsFetchAt;

    private Boolean twoFactorEnforced;
    private String adminEmail;

    public AdminSettings() {}

    public AdminSettings(String id) {
        this.id = id;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNewsCron() { return newsCron; }
    public void setNewsCron(String newsCron) { this.newsCron = newsCron; }

    public Integer getNewsFetchCooldownSeconds() { return newsFetchCooldownSeconds; }
    public void setNewsFetchCooldownSeconds(Integer newsFetchCooldownSeconds) { this.newsFetchCooldownSeconds = newsFetchCooldownSeconds; }

    public Instant getLastNewsFetchAt() { return lastNewsFetchAt; }
    public void setLastNewsFetchAt(Instant lastNewsFetchAt) { this.lastNewsFetchAt = lastNewsFetchAt; }

    public Boolean getTwoFactorEnforced() { return twoFactorEnforced; }
    public void setTwoFactorEnforced(Boolean twoFactorEnforced) { this.twoFactorEnforced = twoFactorEnforced; }

    public String getAdminEmail() { return adminEmail; }
    public void setAdminEmail(String adminEmail) { this.adminEmail = adminEmail; }
}
