package edu.brandeis.cosi103a.groupa;

import edu.brandeis.cosi.atg.cards.Card;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Mutable per-player deck state used by {@link GameEngine} and action-card resolution.
 */
final class DeckState {
    final List<Card> drawPile = new ArrayList<>();
    final List<Card> discardPile = new ArrayList<>();
    final List<Card> playedHand = new ArrayList<>();
    final List<Card> unplayedHand = new ArrayList<>();
    int actions;
    int money;
    int buys;
    int turnCostReduction;

    /**
     * Returns all cards from every pile in this deck state.
     *
     * @return combined collection of all cards owned by the player
     */
    Collection<Card> allCards() {
        List<Card> allCards = new ArrayList<>(drawPile);
        allCards.addAll(discardPile);
        allCards.addAll(playedHand);
        allCards.addAll(unplayedHand);
        return allCards;
    }
}
