package edu.brandeis.cosi103a.groupa;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import edu.brandeis.cosi.atg.cards.Card;
import edu.brandeis.cosi.atg.decisions.BuyDecision;
import edu.brandeis.cosi.atg.decisions.Decision;
import edu.brandeis.cosi.atg.decisions.DiscardCardDecision;
import edu.brandeis.cosi.atg.decisions.EndPhaseDecision;
import edu.brandeis.cosi.atg.decisions.PlayCardDecision;
import edu.brandeis.cosi.atg.state.CardStacks;
import edu.brandeis.cosi.atg.state.GameState;
import edu.brandeis.cosi.atg.state.Hand;
import edu.brandeis.cosi103a.groupa.players.BalancedAutoPlayer;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

/**
 * Unit tests for BalancedAutoPlayer.
 */
public class BalancedAutoPlayerTest {

    @Test
    public void constructor_existsAndNameIsStable() {
        BalancedAutoPlayer player = new BalancedAutoPlayer();

        String name1 = player.getName();
        String name2 = player.getName();

        assertNotNull(name1);
        assertEquals(name1, name2);
        assertEquals("BalancedAutoPlayer", name1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void makeDecision_throwsOnEmptyOptions() {
        BalancedAutoPlayer player = new BalancedAutoPlayer();
        player.makeDecision(null, ImmutableList.of());
    }

    @Test
    public void makeDecision_returnsOnlyOptionWhenSingle() {
        BalancedAutoPlayer player = new BalancedAutoPlayer();

        Decision only = new EndPhaseDecision(GameState.TurnPhase.BUY);
        Decision chosen = player.makeDecision(null, ImmutableList.of(only));

        assertSame(only, chosen);
    }

    @Test
    public void makeDecision_buysFrameworkBeforeMoneyAndOtherPointCards() {
        BalancedAutoPlayer player = new BalancedAutoPlayer();

        Decision buyMethod = new BuyDecision(Card.Type.METHOD);
        Decision buyFramework = new BuyDecision(Card.Type.FRAMEWORK);
        Decision buyDogecoin = new BuyDecision(Card.Type.DOGECOIN);
        Decision endBuy = new EndPhaseDecision(GameState.TurnPhase.BUY);

        Decision chosen = player.makeDecision(null, ImmutableList.of(
            buyDogecoin,
            buyMethod,
            buyFramework,
            endBuy
        ));

        assertSame(buyFramework, chosen);
    }

    @Test
    public void makeDecision_buysEthereumOnOpeningRoundWhenAvailable() {
        BalancedAutoPlayer player = new BalancedAutoPlayer();

        Decision buyEthereum = new BuyDecision(Card.Type.ETHEREUM);
        Decision buyFramework = new BuyDecision(Card.Type.FRAMEWORK);
        Decision buyDogecoin = new BuyDecision(Card.Type.DOGECOIN);
        Decision endBuy = new EndPhaseDecision(GameState.TurnPhase.BUY);

        Decision chosen = player.makeDecision(null, ImmutableList.of(
            buyDogecoin,
            buyFramework,
            buyEthereum,
            endBuy
        ));

        assertSame(buyEthereum, chosen);
    }

    @Test
    public void makeDecision_returnsToFrameworkPriorityAfterOpeningRound() {
        BalancedAutoPlayer player = new BalancedAutoPlayer();

        Decision buyEthereum = new BuyDecision(Card.Type.ETHEREUM);
        Decision endBuy = new EndPhaseDecision(GameState.TurnPhase.BUY);
        Decision openingChoice = player.makeDecision(null, ImmutableList.of(buyEthereum, endBuy));
        assertSame(buyEthereum, openingChoice);

        Decision buyFramework = new BuyDecision(Card.Type.FRAMEWORK);
        Decision buyDogecoin = new BuyDecision(Card.Type.DOGECOIN);
        Decision laterChoice = player.makeDecision(null, ImmutableList.of(buyDogecoin, buyFramework, endBuy));

        assertSame(buyFramework, laterChoice);
    }

    @Test
    public void makeDecision_doesNotBuyModuleOrMethodWhenFrameworkIsNotAvailable() {
        BalancedAutoPlayer player = new BalancedAutoPlayer();

        Decision buyMethod = new BuyDecision(Card.Type.METHOD);
        Decision buyModule = new BuyDecision(Card.Type.MODULE);
        Decision endBuy = new EndPhaseDecision(GameState.TurnPhase.BUY);

        Decision chosen = player.makeDecision(null, ImmutableList.of(
            buyMethod,
            buyModule,
            endBuy
        ));

        assertSame(endBuy, chosen);
    }

    @Test
    public void makeDecision_prefersBestHelpfulActionWhileDeckIsStillMoneyHeavy() {
        BalancedAutoPlayer player = new BalancedAutoPlayer();

        Decision buyDogecoin = new BuyDecision(Card.Type.DOGECOIN);
        Decision buyMonitoring = new BuyDecision(Card.Type.MONITORING);
        Decision buyIpo = new BuyDecision(Card.Type.IPO);
        Decision buySprintPlanning = new BuyDecision(Card.Type.SPRINT_PLANNING);
        Decision endBuy = new EndPhaseDecision(GameState.TurnPhase.BUY);

        Decision chosen = player.makeDecision(null, ImmutableList.of(
            buyDogecoin,
            buyMonitoring,
            buyIpo,
            buySprintPlanning,
            endBuy
        ));

        assertSame(buySprintPlanning, chosen);
    }

    @Test
    public void makeDecision_prioritizesMonitoringWhenAttackCardsAreAvailable() {
        BalancedAutoPlayer player = new BalancedAutoPlayer();

        Decision buyMonitoring = new BuyDecision(Card.Type.MONITORING);
        Decision buyHack = new BuyDecision(Card.Type.HACK);
        Decision buySprintPlanning = new BuyDecision(Card.Type.SPRINT_PLANNING);
        Decision endBuy = new EndPhaseDecision(GameState.TurnPhase.BUY);

        Decision chosen = player.makeDecision(null, ImmutableList.of(
            buySprintPlanning,
            buyHack,
            buyMonitoring,
            endBuy
        ));

        assertSame(buyMonitoring, chosen);
    }

    @Test
    public void makeDecision_limitsEachActionCardToThreeCopies() {
        BalancedAutoPlayer player = new BalancedAutoPlayer();

        buyCard(player, Card.Type.IPO);
        buyCard(player, Card.Type.IPO);
        buyCard(player, Card.Type.IPO);

        Decision buyIpo = new BuyDecision(Card.Type.IPO);
        Decision buyMonitoring = new BuyDecision(Card.Type.MONITORING);
        Decision endBuy = new EndPhaseDecision(GameState.TurnPhase.BUY);

        Decision chosen = player.makeDecision(null, ImmutableList.of(
            buyIpo,
            buyMonitoring,
            endBuy
        ));

        assertSame(buyMonitoring, chosen);
    }

    @Test
    public void makeDecision_buysParallelizationBeforeSamePriceActionCards() {
        BalancedAutoPlayer player = new BalancedAutoPlayer();

        Decision buyMonitoring = new BuyDecision(Card.Type.MONITORING);
        Decision buyParallelization = new BuyDecision(Card.Type.PARALLELIZATION);
        Decision endBuy = new EndPhaseDecision(GameState.TurnPhase.BUY);

        Decision chosen = player.makeDecision(null, ImmutableList.of(
            buyMonitoring,
            buyParallelization,
            endBuy
        ));

        assertSame(buyParallelization, chosen);
    }

    @Test
    public void makeDecision_limitsParallelizationToTwoCopies() {
        BalancedAutoPlayer player = new BalancedAutoPlayer();

        buyCard(player, Card.Type.PARALLELIZATION);
        buyCard(player, Card.Type.PARALLELIZATION);

        Decision buyMonitoring = new BuyDecision(Card.Type.MONITORING);
        Decision buyParallelization = new BuyDecision(Card.Type.PARALLELIZATION);
        Decision endBuy = new EndPhaseDecision(GameState.TurnPhase.BUY);

        Decision chosen = player.makeDecision(null, ImmutableList.of(
            buyParallelization,
            buyMonitoring,
            endBuy
        ));

        assertSame(buyMonitoring, chosen);
    }

    @Test
    public void makeDecision_limitsMonitoringToThreeCopiesEvenAgainstAttacks() {
        BalancedAutoPlayer player = new BalancedAutoPlayer();

        buyCard(player, Card.Type.MONITORING);
        buyCard(player, Card.Type.MONITORING);
        buyCard(player, Card.Type.MONITORING);

        Decision buyMonitoring = new BuyDecision(Card.Type.MONITORING);
        Decision buyHack = new BuyDecision(Card.Type.HACK);
        Decision buySprintPlanning = new BuyDecision(Card.Type.SPRINT_PLANNING);
        Decision endBuy = new EndPhaseDecision(GameState.TurnPhase.BUY);

        Decision chosen = player.makeDecision(null, ImmutableList.of(
            buyMonitoring,
            buyHack,
            buySprintPlanning,
            endBuy
        ));

        assertSame(buySprintPlanning, chosen);
    }

    @Test
    public void makeDecision_buysMonitoringWhenNoMoreExpensiveBuyIsAvailable() {
        BalancedAutoPlayer player = new BalancedAutoPlayer();

        Decision buyBitcoin = new BuyDecision(Card.Type.BITCOIN);
        Decision buyBacklog = new BuyDecision(Card.Type.BACKLOG);
        Decision buyMonitoring = new BuyDecision(Card.Type.MONITORING);
        Decision endBuy = new EndPhaseDecision(GameState.TurnPhase.BUY);

        Decision chosen = player.makeDecision(null, ImmutableList.of(
            buyBitcoin,
            buyBacklog,
            buyMonitoring,
            endBuy
        ));

        assertSame(buyMonitoring, chosen);
    }

    @Test
    public void makeDecision_doesNotBuyFourthMonitoringAsLowMoneyFallback() {
        BalancedAutoPlayer player = new BalancedAutoPlayer();

        buyCard(player, Card.Type.MONITORING);
        buyCard(player, Card.Type.MONITORING);
        buyCard(player, Card.Type.MONITORING);

        Decision buyBitcoin = new BuyDecision(Card.Type.BITCOIN);
        Decision buyMonitoring = new BuyDecision(Card.Type.MONITORING);
        Decision endBuy = new EndPhaseDecision(GameState.TurnPhase.BUY);

        Decision chosen = player.makeDecision(null, ImmutableList.of(
            buyBitcoin,
            buyMonitoring,
            endBuy
        ));

        assertSame(buyBitcoin, chosen);
    }

    @Test
    public void makeDecision_prioritizesMoneyWhenEstimatedDeckIsNotMajorityMoney() {
        BalancedAutoPlayer player = playerWithFiveBoughtActions();

        Decision buyDogecoin = new BuyDecision(Card.Type.DOGECOIN);
        Decision buySprintPlanning = new BuyDecision(Card.Type.SPRINT_PLANNING);
        Decision endBuy = new EndPhaseDecision(GameState.TurnPhase.BUY);

        Decision chosen = player.makeDecision(null, ImmutableList.of(
            buySprintPlanning,
            buyDogecoin,
            endBuy
        ));

        assertSame(buyDogecoin, chosen);
    }

    @Test
    public void makeDecision_prioritizesDogecoinBeforeEthereumWhenBuyingMoney() {
        BalancedAutoPlayer player = playerWithFiveBoughtActions();

        Decision buyDogecoin = new BuyDecision(Card.Type.DOGECOIN);
        Decision buyEthereum = new BuyDecision(Card.Type.ETHEREUM);
        Decision endBuy = new EndPhaseDecision(GameState.TurnPhase.BUY);

        Decision chosen = player.makeDecision(null, ImmutableList.of(
            buyEthereum,
            buyDogecoin,
            endBuy
        ));

        assertSame(buyDogecoin, chosen);
    }

    @Test
    public void makeDecision_fallsBackToMoneyWhenNoEligibleActionBuyExists() {
        BalancedAutoPlayer player = new BalancedAutoPlayer();

        Decision buyDogecoin = new BuyDecision(Card.Type.DOGECOIN);
        Decision buyHack = new BuyDecision(Card.Type.HACK);
        Decision endBuy = new EndPhaseDecision(GameState.TurnPhase.BUY);

        Decision chosen = player.makeDecision(null, ImmutableList.of(
            buyHack,
            buyDogecoin,
            endBuy
        ));

        assertSame(buyDogecoin, chosen);
    }

    @Test
    public void makeDecision_buysBitcoinWhenItIsTheOnlyMoneyOption() {
        BalancedAutoPlayer player = new BalancedAutoPlayer();

        Decision buyBitcoin = new BuyDecision(Card.Type.BITCOIN);
        Decision endBuy = new EndPhaseDecision(GameState.TurnPhase.BUY);

        Decision chosen = player.makeDecision(null, ImmutableList.of(buyBitcoin, endBuy));

        assertSame(buyBitcoin, chosen);
    }

    @Test
    public void makeDecision_staysInActionPhaseBeforeConsideringBuys() {
        BalancedAutoPlayer player = new BalancedAutoPlayer();

        Decision playIpo = new PlayCardDecision(new Card(Card.Type.IPO, 2));
        Decision buyFramework = new BuyDecision(Card.Type.FRAMEWORK);
        Decision endAction = new EndPhaseDecision(GameState.TurnPhase.ACTION);

        Decision chosen = player.makeDecision(null, ImmutableList.of(playIpo, buyFramework, endAction));

        assertSame(playIpo, chosen);
    }

    @Test
    public void makeDecision_playsParallelizationBeforeOtherActions() {
        BalancedAutoPlayer player = new BalancedAutoPlayer();

        Decision playParallelization = new PlayCardDecision(new Card(Card.Type.PARALLELIZATION, 1));
        Decision playSprintPlanning = new PlayCardDecision(new Card(Card.Type.SPRINT_PLANNING, 2));
        Decision endAction = new EndPhaseDecision(GameState.TurnPhase.ACTION);

        Decision chosen = player.makeDecision(null, ImmutableList.of(
            playSprintPlanning,
            playParallelization,
            endAction
        ));

        assertSame(playParallelization, chosen);
    }

    @Test
    public void makeDecision_parallelizationComboPrioritizesMostActionsWhenNoPlusActionWillRemain() {
        BalancedAutoPlayer player = new BalancedAutoPlayer();
        Card codeReview = new Card(Card.Type.CODE_REVIEW, 2);
        Card dailyScrum = new Card(Card.Type.DAILY_SCRUM, 3);
        GameState state = parallelizationComboState(ImmutableList.of(codeReview, dailyScrum));

        Decision playCodeReview = new PlayCardDecision(codeReview);
        Decision playDailyScrum = new PlayCardDecision(dailyScrum);
        Decision endAction = new EndPhaseDecision(GameState.TurnPhase.ACTION);

        Decision chosen = player.makeDecision(state, ImmutableList.of(
            playDailyScrum,
            playCodeReview,
            endAction
        ));

        assertSame(playCodeReview, chosen);
    }

    @Test
    public void makeDecision_parallelizationComboPrioritizesMostCardsWhenPlusActionWillRemain() {
        BalancedAutoPlayer player = new BalancedAutoPlayer();
        Card codeReview = new Card(Card.Type.CODE_REVIEW, 2);
        Card sprintPlanning = new Card(Card.Type.SPRINT_PLANNING, 3);
        Card dailyScrum = new Card(Card.Type.DAILY_SCRUM, 4);
        GameState state = parallelizationComboState(ImmutableList.of(
            codeReview,
            sprintPlanning,
            dailyScrum
        ));

        Decision playCodeReview = new PlayCardDecision(codeReview);
        Decision playSprintPlanning = new PlayCardDecision(sprintPlanning);
        Decision playDailyScrum = new PlayCardDecision(dailyScrum);
        Decision endAction = new EndPhaseDecision(GameState.TurnPhase.ACTION);

        Decision chosen = player.makeDecision(state, ImmutableList.of(
            playCodeReview,
            playSprintPlanning,
            playDailyScrum,
            endAction
        ));

        assertSame(playDailyScrum, chosen);
    }

    @Test
    public void makeDecision_prioritizesActionAddingCardsOverCardOnlyActions() {
        BalancedAutoPlayer player = new BalancedAutoPlayer();

        Decision playMonitoring = new PlayCardDecision(new Card(Card.Type.MONITORING, 1));
        Decision playIpo = new PlayCardDecision(new Card(Card.Type.IPO, 2));
        Decision endAction = new EndPhaseDecision(GameState.TurnPhase.ACTION);

        Decision chosen = player.makeDecision(null, ImmutableList.of(playMonitoring, playIpo, endAction));

        assertSame(playIpo, chosen);
    }

    @Test
    public void makeDecision_playsCodeReviewBeforeActionsWithoutPlusAction() {
        BalancedAutoPlayer player = new BalancedAutoPlayer();

        Decision playMonitoring = new PlayCardDecision(new Card(Card.Type.MONITORING, 1));
        Decision playDailyScrum = new PlayCardDecision(new Card(Card.Type.DAILY_SCRUM, 2));
        Decision playCodeReview = new PlayCardDecision(new Card(Card.Type.CODE_REVIEW, 3));
        Decision endAction = new EndPhaseDecision(GameState.TurnPhase.ACTION);

        Decision chosen = player.makeDecision(null, ImmutableList.of(
            playMonitoring,
            playDailyScrum,
            playCodeReview,
            endAction
        ));

        assertSame(playCodeReview, chosen);
    }

    @Test
    public void makeDecision_playsPlusActionCardsBeforeDailyScrum() {
        BalancedAutoPlayer player = new BalancedAutoPlayer();

        Decision playDailyScrum = new PlayCardDecision(new Card(Card.Type.DAILY_SCRUM, 1));
        Decision playBacklog = new PlayCardDecision(new Card(Card.Type.BACKLOG, 2));
        Decision playUnitTest = new PlayCardDecision(new Card(Card.Type.UNIT_TEST, 3));
        Decision endAction = new EndPhaseDecision(GameState.TurnPhase.ACTION);

        Decision chosen = player.makeDecision(null, ImmutableList.of(
            playDailyScrum,
            playBacklog,
            playUnitTest,
            endAction
        ));

        assertSame(playUnitTest, chosen);
    }

    @Test
    public void makeDecision_playsPlusActionCardsBeforeEvergreenTest() {
        BalancedAutoPlayer player = new BalancedAutoPlayer();
        GameState state = actionStateWithSupply(0, ImmutableMap.of(
            Card.Type.BUG, 10,
            Card.Type.DOGECOIN, 10,
            Card.Type.FRAMEWORK, 8
        ));

        Decision playEvergreenTest = new PlayCardDecision(new Card(Card.Type.EVERGREEN_TEST, 1));
        Decision playCodeReview = new PlayCardDecision(new Card(Card.Type.CODE_REVIEW, 2));
        Decision endAction = new EndPhaseDecision(GameState.TurnPhase.ACTION);

        Decision chosen = player.makeDecision(state, ImmutableList.of(
            playEvergreenTest,
            playCodeReview,
            endAction
        ));

        assertSame(playCodeReview, chosen);
    }

    @Test
    public void makeDecision_prioritizesEvergreenTestOverNonPlusActionCardsWhenMonitoringAbsentAndBugsRemain() {
        BalancedAutoPlayer player = new BalancedAutoPlayer();
        GameState state = actionStateWithSupply(0, ImmutableMap.of(
            Card.Type.BUG, 10,
            Card.Type.DOGECOIN, 10,
            Card.Type.FRAMEWORK, 8
        ));

        Decision playDailyScrum = new PlayCardDecision(new Card(Card.Type.DAILY_SCRUM, 1));
        Decision playEvergreenTest = new PlayCardDecision(new Card(Card.Type.EVERGREEN_TEST, 2));
        Decision endAction = new EndPhaseDecision(GameState.TurnPhase.ACTION);

        Decision chosen = player.makeDecision(state, ImmutableList.of(
            playDailyScrum,
            playEvergreenTest,
            endAction
        ));

        assertSame(playEvergreenTest, chosen);
    }

    @Test
    public void makeDecision_doesNotBoostEvergreenTestWhenMonitoringIsInGame() {
        BalancedAutoPlayer player = new BalancedAutoPlayer();
        GameState state = actionStateWithSupply(0, ImmutableMap.of(
            Card.Type.BUG, 10,
            Card.Type.MONITORING, 10,
            Card.Type.DOGECOIN, 10,
            Card.Type.FRAMEWORK, 8
        ));

        Decision playDailyScrum = new PlayCardDecision(new Card(Card.Type.DAILY_SCRUM, 1));
        Decision playEvergreenTest = new PlayCardDecision(new Card(Card.Type.EVERGREEN_TEST, 2));
        Decision endAction = new EndPhaseDecision(GameState.TurnPhase.ACTION);

        Decision chosen = player.makeDecision(state, ImmutableList.of(
            playEvergreenTest,
            playDailyScrum,
            endAction
        ));

        assertSame(playDailyScrum, chosen);
    }

    @Test
    public void makeDecision_doesNotBoostEvergreenTestWhenNoBugsRemain() {
        BalancedAutoPlayer player = new BalancedAutoPlayer();
        GameState state = actionStateWithSupply(0, ImmutableMap.of(
            Card.Type.BUG, 0,
            Card.Type.DOGECOIN, 10,
            Card.Type.FRAMEWORK, 8
        ));

        Decision playDailyScrum = new PlayCardDecision(new Card(Card.Type.DAILY_SCRUM, 1));
        Decision playEvergreenTest = new PlayCardDecision(new Card(Card.Type.EVERGREEN_TEST, 2));
        Decision endAction = new EndPhaseDecision(GameState.TurnPhase.ACTION);

        Decision chosen = player.makeDecision(state, ImmutableList.of(
            playEvergreenTest,
            playDailyScrum,
            endAction
        ));

        assertSame(playDailyScrum, chosen);
    }

    @Test
    public void makeDecision_playsPlusActionCardsBeforeHack() {
        BalancedAutoPlayer player = new BalancedAutoPlayer();
        GameState state = actionStateWithSupply(0, ImmutableMap.of(
            Card.Type.HACK, 10,
            Card.Type.DOGECOIN, 10,
            Card.Type.FRAMEWORK, 8
        ));

        Decision playHack = new PlayCardDecision(new Card(Card.Type.HACK, 1));
        Decision playCodeReview = new PlayCardDecision(new Card(Card.Type.CODE_REVIEW, 2));
        Decision endAction = new EndPhaseDecision(GameState.TurnPhase.ACTION);

        Decision chosen = player.makeDecision(state, ImmutableList.of(
            playHack,
            playCodeReview,
            endAction
        ));

        assertSame(playCodeReview, chosen);
    }

    @Test
    public void makeDecision_prioritizesHackOverNonPlusActionCardsWhenEvergreenIsAbsent() {
        BalancedAutoPlayer player = new BalancedAutoPlayer();
        GameState state = actionStateWithSupply(0, ImmutableMap.of(
            Card.Type.HACK, 10,
            Card.Type.DOGECOIN, 10,
            Card.Type.FRAMEWORK, 8
        ));

        Decision playDailyScrum = new PlayCardDecision(new Card(Card.Type.DAILY_SCRUM, 1));
        Decision playHack = new PlayCardDecision(new Card(Card.Type.HACK, 2));
        Decision endAction = new EndPhaseDecision(GameState.TurnPhase.ACTION);

        Decision chosen = player.makeDecision(state, ImmutableList.of(
            playDailyScrum,
            playHack,
            endAction
        ));

        assertSame(playHack, chosen);
    }

    @Test
    public void makeDecision_prioritizesHackWhenEvergreenIsNotUseful() {
        BalancedAutoPlayer player = new BalancedAutoPlayer();
        GameState state = actionStateWithSupply(0, ImmutableMap.of(
            Card.Type.BUG, 0,
            Card.Type.EVERGREEN_TEST, 10,
            Card.Type.HACK, 10,
            Card.Type.DOGECOIN, 10,
            Card.Type.FRAMEWORK, 8
        ));

        Decision playDailyScrum = new PlayCardDecision(new Card(Card.Type.DAILY_SCRUM, 1));
        Decision playHack = new PlayCardDecision(new Card(Card.Type.HACK, 2));
        Decision endAction = new EndPhaseDecision(GameState.TurnPhase.ACTION);

        Decision chosen = player.makeDecision(state, ImmutableList.of(
            playDailyScrum,
            playHack,
            endAction
        ));

        assertSame(playHack, chosen);
    }

    @Test
    public void makeDecision_doesNotBoostHackWhenEvergreenIsUseful() {
        BalancedAutoPlayer player = new BalancedAutoPlayer();
        GameState state = actionStateWithSupply(0, ImmutableMap.of(
            Card.Type.BUG, 10,
            Card.Type.EVERGREEN_TEST, 10,
            Card.Type.HACK, 10,
            Card.Type.DOGECOIN, 10,
            Card.Type.FRAMEWORK, 8
        ));

        Decision playDailyScrum = new PlayCardDecision(new Card(Card.Type.DAILY_SCRUM, 1));
        Decision playHack = new PlayCardDecision(new Card(Card.Type.HACK, 2));
        Decision endAction = new EndPhaseDecision(GameState.TurnPhase.ACTION);

        Decision chosen = player.makeDecision(state, ImmutableList.of(
            playHack,
            playDailyScrum,
            endAction
        ));

        assertSame(playDailyScrum, chosen);
    }

    @Test
    public void makeDecision_playsFirstHackWhenMonitoringIsInGame() {
        BalancedAutoPlayer player = new BalancedAutoPlayer();
        GameState state = actionStateWithSupply(0, ImmutableMap.of(
            Card.Type.HACK, 10,
            Card.Type.MONITORING, 10,
            Card.Type.DOGECOIN, 10,
            Card.Type.FRAMEWORK, 8
        ));

        Decision playHack = new PlayCardDecision(new Card(Card.Type.HACK, 1));
        Decision endAction = new EndPhaseDecision(GameState.TurnPhase.ACTION);

        Decision chosen = player.makeDecision(state, ImmutableList.of(playHack, endAction));

        assertSame(playHack, chosen);
    }

    @Test
    public void makeDecision_skipsAttackCardsAfterOneAttackWhenMonitoringIsInGame() {
        BalancedAutoPlayer player = new BalancedAutoPlayer();
        GameState state = actionStateWithSupply(
            0,
            ImmutableMap.of(
                Card.Type.BUG, 10,
                Card.Type.EVERGREEN_TEST, 10,
                Card.Type.HACK, 10,
                Card.Type.MONITORING, 10,
                Card.Type.DOGECOIN, 10,
                Card.Type.FRAMEWORK, 8
            ),
            ImmutableList.of(new Card(Card.Type.EVERGREEN_TEST, 1))
        );

        Decision playHack = new PlayCardDecision(new Card(Card.Type.HACK, 2));
        Decision endAction = new EndPhaseDecision(GameState.TurnPhase.ACTION);

        Decision chosen = player.makeDecision(state, ImmutableList.of(playHack, endAction));

        assertSame(endAction, chosen);
    }

    @Test
    public void makeDecision_skipsHackAfterHackWasAlreadyPlayedThisTurn() {
        BalancedAutoPlayer player = new BalancedAutoPlayer();
        GameState state = actionStateWithSupply(
            0,
            ImmutableMap.of(
                Card.Type.HACK, 10,
                Card.Type.DOGECOIN, 10,
                Card.Type.FRAMEWORK, 8
            ),
            ImmutableList.of(new Card(Card.Type.HACK, 1))
        );

        Decision playHack = new PlayCardDecision(new Card(Card.Type.HACK, 2));
        Decision endAction = new EndPhaseDecision(GameState.TurnPhase.ACTION);

        Decision chosen = player.makeDecision(state, ImmutableList.of(playHack, endAction));

        assertSame(endAction, chosen);
    }

    @Test
    public void makeDecision_prioritizesCardAddingActionsOverBuyOnlyActions() {
        BalancedAutoPlayer player = new BalancedAutoPlayer();

        Decision playDeploymentPipeline = new PlayCardDecision(new Card(Card.Type.DEPLOYMENT_PIPELINE, 1));
        Decision playDailyScrum = new PlayCardDecision(new Card(Card.Type.DAILY_SCRUM, 2));
        Decision endAction = new EndPhaseDecision(GameState.TurnPhase.ACTION);

        Decision chosen = player.makeDecision(null, ImmutableList.of(playDeploymentPipeline, playDailyScrum, endAction));

        assertSame(playDailyScrum, chosen);
    }

    @Test
    public void makeDecision_playsDeploymentPipelineWhenOneMoneyShortOfObjective() {
        BalancedAutoPlayer player = new BalancedAutoPlayer();
        GameState state = actionStateWithMoney(5);

        Decision playDeploymentPipeline = new PlayCardDecision(new Card(Card.Type.DEPLOYMENT_PIPELINE, 1));
        Decision endAction = new EndPhaseDecision(GameState.TurnPhase.ACTION);

        Decision chosen = player.makeDecision(state, ImmutableList.of(playDeploymentPipeline, endAction));

        assertSame(playDeploymentPipeline, chosen);
    }

    @Test
    public void makeDecision_playsDeploymentPipelineWhenOneMoneyShortOfFramework() {
        BalancedAutoPlayer player = new BalancedAutoPlayer();
        GameState state = actionStateWithMoney(7);

        Decision playDeploymentPipeline = new PlayCardDecision(new Card(Card.Type.DEPLOYMENT_PIPELINE, 1));
        Decision endAction = new EndPhaseDecision(GameState.TurnPhase.ACTION);

        Decision chosen = player.makeDecision(state, ImmutableList.of(playDeploymentPipeline, endAction));

        assertSame(playDeploymentPipeline, chosen);
    }

    @Test
    public void makeDecision_playsPlusActionBeforeDeploymentPipeline() {
        BalancedAutoPlayer player = new BalancedAutoPlayer();
        GameState state = actionStateWithMoney(5);

        Decision playDeploymentPipeline = new PlayCardDecision(new Card(Card.Type.DEPLOYMENT_PIPELINE, 1));
        Decision playCodeReview = new PlayCardDecision(new Card(Card.Type.CODE_REVIEW, 2));
        Decision endAction = new EndPhaseDecision(GameState.TurnPhase.ACTION);

        Decision chosen = player.makeDecision(state, ImmutableList.of(
            playDeploymentPipeline,
            playCodeReview,
            endAction
        ));

        assertSame(playCodeReview, chosen);
    }

    @Test
    public void makeDecision_skipsDeploymentPipelineWhenNotOneMoneyShortOfObjective() {
        BalancedAutoPlayer player = new BalancedAutoPlayer();
        GameState state = actionStateWithMoney(6);

        Decision playDeploymentPipeline = new PlayCardDecision(new Card(Card.Type.DEPLOYMENT_PIPELINE, 1));
        Decision endAction = new EndPhaseDecision(GameState.TurnPhase.ACTION);

        Decision chosen = player.makeDecision(state, ImmutableList.of(playDeploymentPipeline, endAction));

        assertSame(endAction, chosen);
    }

    @Test
    public void makeDecision_usesBuysAsTieBreakerAfterActionsAndCards() {
        BalancedAutoPlayer player = new BalancedAutoPlayer();

        Decision playIpo = new PlayCardDecision(new Card(Card.Type.IPO, 1));
        Decision playSprintPlanning = new PlayCardDecision(new Card(Card.Type.SPRINT_PLANNING, 2));
        Decision endAction = new EndPhaseDecision(GameState.TurnPhase.ACTION);

        Decision chosen = player.makeDecision(null, ImmutableList.of(playIpo, playSprintPlanning, endAction));

        assertSame(playSprintPlanning, chosen);
    }

    @Test
    public void makeDecision_playsHighestValueMoneyCardBeforeEndingMoneyPhase() {
        BalancedAutoPlayer player = new BalancedAutoPlayer();

        Decision playBitcoin = new PlayCardDecision(new Card(Card.Type.BITCOIN, 1));
        Decision playDogecoin = new PlayCardDecision(new Card(Card.Type.DOGECOIN, 2));
        Decision endMoney = new EndPhaseDecision(GameState.TurnPhase.MONEY);

        Decision chosen = player.makeDecision(null, ImmutableList.of(playBitcoin, playDogecoin, endMoney));

        assertSame(playDogecoin, chosen);
    }

    @Test
    public void makeDecision_discardsPointCardsDuringBacklogBeforeStopping() {
        BalancedAutoPlayer player = new BalancedAutoPlayer();

        Decision discardBitcoin = new DiscardCardDecision(new Card(Card.Type.BITCOIN, 1));
        Decision discardMethod = new DiscardCardDecision(new Card(Card.Type.METHOD, 2));
        Decision discardIpo = new DiscardCardDecision(new Card(Card.Type.IPO, 3));
        Decision endDiscard = new EndPhaseDecision(GameState.TurnPhase.DISCARD);

        Decision chosen = player.makeDecision(null, ImmutableList.of(
            discardBitcoin,
            discardMethod,
            discardIpo,
            endDiscard
        ));

        assertSame(discardMethod, chosen);
    }

    @Test
    public void makeDecision_stopsBacklogDiscardWhenNoPointCardsRemain() {
        BalancedAutoPlayer player = new BalancedAutoPlayer();

        Decision discardBitcoin = new DiscardCardDecision(new Card(Card.Type.BITCOIN, 1));
        Decision discardIpo = new DiscardCardDecision(new Card(Card.Type.IPO, 2));
        Decision endDiscard = new EndPhaseDecision(GameState.TurnPhase.DISCARD);

        Decision chosen = player.makeDecision(null, ImmutableList.of(
            discardBitcoin,
            discardIpo,
            endDiscard
        ));

        assertSame(endDiscard, chosen);
    }

    private static void buyCard(BalancedAutoPlayer player, Card.Type cardType) {
        player.makeDecision(null, ImmutableList.of(
            new BuyDecision(cardType),
            new EndPhaseDecision(GameState.TurnPhase.BUY)
        ));
    }

    private static GameState actionStateWithMoney(int spendableMoney) {
        return actionStateWithSupply(spendableMoney, ImmutableMap.of(
            Card.Type.DOGECOIN, 10,
            Card.Type.FRAMEWORK, 8
        ));
    }

    private static GameState actionStateWithSupply(int spendableMoney, ImmutableMap<Card.Type, Integer> supply) {
        return actionStateWithSupply(spendableMoney, supply, ImmutableList.of());
    }

    private static GameState actionStateWithSupply(int spendableMoney, ImmutableMap<Card.Type, Integer> supply,
                                                   ImmutableList<Card> playedCards) {
        return new GameState(
            "BalancedAutoPlayer",
            new Hand(playedCards, ImmutableList.of()),
            GameState.TurnPhase.ACTION,
            1,
            spendableMoney,
            1,
            new CardStacks(supply)
        );
    }

    private static GameState parallelizationComboState(ImmutableList<Card> unplayedCards) {
        return new GameState(
            "BalancedAutoPlayer",
            new Hand(
                ImmutableList.of(new Card(Card.Type.PARALLELIZATION, 1)),
                unplayedCards
            ),
            GameState.TurnPhase.ACTION,
            0,
            0,
            1,
            new CardStacks(ImmutableMap.of(
                Card.Type.DOGECOIN, 10,
                Card.Type.FRAMEWORK, 8
            ))
        );
    }

    private static BalancedAutoPlayer playerWithFiveBoughtActions() {
        BalancedAutoPlayer player = new BalancedAutoPlayer();
        buyCard(player, Card.Type.IPO);
        buyCard(player, Card.Type.IPO);
        buyCard(player, Card.Type.IPO);
        buyCard(player, Card.Type.MONITORING);
        buyCard(player, Card.Type.MONITORING);
        return player;
    }
}
