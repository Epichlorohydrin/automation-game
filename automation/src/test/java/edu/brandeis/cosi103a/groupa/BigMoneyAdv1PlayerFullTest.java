package edu.brandeis.cosi103a.groupa;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import edu.brandeis.cosi.atg.cards.Card;
import edu.brandeis.cosi.atg.decisions.BuyDecision;
import edu.brandeis.cosi.atg.decisions.Decision;
import edu.brandeis.cosi.atg.decisions.EndPhaseDecision;
import edu.brandeis.cosi.atg.state.CardStacks;
import edu.brandeis.cosi.atg.state.GameState;
import edu.brandeis.cosi.atg.state.Hand;
import edu.brandeis.cosi103a.groupa.players.BigMoneyAdv1Player;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Comprehensive test suite for BigMoneyAdv1Player enforcing all checklist requirements.
 *
 * <p>Tests verify:</p>
 * <ul>
 *   <li>Class-level correctness: return valid decisions, never null, proper strategy</li>
 *   <li>Method-level behavior: each helper method meets its specification</li>
 *   <li>Advanced Strategy 1 core logic: delay Framework purchase until deck is strong</li>
 * </ul>
 */
public class BigMoneyAdv1PlayerFullTest {

    private BigMoneyAdv1Player player;

    @Before
    public void setUp() {
        player = new BigMoneyAdv1Player();
    }

    // ========================================================================
    // CLASS-LEVEL CORRECTNESS TESTS
    // ========================================================================

    @Test
    public void classLevel_alwaysReturnsDecisionFromOptions() {
        Decision d1 = new EndPhaseDecision(GameState.TurnPhase.MONEY);
        Decision d2 = new BuyDecision(Card.Type.BITCOIN);
        ImmutableList<Decision> options = ImmutableList.of(d1, d2);

        Decision chosen = player.makeDecision(null, options);

        assertTrue("Must return one of the provided options",
                   options.contains(chosen));
    }

    @Test
    public void classLevel_neverReturnsNull() {
        Decision only = new EndPhaseDecision(GameState.TurnPhase.BUY);
        ImmutableList<Decision> options = ImmutableList.of(only);

        Decision chosen = player.makeDecision(null, options);

        assertNotNull("Must never return null", chosen);
    }

    @Test
    public void classLevel_buysFrameworkOnlyWhenAdvancedConditionPasses() {
        // Weak deck: expected money = 1 (all Bitcoins), cannot afford Framework mindset
        ImmutableCollection<Card> weakDeck = ImmutableList.of(
            new Card(Card.Type.BITCOIN, 1),
            new Card(Card.Type.BITCOIN, 2),
            new Card(Card.Type.BITCOIN, 3),
            new Card(Card.Type.BITCOIN, 4),
            new Card(Card.Type.BITCOIN, 5)
        );
        Hand weakHand = new Hand(ImmutableList.of(), weakDeck);
        GameState weakState = createGameState(weakHand, 8); // Has 8 money but weak deck

        Decision buyFramework = new BuyDecision(Card.Type.FRAMEWORK);
        Decision buyEthereum = new BuyDecision(Card.Type.ETHEREUM);
        ImmutableList<Decision> options = ImmutableList.of(buyFramework, buyEthereum);

        Decision chosen = player.makeDecision(weakState, options);

        assertSame("Should NOT buy Framework when deck is weak", buyEthereum, chosen);
    }

    @Test
    public void classLevel_buysHighestValueMoneyWhenNotBuyingFramework() {
        Decision buyBitcoin = new BuyDecision(Card.Type.BITCOIN);   // value 1
        Decision buyEthereum = new BuyDecision(Card.Type.ETHEREUM); // value 2
        Decision buyDogecoin = new BuyDecision(Card.Type.DOGECOIN); // value 3
        ImmutableList<Decision> options = ImmutableList.of(buyBitcoin, buyEthereum, buyDogecoin);

        Decision chosen = player.makeDecision(null, options);

        assertSame("Must buy highest-value money (Dogecoin)", buyDogecoin, chosen);
    }

    @Test
    public void classLevel_picksSafeNonBuyWhenCannotBuy() {
        Decision endMoney = new EndPhaseDecision(GameState.TurnPhase.MONEY);
        Decision endBuy = new EndPhaseDecision(GameState.TurnPhase.BUY);
        ImmutableList<Decision> options = ImmutableList.of(endMoney, endBuy);

        Decision chosen = player.makeDecision(null, options);

        assertTrue("Must pick a safe non-buy action",
                   options.contains(chosen));
    }

    // ========================================================================
    // getName() TESTS
    // ========================================================================

    @Test
    public void getName_returnsStableName() {
        String name1 = player.getName();
        String name2 = player.getName();

        assertNotNull("Name must not be null", name1);
        assertEquals("Name must be stable", name1, name2);
    }

    @Test
    public void getName_identifiesStrategyCorrectly() {
        String name = player.getName();

        assertEquals("Name should indicate Advanced Strategy 1", "BigMoneyAdv1", name);
    }

    // ========================================================================
    // makeDecision() TESTS
    // ========================================================================

    @Test(expected = IllegalArgumentException.class)
    public void makeDecision_throwsOnEmptyOptions() {
        player.makeDecision(null, ImmutableList.of());
    }

    @Test
    public void makeDecision_returnsOnlyOptionWhenSingle() {
        Decision only = new EndPhaseDecision(GameState.TurnPhase.BUY);
        ImmutableList<Decision> options = ImmutableList.of(only);

        Decision chosen = player.makeDecision(null, options);

        assertSame("Must return the only option", only, chosen);
    }

    @Test
    public void makeDecision_prefersFrameworkWhenAdvanced1Approves() {
        // Strong deck: expected money >= 8 (Framework cost)
        ImmutableCollection<Card> strongDeck = ImmutableList.of(
            new Card(Card.Type.DOGECOIN, 1),  // value 3
            new Card(Card.Type.DOGECOIN, 2),  // value 3
            new Card(Card.Type.ETHEREUM, 3),  // value 2
            new Card(Card.Type.ETHEREUM, 4),  // value 2
            new Card(Card.Type.ETHEREUM, 5)   // value 2
        );
        // Expected money per hand = (3+3+2+2+2)/5 * 5 = 12, which is > 8
        Hand strongHand = new Hand(ImmutableList.of(), strongDeck);
        GameState strongState = createGameState(strongHand, 8);

        Decision buyFramework = new BuyDecision(Card.Type.FRAMEWORK);
        Decision buyDogecoin = new BuyDecision(Card.Type.DOGECOIN);
        ImmutableList<Decision> options = ImmutableList.of(buyDogecoin, buyFramework);

        Decision chosen = player.makeDecision(strongState, options);

        assertSame("Should buy Framework when deck is strong enough", buyFramework, chosen);
    }

    @Test
    public void makeDecision_buysBestMoneyWhenFrameworkNotApproved() {
        // Medium deck but not strong enough for Framework
        ImmutableCollection<Card> mediumDeck = ImmutableList.of(
            new Card(Card.Type.ETHEREUM, 1),  // value 2
            new Card(Card.Type.ETHEREUM, 2),  // value 2
            new Card(Card.Type.BITCOIN, 3),   // value 1
            new Card(Card.Type.BITCOIN, 4),   // value 1
            new Card(Card.Type.BITCOIN, 5)    // value 1
        );
        // Expected money = (2+2+1+1+1)/5 * 5 = 7, which is < 8
        Hand mediumHand = new Hand(ImmutableList.of(), mediumDeck);
        GameState mediumState = createGameState(mediumHand, 8);

        Decision buyFramework = new BuyDecision(Card.Type.FRAMEWORK);
        Decision buyDogecoin = new BuyDecision(Card.Type.DOGECOIN);
        ImmutableList<Decision> options = ImmutableList.of(buyFramework, buyDogecoin);

        Decision chosen = player.makeDecision(mediumState, options);

        assertSame("Should buy best money when Framework not approved", buyDogecoin, chosen);
    }

    // ========================================================================
    // ADVANCED STRATEGY 1 CORE LOGIC TESTS
    // ========================================================================

    @Test
    public void advancedStrategy1_delaysFrameworkWhenDeckIsWeak() {
        // All Bitcoin (value 1): expected = 1 * 5 = 5, far below Framework cost (8)
        ImmutableCollection<Card> poorDeck = ImmutableList.of(
            new Card(Card.Type.BITCOIN, 1),
            new Card(Card.Type.BITCOIN, 2),
            new Card(Card.Type.BITCOIN, 3),
            new Card(Card.Type.BITCOIN, 4),
            new Card(Card.Type.BITCOIN, 5)
        );
        Hand poorHand = new Hand(ImmutableList.of(), poorDeck);
        GameState poorState = createGameState(poorHand, 10); // Even with 10 money available

        Decision buyFramework = new BuyDecision(Card.Type.FRAMEWORK);
        Decision buyEthereum = new BuyDecision(Card.Type.ETHEREUM);
        ImmutableList<Decision> options = ImmutableList.of(buyFramework, buyEthereum);

        Decision chosen = player.makeDecision(poorState, options);

        assertSame("Must delay Framework purchase when deck is too weak", buyEthereum, chosen);
    }

    @Test
    public void advancedStrategy1_buysFrameworkWhenDeckIsStrong() {
        // All Dogecoin (value 3): expected = 3 * 5 = 15, well above Framework cost (8)
        ImmutableCollection<Card> richDeck = ImmutableList.of(
            new Card(Card.Type.DOGECOIN, 1),
            new Card(Card.Type.DOGECOIN, 2),
            new Card(Card.Type.DOGECOIN, 3),
            new Card(Card.Type.DOGECOIN, 4),
            new Card(Card.Type.DOGECOIN, 5)
        );
        Hand richHand = new Hand(ImmutableList.of(), richDeck);
        GameState richState = createGameState(richHand, 8);

        Decision buyFramework = new BuyDecision(Card.Type.FRAMEWORK);
        Decision buyModule = new BuyDecision(Card.Type.MODULE);
        ImmutableList<Decision> options = ImmutableList.of(buyModule, buyFramework);

        Decision chosen = player.makeDecision(richState, options);

        assertSame("Must buy Framework when deck is strong enough", buyFramework, chosen);
    }

    @Test
    public void advancedStrategy1_respectsThresholdAtBoundary() {
        // Expected money exactly at Framework cost: 8
        // Better: use cards that average to 8/5 = 1.6 per card
        // 2,2,2,1,1 = 8 total, 8/5 * 5 = 8
        ImmutableCollection<Card> exactDeck = ImmutableList.of(
            new Card(Card.Type.ETHEREUM, 1),  // 2
            new Card(Card.Type.ETHEREUM, 2),  // 2
            new Card(Card.Type.ETHEREUM, 3),  // 2
            new Card(Card.Type.BITCOIN, 4),   // 1
            new Card(Card.Type.BITCOIN, 5)    // 1
        );
        // Expected = (2+2+2+1+1)/5 * 5 = 8
        Hand exactHand = new Hand(ImmutableList.of(), exactDeck);
        GameState exactState = createGameState(exactHand, 8);

        Decision buyFramework = new BuyDecision(Card.Type.FRAMEWORK);
        Decision buyEthereum = new BuyDecision(Card.Type.ETHEREUM);
        ImmutableList<Decision> options = ImmutableList.of(buyFramework, buyEthereum);

        Decision chosen = player.makeDecision(exactState, options);

        // With threshold ">=", at exactly 8, Framework should be bought
        assertSame("At threshold boundary, should buy Framework", buyFramework, chosen);
    }

    @Test
    public void advancedStrategy1_doesNotBuyFrameworkWhenCannotAfford() {
        // Strong deck but insufficient current money
        ImmutableCollection<Card> strongDeck = ImmutableList.of(
            new Card(Card.Type.DOGECOIN, 1),
            new Card(Card.Type.DOGECOIN, 2),
            new Card(Card.Type.DOGECOIN, 3),
            new Card(Card.Type.DOGECOIN, 4),
            new Card(Card.Type.DOGECOIN, 5)
        );
        Hand strongHand = new Hand(ImmutableList.of(), strongDeck);
        GameState poorMoneyState = createGameState(strongHand, 5); // Only 5 money, need 8

        Decision buyFramework = new BuyDecision(Card.Type.FRAMEWORK);
        Decision buyBitcoin = new BuyDecision(Card.Type.BITCOIN);
        ImmutableList<Decision> options = ImmutableList.of(buyFramework, buyBitcoin);

        Decision chosen = player.makeDecision(poorMoneyState, options);

        assertSame("Cannot buy Framework if cannot afford it now", buyBitcoin, chosen);
    }

    // ========================================================================
    // HELPER METHOD TESTS (quality: separate math from strategy)
    // ========================================================================

    @Test
    public void helper_chooseBestMoneyCard_selectsHighestValue() {
        Decision buyBitcoin = new BuyDecision(Card.Type.BITCOIN);
        Decision buyEthereum = new BuyDecision(Card.Type.ETHEREUM);
        ImmutableList<Decision> options = ImmutableList.of(buyBitcoin, buyEthereum);

        Decision chosen = player.makeDecision(null, options);

        assertSame("chooseBestMoneyCard must pick Ethereum over Bitcoin", buyEthereum, chosen);
    }

    @Test
    public void helper_chooseBestMoneyCard_handlesMixedOrder() {
        Decision buyEthereum = new BuyDecision(Card.Type.ETHEREUM);
        Decision buyDogecoin = new BuyDecision(Card.Type.DOGECOIN);
        Decision buyBitcoin = new BuyDecision(Card.Type.BITCOIN);
        ImmutableList<Decision> shuffled = ImmutableList.of(buyEthereum, buyBitcoin, buyDogecoin);

        Decision chosen = player.makeDecision(null, shuffled);

        assertSame("Must find highest value regardless of order", buyDogecoin, chosen);
    }

    @Test
    public void helper_chooseDefaultNonBuy_returnsValidOption() {
        Decision end1 = new EndPhaseDecision(GameState.TurnPhase.MONEY);
        Decision end2 = new EndPhaseDecision(GameState.TurnPhase.BUY);
        ImmutableList<Decision> nonBuyOptions = ImmutableList.of(end1, end2);

        Decision chosen = player.makeDecision(null, nonBuyOptions);

        assertTrue("chooseDefaultNonBuy must return an option from list",
                   nonBuyOptions.contains(chosen));
    }

    @Test
    public void helper_expectedMoneyPerHand_calculatesCorrectly() {
        // Known deck: 3,3,2,2,1 → average 2.2, hand of 5 → 11
        ImmutableCollection<Card> knownDeck = ImmutableList.of(
            new Card(Card.Type.DOGECOIN, 1),   // 3
            new Card(Card.Type.DOGECOIN, 2),   // 3
            new Card(Card.Type.ETHEREUM, 3),   // 2
            new Card(Card.Type.ETHEREUM, 4),   // 2
            new Card(Card.Type.BITCOIN, 5)     // 1
        );
        Hand knownHand = new Hand(ImmutableList.of(), knownDeck);
        GameState knownState = createGameState(knownHand, 10);

        // Test indirectly via shouldBuyFrameworkNow behavior
        // Expected = (3+3+2+2+1)/5 * 5 = 11 > 8, so Framework should be approved
        Decision buyFramework = new BuyDecision(Card.Type.FRAMEWORK);
        Decision buyBitcoin = new BuyDecision(Card.Type.BITCOIN);
        ImmutableList<Decision> options = ImmutableList.of(buyFramework, buyBitcoin);

        Decision chosen = player.makeDecision(knownState, options);

        assertSame("expectedMoneyPerHand calculation should enable Framework", buyFramework, chosen);
    }

    // ========================================================================
    // EDGE CASES
    // ========================================================================

    @Test
    public void edgeCase_nullStateHandledSafely() {
        Decision buyDogecoin = new BuyDecision(Card.Type.DOGECOIN);
        Decision buyBitcoin = new BuyDecision(Card.Type.BITCOIN);
        ImmutableList<Decision> options = ImmutableList.of(buyDogecoin, buyBitcoin);

        Decision chosen = player.makeDecision(null, options);

        assertNotNull("Must handle null state safely", chosen);
        assertTrue("Must return valid option", options.contains(chosen));
    }

    @Test
    public void edgeCase_emptyHandDoesNotCrash() {
        Hand emptyHand = new Hand(ImmutableList.of(), ImmutableList.of());
        GameState emptyState = createGameState(emptyHand, 10);

        Decision buyFramework = new BuyDecision(Card.Type.FRAMEWORK);
        Decision buyEthereum = new BuyDecision(Card.Type.ETHEREUM);
        ImmutableList<Decision> options = ImmutableList.of(buyFramework, buyEthereum);

        Decision chosen = player.makeDecision(emptyState, options);

        assertNotNull("Must handle empty hand without crash", chosen);
    }

    @Test
    public void edgeCase_onlyFrameworkAvailable() {
        ImmutableCollection<Card> weakDeck = ImmutableList.of(
            new Card(Card.Type.BITCOIN, 1),
            new Card(Card.Type.BITCOIN, 2),
            new Card(Card.Type.BITCOIN, 3),
            new Card(Card.Type.BITCOIN, 4),
            new Card(Card.Type.BITCOIN, 5)
        );
        Hand weakHand = new Hand(ImmutableList.of(), weakDeck);
        GameState weakState = createGameState(weakHand, 8);

        Decision buyFramework = new BuyDecision(Card.Type.FRAMEWORK);
        ImmutableList<Decision> onlyOption = ImmutableList.of(buyFramework);

        Decision chosen = player.makeDecision(weakState, onlyOption);

        assertSame("When only Framework available, must buy it even if weak", buyFramework, chosen);
    }

    // ========================================================================
    // HELPER METHODS FOR TEST SETUP
    // ========================================================================

    private GameState createGameState(Hand hand, int spendableMoney) {
        CardStacks stacks = new CardStacks(ImmutableMap.of(
            Card.Type.FRAMEWORK, 8,
            Card.Type.MODULE, 10,
            Card.Type.DOGECOIN, 10,
            Card.Type.ETHEREUM, 10,
            Card.Type.BITCOIN, 10
        ));

        return new GameState(
            "TestPlayer",
            hand,
            GameState.TurnPhase.BUY,
            0,              // availableActions
            spendableMoney, // spendableMoney
            1,              // availableBuys
            stacks
        );
    }
}
