package org.agra.agra_backend.service;

import org.agra.agra_backend.dao.AdminSettingsRepository;
import org.agra.agra_backend.model.AdminSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminSettingsServiceTest {

    @Mock
    private AdminSettingsRepository repository;

    @InjectMocks
    private AdminSettingsService service;

    @Test
    void getSettingsCreatesDefaultsWhenMissing() {
        when(repository.findById("global")).thenReturn(Optional.empty());
        when(repository.save(any(AdminSettings.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminSettings settings = service.getSettings();

        assertThat(settings.getNewsCron()).isEqualTo("0 0 9 ? * MON");
        assertThat(settings.getNewsFetchCooldownSeconds()).isEqualTo(300);
    }

    @Test
    void updateNewsCronRejectsBlank() {
        String cron = " ";

        assertThatThrownBy(() -> service.updateNewsCron(cron))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cron expression must not be empty");
    }

    @Test
    void markNewsFetchNowEnforcesCooldown() {
        AdminSettings settings = new AdminSettings("global");
        settings.setNewsFetchCooldownSeconds(300);
        settings.setLastNewsFetchAt(Instant.now());
        when(repository.findById("global")).thenReturn(Optional.of(settings));

        Duration cooldown = Duration.ofSeconds(300);

        assertThatThrownBy(() -> service.markNewsFetchNow(cooldown))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Fetch-now rate limited");
    }

    @Test
    void updateNewsCronTrimsAndSaves() {
        AdminSettings settings = new AdminSettings("global");
        when(repository.findById("global")).thenReturn(Optional.of(settings));
        when(repository.save(any(AdminSettings.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminSettings updated = service.updateNewsCron(" 0 0 9 ? * MON ");

        assertThat(updated.getNewsCron()).isEqualTo("0 0 9 ? * MON");
    }

    @Test
    void updateAdminEmailPersistsValue() {
        AdminSettings settings = new AdminSettings("global");
        when(repository.findById("global")).thenReturn(Optional.of(settings));
        when(repository.save(any(AdminSettings.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminSettings updated = service.updateAdminEmail("admin@example.com");

        assertThat(updated.getAdminEmail()).isEqualTo("admin@example.com");
    }

    @Test
    void markNewsFetchNowUpdatesTimestampAndCooldown() {
        AdminSettings settings = new AdminSettings("global");
        settings.setLastNewsFetchAt(null);
        settings.setNewsFetchCooldownSeconds(null);
        when(repository.findById("global")).thenReturn(Optional.of(settings));
        when(repository.save(any(AdminSettings.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminSettings updated = service.markNewsFetchNow(Duration.ofSeconds(120));

        assertThat(updated.getLastNewsFetchAt()).isNotNull();
        assertThat(updated.getNewsFetchCooldownSeconds()).isEqualTo(120);
    }
}
