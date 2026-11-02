package edu.brandeis.cosi103a.groupa;

import edu.brandeis.cosi.atg.cards.Card;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Shared helper for choosing the action card set used in a game.
 */
final class ActionCards {

    private static final List<Card.Type> ACTION_CARD_TYPES = List.of(
        Card.Type.BACKLOG,
        Card.Type.DAILY_SCRUM,
        Card.Type.IPO,
        Card.Type.HACK,
        Card.Type.MONITORING,
        Card.Type.TECH_DEBT,
        Card.Type.REFACTOR,
        Card.Type.PARALLELIZATION,
        Card.Type.CODE_REVIEW,
        Card.Type.EVERGREEN_TEST,
        Card.Type.SPRINT_PLANNING,
        Card.Type.MERGE_CONFLICT,
        Card.Type.RANSOMWARE,
        Card.Type.DEPLOYMENT_PIPELINE,
        Card.Type.UNIT_TEST
    );

    private ActionCards() {
    }

    /**
     * Selects ten unique action card types to use for a game.
     *
     * @param rng randomness source for shuffling
     * @return an immutable list of ten action card types
     */
    static List<Card.Type> chooseActionCards(Random rng) {
        List<Card.Type> actionCards = new ArrayList<>(ACTION_CARD_TYPES);
        Collections.shuffle(actionCards, rng);
        return List.copyOf(actionCards.subList(0, 10));
    }
}
