package edu.brandeis.cosi103a.groupa.server;

import com.google.common.collect.ImmutableList;
import edu.brandeis.cosi.atg.decisions.Decision;
import edu.brandeis.cosi.atg.event.Event;
import edu.brandeis.cosi.atg.player.Player;
import edu.brandeis.cosi.atg.state.GameState;
import edu.brandeis.cosi103a.groupa.players.BigMoneyPlayerNew;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PlayerDecisionService {

    private final Player delegate;

    public PlayerDecisionService() {
        this.delegate = new BigMoneyPlayerNew();
    }

    public Decision decide(GameState state, ImmutableList<Decision> options, Event reason, String playerUuid) {
        return delegate.makeDecision(state, options, Optional.ofNullable(reason));
    }

    public void logEvent(GameState state, Event event, String playerUuid) {
        // Hook for remote player-specific logging; no-op by default.
    }
}
