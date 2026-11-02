package edu.brandeis.cosi103a.groupa.players;

import com.google.common.collect.ImmutableList;
import edu.brandeis.cosi.atg.decisions.Decision;
import edu.brandeis.cosi.atg.event.Event;
import edu.brandeis.cosi.atg.event.GameObserver;
import edu.brandeis.cosi.atg.player.Player;
import edu.brandeis.cosi.atg.state.GameState;
import edu.brandeis.cosi103a.groupa.network.DecisionRequest;
import edu.brandeis.cosi103a.groupa.network.DecisionResponse;
import edu.brandeis.cosi103a.groupa.network.LogEventRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class NetworkPlayer implements Player {

    private final String name;
    private final String playerUuid;
    private final String serverBaseUrl;
    private final RestTemplate restTemplate;

    public NetworkPlayer(String name, String serverBaseUrl) {
        this(name, serverBaseUrl, UUID.randomUUID().toString(), new RestTemplate());
    }

    public NetworkPlayer(String name, String serverBaseUrl, String playerUuid, RestTemplate restTemplate) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.serverBaseUrl = normalizeBaseUrl(serverBaseUrl);
        this.playerUuid = Objects.requireNonNull(playerUuid, "playerUuid cannot be null");
        this.restTemplate = Objects.requireNonNull(restTemplate, "restTemplate cannot be null");
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    /**
     * Sends a decision request to the remote server and returns the response decision.
     *
     * @param state the current visible game state
     * @param options the legal decisions available this turn
     * @param lastEvent the most recent event, if any
     * @return the selected decision returned by the remote service
     */
    public Decision makeDecision(GameState state, ImmutableList<Decision> options, Optional<Event> lastEvent) {
        if (options == null || options.isEmpty()) {
            throw new IllegalArgumentException("options must be non-empty");
        }

        DecisionRequest request = new DecisionRequest(state, options, lastEvent.orElse(null), playerUuid);
        String url = serverBaseUrl + "/decide";

        ResponseEntity<DecisionResponse> response;
        try {
            response = restTemplate.postForEntity(url, request, DecisionResponse.class);
        } catch (RestClientException e) {
            throw new IllegalStateException("Failed to call remote /decide endpoint", e);
        }

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null || response.getBody().decision() == null) {
            throw new IllegalStateException("Remote /decide endpoint returned invalid response");
        }

        return response.getBody().decision();
    }

    /**
     * Backward-compatible overload that delegates to the full decision method.
     *
     * @param state the current visible game state
     * @param options the legal decisions available this turn
     * @return the selected decision
     */
    public Decision makeDecision(GameState state, ImmutableList<Decision> options) {
        return makeDecision(state, options, Optional.empty());
    }

    @Override
    public Optional<GameObserver> getObserver() {
        return Optional.of((gameState, event) -> {
            LogEventRequest request = new LogEventRequest(gameState, event, playerUuid);
            String url = serverBaseUrl + "/log-event";
            try {
                restTemplate.postForEntity(url, request, Void.class);
            } catch (RestClientException e) {
                throw new IllegalStateException("Failed to call remote /log-event endpoint", e);
            }
        });
    }

    /**
     * Normalizes the base URL by trimming and removing a trailing slash.
     *
     * @param url raw server base URL
     * @return normalized base URL
     */
    private static String normalizeBaseUrl(String url) {
        Objects.requireNonNull(url, "serverBaseUrl cannot be null");
        String trimmed = url.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("serverBaseUrl cannot be blank");
        }
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }
}
