package edu.brandeis.cosi103a.groupa;

import java.util.Deque;
import java.util.List;
import java.util.Random;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Represents a player's full deck state: draw pile, discard pile, hand, and in-play area.
 *
 * <p>Provides operations for drawing, playing currency, gaining purchased cards,
 * cleaning up at end of turn, and scoring the deck.
 */
public final class PlayerState {
    private Deque<Card> drawPile;
    private Deque<Card> discardPile;
    private List<Card> hand;
    private List<Card> inPlay;

    /**
     * Constructs a player state with a standard starter deck.
     */
    public PlayerState() {
        drawPile = new ArrayDeque<>();
        discardPile = new ArrayDeque<>();
        hand = new ArrayList<>();
        inPlay = new ArrayList<>();
        
        // Initialize with starter deck (example: 10 Coin cards)
        for (int i = 0; i < 10; i++) {
            drawPile.push(new CryptoCard("Coin", 3, 1));
        }
    }

    /**
     * Draws up to {@code n} cards into the player's hand.
     *
     * <p>If the draw pile is empty and the discard pile is not empty, the discard pile is
     * shuffled to form a new draw pile.
     *
     * @param n number of cards to draw
     * @param rng randomness source for shuffling (inject for deterministic tests)
     */
    public void draw(int n, Random rng) {
        for (int i = 0; i < n; i++) {
            // If draw pile is empty, shuffle discard pile into draw pile
            if (drawPile.isEmpty() && !discardPile.isEmpty()) {
                List<Card> discardList = new ArrayList<>(discardPile);
                Collections.shuffle(discardList, rng);
                drawPile.addAll(discardList);
                discardPile.clear();
            }
            
            // Draw a card if available
            if (!drawPile.isEmpty()) {
                hand.add(drawPile.pop());
            }
        }
    }

    /**
     * Plays all cryptocurrency cards from hand, moving them to in-play, and computes coins.
     *
     * @return total coins produced by crypto cards played from hand
     */
    public int playAllCryptoToCoins() {
        int totalCoins = 0;
        List<Card> cardsToRemove = new ArrayList<>();
        
        for (Card card : hand) {
            if (card instanceof CryptoCard) {
                CryptoCard crypto = (CryptoCard) card;
                totalCoins += crypto.coinValue();
                inPlay.add(card);
                cardsToRemove.add(card);
            }
        }
        
        // Remove played cards from hand
        for (Card card : cardsToRemove) {
            hand.remove(card);
        }
        
        return totalCoins;
    }

    /**
     * Adds a bought card to the player's discard pile.
     *
     * @param bought the card gained from the supply
     */
    public void gain(Card bought) {
        discardPile.push(bought);
    }

    /**
     * Cleanup phase: discards all remaining cards in hand and all in-play cards,
     * then draws a new 5-card hand.
     *
     * @param rng randomness source for shuffling (inject for deterministic tests)
     */
    public void cleanupAndRedraw(Random rng) {
        // Discard all cards in hand
        discardPile.addAll(hand);
        hand.clear();
        
        // Discard all in-play cards
        discardPile.addAll(inPlay);
        inPlay.clear();
        
        // Draw a new 5-card hand
        draw(5, rng);
    }

    /**
     * Computes the player's total Automation Points (AP) across their entire deck state.
     *
     * @return total AP in draw pile + discard pile + hand + in-play
     */
    public int totalAutomationPoints() {
        int totalAP = 0;
        
        // Count AP in draw pile
        for (Card card : drawPile) {
            if (card instanceof AutomationCard) {
                AutomationCard automation = (AutomationCard) card;
                totalAP += automation.apValue();
            }
        }
        
        // Count AP in discard pile
        for (Card card : discardPile) {
            if (card instanceof AutomationCard) {
                AutomationCard automation = (AutomationCard) card;
                totalAP += automation.apValue();
            }
        }
        
        // Count AP in hand
        for (Card card : hand) {
            if (card instanceof AutomationCard) {
                AutomationCard automation = (AutomationCard) card;
                totalAP += automation.apValue();
            }
        }
        
        // Count AP in play
        for (Card card : inPlay) {
            if (card instanceof AutomationCard) {
                AutomationCard automation = (AutomationCard) card;
                totalAP += automation.apValue();
            }
        }
        
        return totalAP;
    }

    // Getters kept package-private for testing
    Deque<Card> drawPile() { 
        return drawPile; 
    }
    
    Deque<Card> discardPile() { 
        return discardPile; 
    }
    
    List<Card> hand() { 
        return hand; 
    }
    
    List<Card> inPlay() { 
        return inPlay; 
    }
}
