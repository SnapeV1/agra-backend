package org.agra.agra_backend.misc;

import java.util.Map;

public final class I18nResolver {
    private I18nResolver() {
    }

    public static String resolve(Map<String, String> translations, String lang, String fallback) {
        if (translations == null || translations.isEmpty()) {
            return null;
        }

        if (lang != null && translations.containsKey(lang)) {
            return translations.get(lang);
        }

        if (fallback != null && translations.containsKey(fallback)) {
            return translations.get(fallback);
        }

        return translations.values().iterator().next();
    }

    public static String resolveKey(Map<String, String> translations, String lang, String fallback) {
        if (translations == null || translations.isEmpty()) {
            return null;
        }

        if (lang != null && translations.containsKey(lang)) {
            return lang;
        }

        if (fallback != null && translations.containsKey(fallback)) {
            return fallback;
        }

        return translations.keySet().iterator().next();
    }
}
