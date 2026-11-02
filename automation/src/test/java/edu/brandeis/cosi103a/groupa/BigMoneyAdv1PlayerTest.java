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
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

/**
 * Unit tests for BigMoneyAdv1Player.
 *
 * <p>Tests verify that Advanced Strategy 1 is correctly implemented:</p>
 * <ul>
 *   <li>Framework is bought only when expected hand money is sufficient</li>
 *   <li>Money cards are prioritized by value when Framework isn't bought</li>
 *   <li>Edge cases are handled correctly</li>
 * </ul>
 */
public class BigMoneyAdv1PlayerTest {

    @Test
    public void constructor_existsAndNameIsStable() {
        BigMoneyAdv1Player player = new BigMoneyAdv1Player();

        String name1 = player.getName();
        String name2 = player.getName();

        assertNotNull("Name should be non-null", name1);
        assertEquals("Name should be stable across calls", name1, name2);
        assertEquals("BigMoneyAdv1", name1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void makeDecision_throwsOnEmptyOptions() {
        BigMoneyAdv1Player player = new BigMoneyAdv1Player();
        player.makeDecision(null, ImmutableList.of());
    }

    @Test
    public void makeDecision_returnsOnlyOptionWhenSingle() {
        BigMoneyAdv1Player player = new BigMoneyAdv1Player();

        Decision only = new EndPhaseDecision(GameState.TurnPhase.BUY);
        Decision chosen = player.makeDecision(null, ImmutableList.of(only));

        assertSame(only, chosen);
    }

    @Test
    public void makeDecision_picksBestMoneyWhenNoFramework() {
        BigMoneyAdv1Player player = new BigMoneyAdv1Player();

        Decision buyBitcoin = new BuyDecision(Card.Type.BITCOIN);
        Decision buyEthereum = new BuyDecision(Card.Type.ETHEREUM);
        Decision buyDogecoin = new BuyDecision(Card.Type.DOGECOIN);
        ImmutableList<Decision> options = ImmutableList.of(buyBitcoin, buyEthereum, buyDogecoin);

        Decision chosen = player.makeDecision(null, options);

        assertSame("Should pick Dogecoin (highest value)", buyDogecoin, chosen);
    }

    @Test
    public void makeDecision_fallsBackToFirstOptionWhenNoBuys() {
        BigMoneyAdv1Player player = new BigMoneyAdv1Player();

        Decision endMoney = new EndPhaseDecision(GameState.TurnPhase.MONEY);
        Decision endBuy = new EndPhaseDecision(GameState.TurnPhase.BUY);
        ImmutableList<Decision> options = ImmutableList.of(endMoney, endBuy);

        Decision chosen = player.makeDecision(null, options);

        assertSame(endMoney, chosen);
    }

    @Test
    public void makeDecision_buysFrameworkWhenExpectedMoneyIsSufficient() {
        BigMoneyAdv1Player player = new BigMoneyAdv1Player();

        // Create a hand with high-value money cards
        ImmutableCollection<Card> moneyCards = ImmutableList.of(
            new Card(Card.Type.DOGECOIN, 1),
            new Card(Card.Type.ETHEREUM, 2),
            new Card(Card.Type.ETHEREUM, 3),
            new Card(Card.Type.BITCOIN, 4),
            new Card(Card.Type.BITCOIN, 5)
        );
        Hand hand = new Hand(ImmutableList.of(), moneyCards);

        CardStacks stacks = new CardStacks(ImmutableMap.of(
            Card.Type.FRAMEWORK, 8,
            Card.Type.DOGECOIN, 10
        ));

        GameState state = new GameState(
            "TestPlayer",
            hand,
            GameState.TurnPhase.BUY,
            0,
            8, // enough money to buy Framework
            1,
            stacks
        );

        Decision buyFramework = new BuyDecision(Card.Type.FRAMEWORK);
        Decision buyDogecoin = new BuyDecision(Card.Type.DOGECOIN);
        ImmutableList<Decision> options = ImmutableList.of(buyDogecoin, buyFramework);

        Decision chosen = player.makeDecision(state, options);

        // Expected money per hand = (3+2+2+1+1)/5 * 5 = 9, which is > Framework cost (8)
        assertSame("Should buy Framework when expected money is sufficient", buyFramework, chosen);
    }

    @Test
    public void makeDecision_doesNotBuyFrameworkWhenExpectedMoneyIsInsufficient() {
        BigMoneyAdv1Player player = new BigMoneyAdv1Player();

        // Create a hand with low-value money cards
        ImmutableCollection<Card> poorCards = ImmutableList.of(
            new Card(Card.Type.BITCOIN, 1),
            new Card(Card.Type.BITCOIN, 2),
            new Card(Card.Type.BITCOIN, 3),
            new Card(Card.Type.BITCOIN, 4),
            new Card(Card.Type.BITCOIN, 5)
        );
        Hand hand = new Hand(ImmutableList.of(), poorCards);

        CardStacks stacks = new CardStacks(ImmutableMap.of(
            Card.Type.FRAMEWORK, 8,
            Card.Type.ETHEREUM, 10
        ));

        GameState state = new GameState(
            "TestPlayer",
            hand,
            GameState.TurnPhase.BUY,
            0,
            8, // enough money to buy Framework
            1,
            stacks
        );

        Decision buyFramework = new BuyDecision(Card.Type.FRAMEWORK);
        Decision buyEthereum = new BuyDecision(Card.Type.ETHEREUM);
        ImmutableList<Decision> options = ImmutableList.of(buyFramework, buyEthereum);

        Decision chosen = player.makeDecision(state, options);

        // Expected money per hand = (1+1+1+1+1)/5 * 5 = 5, which is < Framework cost (8)
        assertSame("Should not buy Framework when expected money is insufficient", buyEthereum, chosen);
    }
}
