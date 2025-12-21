package org.agra.agra_backend.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AdminSettingsTest {

    @Test
    void gettersAndSettersWork() {
        AdminSettings settings = new AdminSettings("global");
        settings.setNewsCron("cron");
        settings.setNewsFetchCooldownSeconds(120);
        settings.setLastNewsFetchAt(Instant.now());
        settings.setTwoFactorEnforced(true);
        settings.setAdminEmail("admin@example.com");

        assertThat(settings.getId()).isEqualTo("global");
        assertThat(settings.getNewsCron()).isEqualTo("cron");
        assertThat(settings.getNewsFetchCooldownSeconds()).isEqualTo(120);
        assertThat(settings.getLastNewsFetchAt()).isNotNull();
        assertThat(settings.getTwoFactorEnforced()).isTrue();
        assertThat(settings.getAdminEmail()).isEqualTo("admin@example.com");
    }
}
