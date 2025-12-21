package org.agra.agra_backend.service;

import org.agra.agra_backend.dao.UserRepository;
import org.agra.agra_backend.model.NotificationPreferences;
import org.agra.agra_backend.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationPreferencesServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationPreferencesService service;

    @Test
    void getOrCreateCreatesDefaultPrefs() {
        User user = new User();
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));

        NotificationPreferences prefs = service.getOrCreate("u1");

        assertThat(prefs.isNotificationsEnabled()).isTrue();
        assertThat(prefs.getChannels()).contains("email");
        verify(userRepository).save(user);
    }

    @Test
    void upsertNormalizesEmptyValues() {
        User user = new User();
        NotificationPreferences existing = new NotificationPreferences();
        user.setNotificationPreferences(existing);
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));

        NotificationPreferences incoming = new NotificationPreferences();
        incoming.setNotificationsEnabled(false);

        NotificationPreferences result = service.upsert("u1", incoming);

        assertThat(result.isNotificationsEnabled()).isFalse();
        assertThat(result.getChannels()).contains("email");
        assertThat(result.getDigest().getFrequency()).isEqualTo("daily");
        verify(userRepository).save(user);
    }
}
