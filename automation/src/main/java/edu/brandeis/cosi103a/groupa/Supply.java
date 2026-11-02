package edu.brandeis.cosi103a.groupa;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * Shared market of available cards to purchase.
 *
 * <p>Tracks remaining counts for each card type and provides purchase validation.
 */
public final class Supply {
    private Map<Card, Integer> cardCounts;
    private Card gameEndCard; // The card pile that triggers game end when empty

    /**
     * Constructs a supply with standard card piles.
     */
    public Supply() {
        cardCounts = new HashMap<>();
        
        // Initialize standard card piles (example cards)
        AutomationCard bitcoin = new AutomationCard("Bitcoin", 5, 1);
        AutomationCard method = new AutomationCard("Method", 6, 2);
        CryptoCard ethereum = new CryptoCard("Ethereum", 8, 3);
        CryptoCard coin = new CryptoCard("Coin", 3, 1);
        
        // Set up initial counts
        cardCounts.put(bitcoin, 10);
        cardCounts.put(method, 10);
        cardCounts.put(ethereum, 10);
        cardCounts.put(coin, 10);
        
        // Game ends when one of these piles is empty (e.g., Bitcoin)
        gameEndCard = bitcoin;
    }

    /**
     * Checks whether the given card is available to buy (count > 0).
     *
     * @param card the card type the player wants to buy
     * @return true if at least one copy remains
     */
    public boolean canBuy(Card card) {
        return cardCounts.getOrDefault(card, 0) > 0;
    }

    /**
     * Removes one copy of the given card from the supply.
     *
     * @param card the card type to remove
     * @throws IllegalStateException if the card is not available
     */
    public void take(Card card) {
        if (!canBuy(card)) {
            throw new IllegalStateException("Card " + card.name() + " is not available");
        }
        cardCounts.put(card, cardCounts.get(card) - 1);
    }

    /**
     * Determines whether the game end condition is met.
     *
     * @return true if the required supply pile is empty (per IP2 rules)
     */
    public boolean isGameOver() {
        return !canBuy(gameEndCard);
    }

    /**
     * Returns the list of card types the player can afford with the given coins.
     *
     * @param coins available coins
     * @return list of card types with cost <= coins and count > 0
     */
    public List<Card> purchasableUpTo(int coins) {
        List<Card> purchasable = new ArrayList<>();
        for (Card card : cardCounts.keySet()) {
            if (canBuy(card) && card.cost() <= coins) {
                purchasable.add(card);
            }
        }
        return purchasable;
    }

    /**
     * Checks if the supply is empty (game end condition).
     *
     * @return true if the supply is empty
     */
    public boolean isEmpty() {
        return isGameOver();
    }
}
