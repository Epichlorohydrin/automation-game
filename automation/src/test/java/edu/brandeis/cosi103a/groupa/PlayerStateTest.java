package edu.brandeis.cosi103a.groupa;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import java.util.Random;

/**
 * Unit tests for PlayerState class.
 * Tests deck management, drawing, playing, gaining cards, and scoring.
 */
public class PlayerStateTest {
    
    private PlayerState player;
    private Random rng;

    @Before
    public void setUp() {
        player = new PlayerState();
        rng = new Random(42); // Fixed seed for deterministic tests
    }

    /**
     * Test that player initializes with starter deck.
     */
    @Test
    public void testPlayerInitialization() {
        assertNotNull("Player should be initialized", player);
        // Starter deck should have 10 Coin cards
        assertEquals("Starter deck should have cards", 10, player.drawPile().size());
    }

    /**
     * Test that draw adds cards to hand.
     */
    @Test
    public void testDrawCardsToHand() {
        int initialHandSize = player.hand().size();
        player.draw(3, rng);
        assertEquals("Hand should have 3 cards after drawing 3", 
                     initialHandSize + 3, player.hand().size());
    }

    /**
     * Test that draw reshuffles discard pile when draw pile is empty.
     */
    @Test
    public void testDrawReshufflesDiscardPile() {
        // Draw all cards from draw pile (10 cards)
        player.draw(10, rng);
        
        // Now add some cards to discard pile
        player.gain(new CryptoCard("Test", 1, 1));
        player.gain(new CryptoCard("Test", 1, 1));
        
        // Draw more cards - should reshuffle discard pile
        player.draw(2, rng);
        
        // Should have cards from reshuffled discard pile
        assertTrue("Should be able to draw from reshuffled discard pile", 
                   player.hand().size() >= 2);
    }

    /**
     * Test that playAllCryptoToCoins plays all crypto cards and returns coins.
     */
    @Test
    public void testPlayAllCryptoToCoins() {
        // Clear hand and add known crypto cards
        player.hand().clear();
        player.hand().add(new CryptoCard("Bitcoin", 3, 2));
        player.hand().add(new CryptoCard("Ethereum", 4, 3));
        
        int coins = player.playAllCryptoToCoins();
        
        assertEquals("Should get 5 coins from playing 2+3", 5, coins);
        assertEquals("Hand should be empty after playing all crypto", 0, player.hand().size());
        assertEquals("In-play should have 2 cards", 2, player.inPlay().size());
    }

    /**
     * Test that gain adds card to discard pile.
     */
    @Test
    public void testGainAddsCardToDiscardPile() {
        int initialDiscardSize = player.discardPile().size();
        Card card = new AutomationCard("Bitcoin", 5, 1);
        player.gain(card);
        
        assertEquals("Discard pile should have one more card", 
                     initialDiscardSize + 1, player.discardPile().size());
    }

    /**
     * Test that cleanupAndRedraw moves all cards to discard and redraws 5.
     */
    @Test
    public void testCleanupAndRedraw() {
        // Setup: put some cards in hand and in-play
        player.hand().clear();
        player.hand().add(new CryptoCard("Test1", 1, 1));
        player.hand().add(new CryptoCard("Test2", 1, 1));
        
        player.inPlay().clear();
        player.inPlay().add(new CryptoCard("Test3", 1, 1));
        
        player.cleanupAndRedraw(rng);
        
        assertEquals("Hand should have 5 cards after cleanup and redraw", 5, player.hand().size());
        assertEquals("In-play should be empty after cleanup", 0, player.inPlay().size());
    }

    /**
     * Test that totalAutomationPoints counts AP from all piles.
     */
    @Test
    public void testTotalAutomationPoints() {
        // Clear and add known automation cards
        player.drawPile().clear();
        player.drawPile().add(new AutomationCard("Method", 6, 2));
        
        player.discardPile().clear();
        player.discardPile().add(new AutomationCard("Advanced", 7, 3));
        
        player.hand().clear();
        player.hand().add(new AutomationCard("Basic", 5, 1));
        
        player.inPlay().clear();
        player.inPlay().add(new AutomationCard("Premium", 8, 4));
        
        int totalAP = player.totalAutomationPoints();
        assertEquals("Total AP should be 2+3+1+4 = 10", 10, totalAP);
    }

    /**
     * Test that totalAutomationPoints returns 0 when no automation cards.
     */
    @Test
    public void testTotalAutomationPointsWithNoCryptoCards() {
        // Clear all piles and add only crypto cards
        player.drawPile().clear();
        player.drawPile().add(new CryptoCard("Coin", 3, 1));
        
        player.discardPile().clear();
        player.hand().clear();
        player.inPlay().clear();
        
        int totalAP = player.totalAutomationPoints();
        assertEquals("Total AP should be 0 with no automation cards", 0, totalAP);
    }

    /**
     * Test that draw does not exceed available cards.
     */
    @Test
    public void testDrawDoesNotExceedAvailableCards() {
        player.draw(20, rng); // Try to draw more than available (10 starter cards)
        
        // Should have at most 10 cards in hand
        assertTrue("Should not have more than 10 cards from starter deck", 
                   player.hand().size() <= 10);
    }
}
