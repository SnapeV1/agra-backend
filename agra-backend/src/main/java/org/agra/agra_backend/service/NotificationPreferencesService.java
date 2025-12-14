package org.agra.agra_backend.service;

import org.agra.agra_backend.model.NotificationPreferences;
import org.agra.agra_backend.model.User;
import org.agra.agra_backend.dao.UserRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class NotificationPreferencesService {

    private final UserRepository userRepository;

    public NotificationPreferencesService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public NotificationPreferences getOrCreate(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found for id " + userId));

        NotificationPreferences prefs = user.getNotificationPreferences();
        if (prefs == null) {
            prefs = defaultForUser();
            user.setNotificationPreferences(prefs);
            userRepository.save(user);
        }
        return prefs;
    }

    public NotificationPreferences upsert(String userId, NotificationPreferences incoming) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found for id " + userId));

        NotificationPreferences prefs = user.getNotificationPreferences();
        if (prefs == null) {
            prefs = defaultForUser();
        }

        prefs.setNotificationsEnabled(incoming.isNotificationsEnabled());
        prefs.setChannels(safeChannels(incoming.getChannels()));
        prefs.setDigest(safeDigest(incoming.getDigest()));

        user.setNotificationPreferences(prefs);
        userRepository.save(user);
        return prefs;
    }

    private NotificationPreferences defaultForUser() {
        NotificationPreferences prefs = new NotificationPreferences();
        prefs.setNotificationsEnabled(true);
        prefs.setChannels(new ArrayList<>(List.of("email", "websocket")));
        prefs.setDigest(new NotificationPreferences.DigestSettings(false, "daily"));
        return prefs;
    }

    private List<String> safeChannels(List<String> channels) {
        if (channels == null || channels.isEmpty()) {
            return new ArrayList<>(List.of("email", "websocket"));
        }
        return new ArrayList<>(channels);
    }

    private NotificationPreferences.DigestSettings safeDigest(NotificationPreferences.DigestSettings digest) {
        if (digest == null) {
            return new NotificationPreferences.DigestSettings(false, "daily");
        }
        if (digest.getFrequency() == null || digest.getFrequency().isBlank()) {
            digest.setFrequency("daily");
        }
        return digest;
    }
}
