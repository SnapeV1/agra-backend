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
        assertThat(translations).containsEntry(I18nResolver.resolveKey(translations, "fr", "en"), resolved);
    }

    @Test
    void resolveUsesFallbackLanguage() {
        Map<String, String> translations = Map.of("en", "Hello", "fr", "Bonjour");

        String resolved = I18nResolver.resolve(translations, "de", "en");

        assertThat(resolved).isEqualTo("Hello");
        assertThat(translations).containsEntry(I18nResolver.resolveKey(translations, "de", "en"), resolved);
    }

    @Test
    void resolveFallsBackToFirstEntry() {
        Map<String, String> translations = Map.of("es", "Hola", "it", "Ciao");

        String resolved = I18nResolver.resolve(translations, "de", "en");
        String key = I18nResolver.resolveKey(translations, "de", "en");

        assertThat(resolved).isNotNull();
        assertThat(key).isNotNull();
        assertThat(translations).containsEntry(key, resolved);
    }
}
