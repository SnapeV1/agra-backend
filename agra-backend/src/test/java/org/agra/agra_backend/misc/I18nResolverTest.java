package org.agra.agra_backend.misc;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class I18nResolverTest {

    @Test
    void resolvePrefersRequestedLanguage() {
        Map<String, String> translations = Map.of("en", "Hello", "fr", "Bonjour");

        String resolved = I18nResolver.resolve(translations, "fr", "en");

        assertThat(resolved).isEqualTo("Bonjour");
        assertThat(I18nResolver.resolveKey(translations, "fr", "en")).isEqualTo("fr");
    }

    @Test
    void resolveUsesFallbackLanguage() {
        Map<String, String> translations = Map.of("en", "Hello", "fr", "Bonjour");

        String resolved = I18nResolver.resolve(translations, "de", "en");

        assertThat(resolved).isEqualTo("Hello");
        assertThat(I18nResolver.resolveKey(translations, "de", "en")).isEqualTo("en");
    }

    @Test
    void resolveFallsBackToFirstEntry() {
        Map<String, String> translations = Map.of("es", "Hola", "it", "Ciao");

        String resolved = I18nResolver.resolve(translations, "de", "en");
        String key = I18nResolver.resolveKey(translations, "de", "en");

        assertThat(resolved).isNotNull();
        assertThat(key).isNotNull();
        assertThat(translations.get(key)).isEqualTo(resolved);
    }
}
