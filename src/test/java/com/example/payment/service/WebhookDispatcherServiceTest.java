package com.example.payment.service;

import com.example.payment.domain.WebhookEvent;
import com.example.payment.domain.WebhookStatus;
import com.example.payment.repository.WebhookEventRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

class WebhookDispatcherServiceTest {

    private WebhookEventRepository repository;
    private HttpClient httpClient;

    @BeforeEach
    void setup() {
        repository = mock(WebhookEventRepository.class);
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    static class FixedStatusHandler implements HttpHandler {
        private final int status;

        FixedStatusHandler(int status) {
            this.status = status;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            byte[] body = new byte[0];
            exchange.sendResponseHeaders(status, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        }
    }

    @Nested
    @DisplayName("dispatchLoop")
    class DispatchLoop {

        private HttpServer server;
        private int port;

        @BeforeEach
        void startServer() throws IOException {
            server = HttpServer.create(new InetSocketAddress(0), 0);
            port = server.getAddress().getPort();
            // Handlers for different paths
            server.createContext("/ok", new FixedStatusHandler(204));
            server.createContext("/s200", new FixedStatusHandler(200));
            server.createContext("/b400", new FixedStatusHandler(400));
            server.createContext("/e500", new FixedStatusHandler(500));
            server.start();
        }

        @AfterEach
        void stopServer() {
            if (server != null) server.stop(0);
        }

        @ParameterizedTest(name = "status {1} => delivered={2}")
        @CsvSource({
                "ok,204,true",
                "s200,200,true",
                "b400,400,false",
                "e500,500,false"
        })
        void shouldHandleHttpStatuses(String path, int code, boolean delivered) {
            // given
            WebhookEvent ev = new WebhookEvent();
            ev.setId("e1");
            ev.setTargetUrl("http://localhost:" + port + "/" + path);
            ev.setPayloadJson("{}\n");
            ev.setStatus(WebhookStatus.PENDING);
            ev.setAttempts(0);
            ev.setCreatedAt(Instant.now().minusSeconds(5));
            ev.setNextAttemptAt(Instant.now().minusSeconds(1)); // due

            List<WebhookEvent> due = new ArrayList<>();
            due.add(ev);
            given(repository.findByStatusInAndNextAttemptAtLessThanEqual(
                    anyList(), any(Instant.class))).willReturn(due);
            given(repository.save(any(WebhookEvent.class))).willAnswer(inv -> inv.getArgument(0));

            WebhookDispatcherService service = new WebhookDispatcherService(
                    repository, httpClient,
                    true, // enabled
                    3,    // maxAttempts
                    50,   // baseBackoffMs
                    1_000 // maxBackoffMs
            );

            Instant before = Instant.now();
            // when
            service.dispatchLoop();
            Instant after = Instant.now();

            // then
            then(repository).should().save(argThat(saved -> {
                assertEquals("e1", saved.getId());
                assertEquals(1, saved.getAttempts(), "attempt should increment");
                if (delivered) {
                    assertEquals(WebhookStatus.DELIVERED, saved.getStatus());
                    assertNull(saved.getNextAttemptAt());
                    assertNull(saved.getLastError());
                } else {
                    assertEquals(WebhookStatus.RETRY, saved.getStatus());
                    assertNotNull(saved.getNextAttemptAt());
                    assertTrue(saved.getNextAttemptAt().isAfter(before.minusMillis(1)));
                    assertTrue(saved.getNextAttemptAt().isBefore(after.plusSeconds(2)) || saved.getNextAttemptAt().isAfter(before),
                            "next attempt should be scheduled in the future");
                    assertNotNull(saved.getLastError());
                    assertTrue(saved.getLastError().contains("HTTP "));
                }
                assertNotNull(saved.getLastAttemptAt());
                return true;
            }));
        }

        @Test
        void shouldTreatNetworkErrorsAs599AndRetry() {
            // given
            WebhookEvent ev = new WebhookEvent();
            ev.setId("e2");
            // Port 1 is almost certainly closed -> connection refused
            ev.setTargetUrl("http://127.0.0.1:1/unreachable");
            ev.setPayloadJson("{}");
            ev.setStatus(WebhookStatus.PENDING);
            ev.setAttempts(0);
            ev.setCreatedAt(Instant.now().minusSeconds(5));
            ev.setNextAttemptAt(Instant.now().minusSeconds(1));

            given(repository.findByStatusInAndNextAttemptAtLessThanEqual(anyList(), any())).willReturn(List.of(ev));
            given(repository.save(any(WebhookEvent.class))).willAnswer(inv -> inv.getArgument(0));

            WebhookDispatcherService service = new WebhookDispatcherService(
                    repository, httpClient, true, 3, 50, 1_000);

            // when
            service.dispatchLoop();

            // then
            then(repository).should().save(argThat(saved -> {
                assertEquals(WebhookStatus.RETRY, saved.getStatus());
                assertEquals(1, saved.getAttempts());
                assertNotNull(saved.getNextAttemptAt());
                assertTrue(saved.getLastError().contains("599"));
                return true;
            }));
        }

        @Test
        void shouldMarkFailedWhenReachingMaxAttempts() {
            // given
            WebhookEvent ev = new WebhookEvent();
            ev.setId("e3");
            ev.setTargetUrl("http://localhost:" + port + "/b400"); // non-2xx
            ev.setPayloadJson("{}");
            ev.setStatus(WebhookStatus.RETRY);
            ev.setAttempts(2); // will become 3 (max)
            ev.setCreatedAt(Instant.now().minusSeconds(10));
            ev.setNextAttemptAt(Instant.now().minusSeconds(1));

            given(repository.findByStatusInAndNextAttemptAtLessThanEqual(anyList(), any())).willReturn(List.of(ev));
            given(repository.save(any(WebhookEvent.class))).willAnswer(inv -> inv.getArgument(0));

            WebhookDispatcherService service = new WebhookDispatcherService(
                    repository, httpClient, true, 3, 10, 100);

            // when
            service.dispatchLoop();

            // then
            then(repository).should().save(argThat(saved -> {
                assertEquals(3, saved.getAttempts());
                assertEquals(WebhookStatus.FAILED, saved.getStatus());
                assertNull(saved.getNextAttemptAt());
                assertNotNull(saved.getLastError());
                return true;
            }));
        }
    }
}
