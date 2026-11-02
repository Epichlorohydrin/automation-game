package edu.brandeis.cosi103a.groupa.server;

import edu.brandeis.cosi.atg.decisions.Decision;
import edu.brandeis.cosi103a.groupa.network.DecisionRequest;
import edu.brandeis.cosi103a.groupa.network.DecisionResponse;
import edu.brandeis.cosi103a.groupa.network.LogEventRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping
public class PlayerServerController {

    private final PlayerDecisionService playerDecisionService;

    public PlayerServerController(PlayerDecisionService playerDecisionService) {
        this.playerDecisionService = playerDecisionService;
    }

    /**
     * Validates and processes a remote decision request.
     *
     * @param request inbound decision request
     * @return response containing the selected decision
     */
    @PostMapping("/decide")
    public ResponseEntity<DecisionResponse> decide(@RequestBody DecisionRequest request) {
        validateDecisionRequest(request);
        Decision decision = playerDecisionService.decide(
            request.state(),
            request.options(),
            request.reason(),
            request.playerUuid()
        );
        return ResponseEntity.ok(new DecisionResponse(decision));
    }

    /**
     * Validates and processes a remote event log request.
     *
     * @param request inbound log event request
     * @return empty successful response
     */
    @PostMapping("/log-event")
    public ResponseEntity<Void> logEvent(@RequestBody LogEventRequest request) {
        validateLogEventRequest(request);
        playerDecisionService.logEvent(request.state(), request.event(), request.playerUuid());
        return ResponseEntity.ok().build();
    }

    /**
     * Validates required fields for a decision request.
     *
     * @param request inbound decision request
     */
    private static void validateDecisionRequest(DecisionRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }
        if (request.options() == null || request.options().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "options must be non-empty");
        }
        if (request.playerUuid() == null || request.playerUuid().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "player_uuid is required");
        }
    }

    /**
     * Validates required fields for a log event request.
     *
     * @param request inbound log event request
     */
    private static void validateLogEventRequest(LogEventRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }
        if (request.playerUuid() == null || request.playerUuid().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "player_uuid is required");
        }
    }
}
