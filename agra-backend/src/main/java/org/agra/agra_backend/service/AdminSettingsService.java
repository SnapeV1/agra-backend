package org.agra.agra_backend.service;

import java.time.Duration;
import java.time.Instant;
import org.agra.agra_backend.dao.AdminSettingsRepository;
import org.agra.agra_backend.model.AdminSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminSettingsService {

    private static final Logger log = LoggerFactory.getLogger(AdminSettingsService.class);
    private static final String SETTINGS_ID = "global";
    private static final String DEFAULT_NEWS_CRON = "0 0 9 ? * MON";
    private static final int DEFAULT_FETCH_COOLDOWN_SECONDS = 300;

    private final AdminSettingsRepository repository;

    public AdminSettingsService(AdminSettingsRepository repository) {
        this.repository = repository;
    }

    public AdminSettings getSettings() {
        return repository.findById(SETTINGS_ID).orElseGet(() -> {
            AdminSettings s = new AdminSettings(SETTINGS_ID);
            s.setNewsCron(DEFAULT_NEWS_CRON);
            s.setNewsFetchCooldownSeconds(DEFAULT_FETCH_COOLDOWN_SECONDS);
            return repository.save(s);
        });
    }

    @Transactional
    public AdminSettings updateNewsCron(String cron) {
        if (!StringUtils.hasText(cron)) {
            throw new IllegalArgumentException("Cron expression must not be empty");
        }
        AdminSettings s = getSettings();
        s.setNewsCron(cron.trim());
        return repository.save(s);
    }

    @Transactional
    public AdminSettings updateAdminEmail(String email) {
        AdminSettings s = getSettings();
        s.setAdminEmail(email);
        return repository.save(s);
    }

    @Transactional
    public AdminSettings markNewsFetchNow(Duration cooldown) {
        AdminSettings s = getSettings();
        Instant now = Instant.now();
        Instant last = s.getLastNewsFetchAt();
        int cooldownSeconds = cooldown != null ? (int) cooldown.toSeconds() : DEFAULT_FETCH_COOLDOWN_SECONDS;
        if (s.getNewsFetchCooldownSeconds() == null) {
            s.setNewsFetchCooldownSeconds(DEFAULT_FETCH_COOLDOWN_SECONDS);
        }
        if (last != null) {
            Instant nextAllowed = last.plusSeconds(s.getNewsFetchCooldownSeconds() == null
                    ? DEFAULT_FETCH_COOLDOWN_SECONDS
                    : s.getNewsFetchCooldownSeconds());
            if (now.isBefore(nextAllowed)) {
                long waitSeconds = Duration.between(now, nextAllowed).toSeconds();
                throw new IllegalStateException("Fetch-now rate limited. Try again in " + waitSeconds + " seconds.");
            }
        }
        s.setLastNewsFetchAt(now);
        if (cooldown != null) {
            s.setNewsFetchCooldownSeconds((int) cooldown.toSeconds());
        }
        return repository.save(s);
    }
}
