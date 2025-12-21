package org.agra.agra_backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class LocaleConfigTest {

    @Test
    void localeResolverSupportsDefaults() {
        LocaleConfig config = new LocaleConfig();

        LocaleResolver resolver = config.localeResolver();

        assertThat(resolver).isInstanceOf(AcceptHeaderLocaleResolver.class);
        AcceptHeaderLocaleResolver accept = (AcceptHeaderLocaleResolver) resolver;
        assertThat(accept.getSupportedLocales()).contains(Locale.ENGLISH, Locale.FRENCH);
        MockHttpServletRequest request = new MockHttpServletRequest();
        assertThat(accept.resolveLocale(request)).isEqualTo(Locale.ENGLISH);
    }

    @Test
    void messageSourceUsesBundle() {
        LocaleConfig config = new LocaleConfig();

        MessageSource source = config.messageSource();

        assertThat(source).isNotNull();
    }
}
