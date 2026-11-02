package edu.brandeis.cosi103a.groupa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import edu.brandeis.cosi.atg.cards.Card;
import edu.brandeis.cosi.atg.decisions.BuyDecision;
import edu.brandeis.cosi.atg.decisions.Decision;
import edu.brandeis.cosi.atg.event.Event;
import edu.brandeis.cosi103a.groupa.network.DecisionResponse;
import edu.brandeis.cosi103a.groupa.players.NetworkPlayer;
import org.junit.Test;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class NetworkPlayerTest {

    @Test
    public void makeDecision_postsDecisionRequestAndReturnsServerDecision() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        AtomicReference<String> capturedBody = new AtomicReference<>();
        AtomicBoolean decideCalled = new AtomicBoolean(false);

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/decide", exchange -> handleDecide(exchange, mapper, capturedBody, decideCalled));
        server.start();

        try {
            int port = server.getAddress().getPort();
            NetworkPlayer player = new NetworkPlayer("NetPlayer", "http://localhost:" + port, "p-1", new RestTemplate());

            ImmutableList<Decision> options = ImmutableList.of(
                new BuyDecision(Card.Type.METHOD),
                new BuyDecision(Card.Type.BITCOIN)
            );

            Decision chosen = player.makeDecision(null, options, Optional.<Event>empty());

            assertEquals(new BuyDecision(Card.Type.METHOD), chosen);
            assertTrue(decideCalled.get());
            String sentBody = capturedBody.get();
            assertNotNull(sentBody);
            assertTrue(sentBody.contains("\"player_uuid\":\"p-1\""));
            assertTrue(sentBody.contains("\"options\""));
        } finally {
            server.stop(0);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void makeDecision_throwsOnEmptyOptions() {
        NetworkPlayer player = new NetworkPlayer("NetPlayer", "http://localhost:1", "p-1", new RestTemplate());
        player.makeDecision(null, ImmutableList.of(), Optional.empty());
    }

    @Test
    public void makeDecision_throwsWhenServerReturnsInvalidBody() throws Exception {
        AtomicBoolean called = new AtomicBoolean(false);
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/decide", exchange -> {
            try (exchange) {
                called.set(true);
                byte[] payload = "{}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, payload.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(payload);
                }
            }
        });
        server.start();

        try {
            int port = server.getAddress().getPort();
            NetworkPlayer player = new NetworkPlayer("NetPlayer", "http://localhost:" + port, "p-1", new RestTemplate());
            ImmutableList<Decision> options = ImmutableList.of(new BuyDecision(Card.Type.METHOD));

            try {
                player.makeDecision(null, options, Optional.empty());
                fail("Expected IllegalStateException");
            } catch (IllegalStateException expected) {
                assertTrue(called.get());
            }
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void makeDecision_throwsWhenServerUnavailable() throws Exception {
        int unusedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            unusedPort = socket.getLocalPort();
        }

        NetworkPlayer player = new NetworkPlayer("NetPlayer", "http://localhost:" + unusedPort, "p-1", new RestTemplate());
        ImmutableList<Decision> options = ImmutableList.of(new BuyDecision(Card.Type.METHOD));

        try {
            player.makeDecision(null, options, Optional.empty());
            fail("Expected IllegalStateException");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("/decide"));
        }
    }

    @Test
    public void getObserver_postsLogEvent() throws Exception {
        AtomicReference<String> capturedBody = new AtomicReference<>();
        AtomicBoolean logCalled = new AtomicBoolean(false);

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/log-event", exchange -> {
            try (exchange) {
                byte[] body = exchange.getRequestBody().readAllBytes();
                capturedBody.set(new String(body, StandardCharsets.UTF_8));
                logCalled.set(true);
                exchange.sendResponseHeaders(200, -1);
            }
        });
        server.start();

        try {
            int port = server.getAddress().getPort();
            NetworkPlayer player = new NetworkPlayer("NetPlayer", "http://localhost:" + port, "p-1", new RestTemplate());
            assertTrue(player.getObserver().isPresent());

            player.getObserver().get().notifyEvent(null, null);

            assertTrue(logCalled.get());
            String sent = capturedBody.get();
            assertNotNull(sent);
            assertTrue(sent.contains("\"player_uuid\":\"p-1\""));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void baseUrl_withTrailingSlash_stillCallsDecide() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        AtomicBoolean decideCalled = new AtomicBoolean(false);
        AtomicReference<String> capturedBody = new AtomicReference<>();

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/decide", exchange -> handleDecide(exchange, mapper, capturedBody, decideCalled));
        server.start();

        try {
            int port = server.getAddress().getPort();
            NetworkPlayer player = new NetworkPlayer("NetPlayer", "http://localhost:" + port + "/", "p-1", new RestTemplate());
            ImmutableList<Decision> options = ImmutableList.of(new BuyDecision(Card.Type.METHOD));

            Decision chosen = player.makeDecision(null, options, Optional.empty());
            assertEquals(new BuyDecision(Card.Type.METHOD), chosen);
            assertTrue(decideCalled.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void getName_returnsConfiguredName() {
        NetworkPlayer player = new NetworkPlayer("RemotePriority", "http://localhost:1", "p-1", new RestTemplate());
        assertEquals("RemotePriority", player.getName());
    }

    private static void handleDecide(HttpExchange exchange,
                                     ObjectMapper mapper,
                                     AtomicReference<String> capturedBody,
                                     AtomicBoolean decideCalled) throws IOException {
        try (exchange) {
            byte[] requestBytes = exchange.getRequestBody().readAllBytes();
            capturedBody.set(new String(requestBytes, StandardCharsets.UTF_8));
            decideCalled.set(true);

            DecisionResponse response = new DecisionResponse(new BuyDecision(Card.Type.METHOD));
            byte[] payload = mapper.writeValueAsBytes(response);

            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, payload.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(payload);
            }
        }
    }
}
