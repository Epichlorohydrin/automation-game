




package edu.brandeis.cosi103a.groupa.network;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import edu.brandeis.cosi.atg.decisions.Decision;
import edu.brandeis.cosi.atg.event.Event;
import edu.brandeis.cosi.atg.state.GameState;

public record DecisionRequest(
    @JsonProperty("state") GameState state,
    @JsonProperty("options") ImmutableList<Decision> options,
    @JsonProperty("reason") Event reason,
    @JsonProperty("player_uuid") String playerUuid
) {
}
