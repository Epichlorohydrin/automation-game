package edu.brandeis.cosi103a.groupa.players;

import com.google.common.collect.ImmutableList;
import edu.brandeis.cosi.atg.cards.Card;
import edu.brandeis.cosi.atg.decisions.BuyDecision;
import edu.brandeis.cosi.atg.decisions.Decision;
import edu.brandeis.cosi.atg.event.Event;
import edu.brandeis.cosi.atg.event.GameObserver;
import edu.brandeis.cosi.atg.player.Player;
import edu.brandeis.cosi.atg.state.GameState;

import java.util.Collection;
import java.util.Optional;

/**
 * Implements a Big Money player that prioritizes Framework purchases when the expected hand value supports it.
 */
public class BigMoneyAdv1Player implements Player {

    private static final int HAND_SIZE = 5;

    /**
     * Returns the player name used by the game engine.
     *
     * @return the player name
     */
    @Override
    public String getName() {
        return "BigMoneyAdv1";
    }

    /**
     * Chooses one legal decision from the available options.
     *
     * @param state the current visible game state
     * @param options the legal decisions available this turn
     * @param lastEvent the most recent event, if any
     * @return the selected decision
     */
    @Override
    public Decision makeDecision(GameState state, ImmutableList<Decision> options, Optional<Event> lastEvent) {
        if (options == null || options.isEmpty()) {
            throw new IllegalArgumentException("options must be non-empty");
        }

        if (options.size() == 1) {
            return options.get(0);
        }

        Optional<Decision> buyChoice = chooseBuyDecision(state, options);
        if (buyChoice.isPresent()) {
            return buyChoice.get();
        }

        return chooseDefaultNonBuyDecision(options);
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

    /**
     * Chooses the highest-priority buy decision available for the current turn.
     *
     * @param state the current game state
     * @param options the legal decisions available this turn
     * @return an optional buy decision, or empty if none is available
     */
    private Optional<Decision> chooseBuyDecision(GameState state,
                                                 ImmutableList<Decision> options) {
        if (shouldBuyFrameworkNow(state)) {
            Optional<Decision> frameworkBuy = findBuyDecisionFor(Card.Type.FRAMEWORK, options);
            if (frameworkBuy.isPresent()) {
                return frameworkBuy;
            }
        }

        return chooseBestMoneyCard(state, options);
    }

    /**
     * Determines whether Framework should be purchased on this turn.
     *
     * @param state the current game state
     * @return true if Framework should be bought now
     */
    private boolean shouldBuyFrameworkNow(GameState state) {
        if (state == null) {
            return false;
        }

        int frameworkCost = Card.Type.FRAMEWORK.cost();
        int currentMoney = state.spendableMoney();

        if (currentMoney < frameworkCost) {
            return false;
        }

        double expectedMoney = expectedMoneyPerHand(state);
        return expectedMoney >= frameworkCost;
    }

    /**
     * Estimates the expected money value of a full hand.
     *
     * @param state the current game state
     * @return the estimated money value of a hand
     */
    private double expectedMoneyPerHand(GameState state) {
        if (state == null || state.currentPlayerHand() == null) {
            return 0.0;
        }

        Collection<Card> allCards = state.currentPlayerHand().getAllCards();
        if (allCards.isEmpty()) {
            return 0.0;
        }

        double moneyPerCard = expectedMoneyPerCard(allCards);
        return moneyPerCard * HAND_SIZE;
    }

    /**
     * Computes the expected money value per card in the deck.
     *
     * @param cards a collection representing known deck cards
     * @return the average money value per card
     */
    private double expectedMoneyPerCard(Collection<Card> cards) {
        if (cards.isEmpty()) {
            return 0.0;
        }

        int totalMoney = 0;
        for (Card card : cards) {
            totalMoney += card.value();
        }

        return (double) totalMoney / cards.size();
    }

    /**
     * Finds a buy decision for a specific card type.
     *
      * @param cardType the card type to search for
      * @param options the legal decisions available this turn
      * @return an optional matching buy decision
     */
    private Optional<Decision> findBuyDecisionFor(Card.Type cardType,
                                                   ImmutableList<Decision> options) {
        for (Decision d : options) {
            if (d instanceof BuyDecision) {
                BuyDecision buyDec = (BuyDecision) d;
                if (buyDec.cardType() == cardType) {
                    return Optional.of(d);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Chooses the highest-value money card the player can afford.
     *
     * @param state the current game state
     * @param options the legal decisions available this turn
     * @return an optional buy decision for the best available money card
     */
    private Optional<Decision> chooseBestMoneyCard(GameState state,
                                                    ImmutableList<Decision> options) {
        Optional<Decision> dogecoin = findBuyDecisionFor(Card.Type.DOGECOIN, options);
        if (dogecoin.isPresent()) {
            return dogecoin;
        }

        Optional<Decision> ethereum = findBuyDecisionFor(Card.Type.ETHEREUM, options);
        if (ethereum.isPresent()) {
            return ethereum;
        }

        Optional<Decision> bitcoin = findBuyDecisionFor(Card.Type.BITCOIN, options);
        if (bitcoin.isPresent()) {
            return bitcoin;
        }

        return Optional.empty();
    }

    /**
     * Chooses a default non-buy decision when no buy is possible.
     *
     * @param options all legal decisions
     * @return a default non-buy decision
     */
    private Decision chooseDefaultNonBuyDecision(ImmutableList<Decision> options) {
        return options.get(0);
    }

    @Override
    public Optional<GameObserver> getObserver() {
        return Optional.empty();
    }
}
