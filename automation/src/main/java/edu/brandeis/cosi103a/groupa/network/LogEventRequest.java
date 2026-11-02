package edu.brandeis.cosi103a.groupa.network;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.brandeis.cosi.atg.event.Event;
import edu.brandeis.cosi.atg.state.GameState;

public record LogEventRequest(
    @JsonProperty("state") GameState state,
    @JsonProperty("event") Event event,
    @JsonProperty("player_uuid") String playerUuid
) {
}
