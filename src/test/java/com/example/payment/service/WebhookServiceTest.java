package com.example.payment.service;

import com.example.payment.domain.WebhookRegistration;
import com.example.payment.repository.WebhookRegistrationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;

class WebhookServiceTest {

    private final WebhookRegistrationRepository repository = mock(WebhookRegistrationRepository.class);
    private final WebhookService service = new WebhookService(repository);

    @Nested
    @DisplayName("validate url and register")
    class ValidateAndRegister {
        @ParameterizedTest(name = "invalid url -> {0}")
        @ValueSource(strings = {
                "", "ftp://host", "http://", "https://", "http:// ", "not-a-url", "http:/missing-slash"})
        void shouldRejectInvalidUrls(String url) {
            // when/then
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.register(url));
            assertTrue(ex.getMessage().toLowerCase().contains("invalid endpointurl"));
            verifyNoInteractions(repository);
        }

        @ParameterizedTest(name = "valid url -> {0}")
        @ValueSource(strings = {
                "http://example.com/wh", "https://example.com", "https://api.example.com/path?q=1"})
        void shouldAcceptValidUrls(String url) {
            // given
            given(repository.existsByEndpointUrl(url)).willReturn(false);
            ArgumentCaptor<WebhookRegistration> captor = ArgumentCaptor.forClass(WebhookRegistration.class);
            given(repository.save(any())).willAnswer(inv -> {
                WebhookRegistration w = inv.getArgument(0);
                w.setId("id-1");
                return w;
            });

            // when
            WebhookRegistration saved = service.register(url);

            // then
            then(repository).should().existsByEndpointUrl(url);
            then(repository).should().save(captor.capture());
            WebhookRegistration arg = captor.getValue();
            assertEquals(url, arg.getEndpointUrl());
            assertTrue(arg.isActive());
            assertNotNull(arg.getCreatedAt());
            assertEquals("id-1", saved.getId());
        }

        @Test
        void shouldBeIdempotentOnExistingUrl() {
            // given
            String url = "https://a.b/c";
            given(repository.existsByEndpointUrl(url)).willReturn(true);
            WebhookRegistration existing = new WebhookRegistration();
            existing.setId("abc");
            existing.setEndpointUrl(url);
            existing.setActive(false);
            existing.setCreatedAt(Instant.now().minusSeconds(60));
            List<WebhookRegistration> list = new ArrayList<>();
            list.add(existing);
            given(repository.findAll()).willReturn(list);
            given(repository.save(any(WebhookRegistration.class))).willAnswer(inv -> inv.getArgument(0));

            // when
            WebhookRegistration result = service.register(url);

            // then
            assertEquals("abc", result.getId());
            assertTrue(result.isActive(), "should re-activate existing");
            then(repository).should().save(existing);
        }
    }
}
