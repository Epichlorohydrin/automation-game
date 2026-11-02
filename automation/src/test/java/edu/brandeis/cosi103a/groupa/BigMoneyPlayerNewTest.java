package edu.brandeis.cosi103a.groupa;

import com.google.common.collect.ImmutableList;
import edu.brandeis.cosi.atg.cards.Card;
import edu.brandeis.cosi.atg.decisions.BuyDecision;
import edu.brandeis.cosi.atg.decisions.Decision;
import edu.brandeis.cosi.atg.decisions.DiscardCardDecision;
import edu.brandeis.cosi.atg.decisions.EndPhaseDecision;
import edu.brandeis.cosi.atg.event.GainCardEvent;
import edu.brandeis.cosi.atg.state.GameState;
import edu.brandeis.cosi103a.groupa.players.BigMoneyPlayerNew;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * Unit tests for the tuned BigMoneyPlayerNew strategy.
 */
public class BigMoneyPlayerNewTest {

    @Test
    public void getName_isStable() {
        BigMoneyPlayerNew player = new BigMoneyPlayerNew();

        assertEquals("bigmoneyplayernew", player.getName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void makeDecision_throwsOnEmptyOptions() {
        BigMoneyPlayerNew player = new BigMoneyPlayerNew();

        player.makeDecision(null, ImmutableList.of());
    }

    @Test
    public void makeDecision_buysHackBeforeOtherAttacksAndIpo() {
        BigMoneyPlayerNew player = new BigMoneyPlayerNew();

        Decision buyHack = new BuyDecision(Card.Type.HACK);
        Decision buyRansomware = new BuyDecision(Card.Type.RANSOMWARE);
        Decision buyEvergreenTest = new BuyDecision(Card.Type.EVERGREEN_TEST);
        Decision buyIpo = new BuyDecision(Card.Type.IPO);

        Decision chosen = player.makeDecision(null, ImmutableList.of(
            buyIpo,
            buyEvergreenTest,
            buyRansomware,
            buyHack
        ));

        assertSame(buyHack, chosen);
    }

    @Test
    public void makeDecision_buysRansomwareBeforeEvergreenTestWhenHackUnavailable() {
        BigMoneyPlayerNew player = new BigMoneyPlayerNew();

        Decision buyRansomware = new BuyDecision(Card.Type.RANSOMWARE);
        Decision buyEvergreenTest = new BuyDecision(Card.Type.EVERGREEN_TEST);
        Decision buyIpo = new BuyDecision(Card.Type.IPO);

        Decision chosen = player.makeDecision(null, ImmutableList.of(
            buyIpo,
            buyEvergreenTest,
            buyRansomware
        ));

        assertSame(buyRansomware, chosen);
    }

    @Test
    public void makeDecision_limitsAttackBuysToFourCopies() {
        BigMoneyPlayerNew player = new BigMoneyPlayerNew();
        Decision buyHack = new BuyDecision(Card.Type.HACK);
        Decision endBuy = new EndPhaseDecision(GameState.TurnPhase.BUY);

        for (int i = 0; i < 4; i++) {
            assertSame(buyHack, player.makeDecision(null, ImmutableList.of(buyHack, endBuy)));
        }

        Decision buyIpo = new BuyDecision(Card.Type.IPO);
        Decision fifthChoice = player.makeDecision(null, ImmutableList.of(buyHack, buyIpo, endBuy));

        assertSame(buyIpo, fifthChoice);
    }

    @Test
    public void makeDecision_doesNotBuyDeploymentPipeline() {
        BigMoneyPlayerNew player = new BigMoneyPlayerNew();

        Decision buyDeploymentPipeline = new BuyDecision(Card.Type.DEPLOYMENT_PIPELINE);
        Decision buyEthereum = new BuyDecision(Card.Type.ETHEREUM);

        Decision chosen = player.makeDecision(null, ImmutableList.of(
            buyDeploymentPipeline,
            buyEthereum
        ));

        assertSame(buyEthereum, chosen);
    }

    @Test
    public void makeDecision_buysMonitoringOnlyAfterOpponentAttackObserved() {
        BigMoneyPlayerNew player = new BigMoneyPlayerNew();

        Decision buyMonitoring = new BuyDecision(Card.Type.MONITORING);
        Decision buyEthereum = new BuyDecision(Card.Type.ETHEREUM);
        ImmutableList<Decision> options = ImmutableList.of(buyMonitoring, buyEthereum);

        assertSame(buyEthereum, player.makeDecision(null, options));

        Decision chosen = player.makeDecision(
            null,
            options,
            Optional.of(new GainCardEvent(Card.Type.HACK, "opponent"))
        );

        assertSame(buyMonitoring, chosen);
    }

    @Test
    public void makeDecision_discardsBugBeforeEndingOptionalDiscard() {
        BigMoneyPlayerNew player = new BigMoneyPlayerNew();

        Card bug = new Card(Card.Type.BUG, 1);
        Card bitcoin = new Card(Card.Type.BITCOIN, 2);
        Decision discardBug = new DiscardCardDecision(bug);
        Decision discardBitcoin = new DiscardCardDecision(bitcoin);
        Decision endDiscard = new EndPhaseDecision(GameState.TurnPhase.DISCARD);

        Decision chosen = player.makeDecision(null, ImmutableList.of(discardBitcoin, discardBug, endDiscard));

        assertSame(discardBug, chosen);
    }

    @Test
    public void makeDecision_keepsBitcoinWhenOptionalDiscardWouldMissFrameworkMoney() {
        BigMoneyPlayerNew player = new BigMoneyPlayerNew();

        Card bitcoin = new Card(Card.Type.BITCOIN, 1);
        Card dogecoin = new Card(Card.Type.DOGECOIN, 2);
        Card ethereum = new Card(Card.Type.ETHEREUM, 3);
        Decision discardBitcoin = new DiscardCardDecision(bitcoin);
        Decision discardDogecoin = new DiscardCardDecision(dogecoin);
        Decision discardEthereum = new DiscardCardDecision(ethereum);
        Decision endDiscard = new EndPhaseDecision(GameState.TurnPhase.DISCARD);

        Decision chosen = player.makeDecision(null, ImmutableList.of(
            discardBitcoin,
            discardDogecoin,
            discardEthereum,
            endDiscard
        ));

        assertSame(endDiscard, chosen);
    }

    @Test
    public void makeDecision_discardsBitcoinWhenOptionalDiscardStillLeavesFrameworkMoney() {
        BigMoneyPlayerNew player = new BigMoneyPlayerNew();

        Card bitcoin = new Card(Card.Type.BITCOIN, 1);
        Card dogecoinOne = new Card(Card.Type.DOGECOIN, 2);
        Card dogecoinTwo = new Card(Card.Type.DOGECOIN, 3);
        Card ethereum = new Card(Card.Type.ETHEREUM, 4);
        Decision discardBitcoin = new DiscardCardDecision(bitcoin);
        Decision endDiscard = new EndPhaseDecision(GameState.TurnPhase.DISCARD);

        Decision chosen = player.makeDecision(null, ImmutableList.of(
            new DiscardCardDecision(dogecoinOne),
            discardBitcoin,
            new DiscardCardDecision(ethereum),
            new DiscardCardDecision(dogecoinTwo),
            endDiscard
        ));

        assertSame(discardBitcoin, chosen);
    }

    @Test
    public void makeDecision_discardsLeastPainfulCardWhenDiscardIsForced() {
        BigMoneyPlayerNew player = new BigMoneyPlayerNew();

        Card bug = new Card(Card.Type.BUG, 1);
        Card dogecoin = new Card(Card.Type.DOGECOIN, 2);
        Card framework = new Card(Card.Type.FRAMEWORK, 3);
        Decision discardBug = new DiscardCardDecision(bug);

        Decision chosen = player.makeDecision(null, ImmutableList.of(
            new DiscardCardDecision(framework),
            new DiscardCardDecision(dogecoin),
            discardBug
        ));

        assertSame(discardBug, chosen);
    }
}
