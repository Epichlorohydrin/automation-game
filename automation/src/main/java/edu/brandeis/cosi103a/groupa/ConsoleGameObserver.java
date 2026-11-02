package edu.brandeis.cosi103a.groupa;

import edu.brandeis.cosi.atg.event.Event;
import edu.brandeis.cosi.atg.event.GameObserver;
import edu.brandeis.cosi.atg.state.GameState;

/**
 * Simple observer that prints game events for local debugging.
 */
public final class ConsoleGameObserver implements GameObserver {

    @Override
    public void notifyEvent(GameState state, Event event) {
        System.out.println("[EVENT] " + event.getDescription() + " | player=" + state.currentPlayerName()
            + " phase=" + state.phase());
    }
}
