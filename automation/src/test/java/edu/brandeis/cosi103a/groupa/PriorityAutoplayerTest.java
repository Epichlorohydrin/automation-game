package edu.brandeis.cosi103a.groupa;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import edu.brandeis.cosi.atg.cards.Card;
import edu.brandeis.cosi.atg.decisions.BuyDecision;
import edu.brandeis.cosi.atg.decisions.ChooseEffectDecision;
import edu.brandeis.cosi.atg.decisions.Decision;
import edu.brandeis.cosi.atg.decisions.DiscardCardDecision;
import edu.brandeis.cosi.atg.decisions.EndPhaseDecision;
import edu.brandeis.cosi.atg.decisions.GainCardDecision;
import edu.brandeis.cosi.atg.decisions.PlayCardDecision;
import edu.brandeis.cosi.atg.decisions.TrashCardDecision;
import edu.brandeis.cosi.atg.state.CardStacks;
import edu.brandeis.cosi.atg.state.GameState;
import edu.brandeis.cosi.atg.state.Hand;
import edu.brandeis.cosi103a.groupa.players.PriorityAutoplayer;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * Unit tests for PriorityAutoplayer.
 */
public class PriorityAutoplayerTest {

    @Test
    public void getName_returnsStableName() {
        PriorityAutoplayer player = new PriorityAutoplayer();

        assertEquals("PriorityAutoplayer", player.getName());
        assertEquals(player.getName(), player.getName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void makeDecision_throwsOnEmptyOptions() {
        new PriorityAutoplayer().makeDecision(null, ImmutableList.of());
    }

    @Test
    public void makeDecision_returnsOnlyOptionWhenSingle() {
        PriorityAutoplayer player = new PriorityAutoplayer();
        Decision only = new EndPhaseDecision(GameState.TurnPhase.BUY);

        Decision chosen = player.makeDecision(null, ImmutableList.of(only));

        assertSame(only, chosen);
    }

    @Test
    public void makeDecision_prefersConfiguredBuyPriority() {
        PriorityAutoplayer player = new PriorityAutoplayer();
        Decision buyMethod = new BuyDecision(Card.Type.METHOD);
        Decision buyFramework = new BuyDecision(Card.Type.FRAMEWORK);
        Decision buyRefactor = new BuyDecision(Card.Type.REFACTOR);

        Decision chosen = player.makeDecision(null, ImmutableList.of(buyMethod, buyFramework, buyRefactor));

        assertSame(buyFramework, chosen);
    }

    @Test
    public void makeDecision_skipsBuyingWhenOnlyNonPriorityBuysAreAvailable() {
        PriorityAutoplayer player = new PriorityAutoplayer();
        Decision end = new EndPhaseDecision(GameState.TurnPhase.BUY);
        Decision buyHack = new BuyDecision(Card.Type.HACK);
        Decision buyDailyScrum = new BuyDecision(Card.Type.DAILY_SCRUM);

        Decision chosen = player.makeDecision(null, ImmutableList.of(end, buyHack, buyDailyScrum));

        assertSame(end, chosen);
    }

    @Test
    public void makeDecision_inBuyContextDoesNotFallbackToPlayChoices() {
        PriorityAutoplayer player = new PriorityAutoplayer();
        Decision end = new EndPhaseDecision(GameState.TurnPhase.BUY);
        Decision buyHack = new BuyDecision(Card.Type.HACK);
        Decision playDogecoin = new PlayCardDecision(new Card(Card.Type.DOGECOIN, 3));

        Decision chosen = player.makeDecision(null, ImmutableList.of(playDogecoin, buyHack, end));

        assertSame(end, chosen);
    }

    @Test(expected = IllegalStateException.class)
    public void makeDecision_inBuyContextThrowsWhenNoPriorityBuyAndNoBuyEndOption() {
        PriorityAutoplayer player = new PriorityAutoplayer();
        Decision buyHack = new BuyDecision(Card.Type.HACK);
        Decision playDogecoin = new PlayCardDecision(new Card(Card.Type.DOGECOIN, 3));

        player.makeDecision(null, ImmutableList.of(playDogecoin, buyHack));
    }

    @Test
    public void makeDecision_skipsModuleUntilPlayerHasMoreThanTwentyFiveCards() {
        PriorityAutoplayer player = new PriorityAutoplayer();
        Decision buyModule = new BuyDecision(Card.Type.MODULE);
        Decision buyDogecoin = new BuyDecision(Card.Type.DOGECOIN);

        ImmutableList<Card> visibleCards = ImmutableList.of(
            new Card(Card.Type.BITCOIN, 1), new Card(Card.Type.BITCOIN, 1), new Card(Card.Type.BITCOIN, 1),
            new Card(Card.Type.BITCOIN, 1), new Card(Card.Type.BITCOIN, 1), new Card(Card.Type.BITCOIN, 1),
            new Card(Card.Type.BITCOIN, 1), new Card(Card.Type.BITCOIN, 1), new Card(Card.Type.BITCOIN, 1),
            new Card(Card.Type.BITCOIN, 1), new Card(Card.Type.BITCOIN, 1), new Card(Card.Type.BITCOIN, 1),
            new Card(Card.Type.BITCOIN, 1), new Card(Card.Type.BITCOIN, 1), new Card(Card.Type.BITCOIN, 1),
            new Card(Card.Type.BITCOIN, 1), new Card(Card.Type.BITCOIN, 1), new Card(Card.Type.BITCOIN, 1),
            new Card(Card.Type.BITCOIN, 1), new Card(Card.Type.BITCOIN, 1), new Card(Card.Type.BITCOIN, 1),
            new Card(Card.Type.BITCOIN, 1), new Card(Card.Type.BITCOIN, 1), new Card(Card.Type.BITCOIN, 1),
            new Card(Card.Type.BITCOIN, 1)
        );

        Decision chosen = player.makeDecision(
            createGameState(ImmutableList.of(), visibleCards, 1, 8),
            ImmutableList.of(buyDogecoin, buyModule)
        );

        assertSame(buyDogecoin, chosen);
    }

    @Test
    public void makeDecision_prefersDogecoinOverModuleWhenModuleBecomesAvailable() {
        PriorityAutoplayer player = new PriorityAutoplayer();
        Decision buyModule = new BuyDecision(Card.Type.MODULE);
        Decision buyDogecoin = new BuyDecision(Card.Type.DOGECOIN);

        ImmutableList<Card> visibleCards = ImmutableList.of(
            new Card(Card.Type.BITCOIN, 1), new Card(Card.Type.BITCOIN, 1), new Card(Card.Type.BITCOIN, 1),
            new Card(Card.Type.BITCOIN, 1), new Card(Card.Type.BITCOIN, 1), new Card(Card.Type.BITCOIN, 1),
            new Card(Card.Type.BITCOIN, 1), new Card(Card.Type.BITCOIN, 1), new Card(Card.Type.BITCOIN, 1),
            new Card(Card.Type.BITCOIN, 1), new Card(Card.Type.BITCOIN, 1), new Card(Card.Type.BITCOIN, 1),
            new Card(Card.Type.BITCOIN, 1), new Card(Card.Type.BITCOIN, 1), new Card(Card.Type.BITCOIN, 1),
            new Card(Card.Type.BITCOIN, 1), new Card(Card.Type.BITCOIN, 1), new Card(Card.Type.BITCOIN, 1),
            new Card(Card.Type.BITCOIN, 1), new Card(Card.Type.BITCOIN, 1), new Card(Card.Type.BITCOIN, 1),
            new Card(Card.Type.BITCOIN, 1), new Card(Card.Type.BITCOIN, 1), new Card(Card.Type.BITCOIN, 1),
            new Card(Card.Type.BITCOIN, 1), new Card(Card.Type.BITCOIN, 1)
        );

        Decision chosen = player.makeDecision(
            createGameState(ImmutableList.of(), visibleCards, 1, 8),
            ImmutableList.of(buyDogecoin, buyModule)
        );

        assertSame(buyDogecoin, chosen);
    }

    @Test
    public void makeDecision_skipsRefactorWhenAtMaxAndMovesToNextPriority() {
        PriorityAutoplayer player = new PriorityAutoplayer();
        Decision buyRefactor = new BuyDecision(Card.Type.REFACTOR);
        Decision buyTechDebt = new BuyDecision(Card.Type.TECH_DEBT);

        ImmutableList<Card> visibleCards = ImmutableList.of(
            new Card(Card.Type.REFACTOR, 1),
            new Card(Card.Type.REFACTOR, 2),
            new Card(Card.Type.REFACTOR, 3)
        );

        Decision chosen = player.makeDecision(
            createGameState(ImmutableList.of(), visibleCards, 1, 8),
            ImmutableList.of(buyTechDebt, buyRefactor)
        );

        assertSame(buyTechDebt, chosen);
    }

    @Test
    public void makeDecision_skipsUnitTestWhenAtMaxAndFallsBackToEthereum() {
        PriorityAutoplayer player = new PriorityAutoplayer();
        Decision buyUnitTest = new BuyDecision(Card.Type.UNIT_TEST);
        Decision buyEthereum = new BuyDecision(Card.Type.ETHEREUM);

        ImmutableList<Card> visibleCards = ImmutableList.of(
            new Card(Card.Type.UNIT_TEST, 1),
            new Card(Card.Type.UNIT_TEST, 2),
            new Card(Card.Type.UNIT_TEST, 3),
            new Card(Card.Type.UNIT_TEST, 4)
        );

        Decision chosen = player.makeDecision(
            createGameState(ImmutableList.of(), visibleCards, 1, 8),
            ImmutableList.of(buyEthereum, buyUnitTest)
        );

        assertSame(buyEthereum, chosen);
    }

    @Test
    public void makeDecision_skipsGainWhenOnlyNonPriorityGainCardsAreAvailable() {
        PriorityAutoplayer player = new PriorityAutoplayer();
        Decision endGain = new EndPhaseDecision(GameState.TurnPhase.GAIN);
        Decision gainBug = new GainCardDecision(Card.Type.BUG);
        Decision gainMethod = new GainCardDecision(Card.Type.METHOD);

        Decision chosen = player.makeDecision(null, ImmutableList.of(gainBug, gainMethod, endGain));

        assertSame(endGain, chosen);
    }

    @Test
    public void makeDecision_mandatoryGainFallsBackToBitcoinWhenOffered() {
        PriorityAutoplayer player = new PriorityAutoplayer();
        Decision gainBug = new GainCardDecision(Card.Type.BUG);
        Decision gainBitcoin = new GainCardDecision(Card.Type.BITCOIN);
        Decision gainMethod = new GainCardDecision(Card.Type.METHOD);

        Decision chosen = player.makeDecision(null, ImmutableList.of(gainBug, gainMethod, gainBitcoin));

        assertSame(gainBitcoin, chosen);
    }

    @Test(expected = IllegalStateException.class)
    public void makeDecision_throwsOnMandatoryGainWhenBitcoinIsNotOffered() {
        PriorityAutoplayer player = new PriorityAutoplayer();
        Decision gainBug = new GainCardDecision(Card.Type.BUG);
        Decision gainMethod = new GainCardDecision(Card.Type.METHOD);

        player.makeDecision(null, ImmutableList.of(gainBug, gainMethod));
    }

    @Test
    public void makeDecision_prefersBuyPriorityWhenChoosingGainCard() {
        PriorityAutoplayer player = new PriorityAutoplayer();
        Decision gainBug = new GainCardDecision(Card.Type.BUG);
        Decision gainMethod = new GainCardDecision(Card.Type.METHOD);
        Decision gainEthereum = new GainCardDecision(Card.Type.ETHEREUM);

        Decision chosen = player.makeDecision(null, ImmutableList.of(gainBug, gainMethod, gainEthereum));

        assertSame(gainEthereum, chosen);
    }

    @Test
    public void makeDecision_prefersConfiguredPlayPriority() {
        PriorityAutoplayer player = new PriorityAutoplayer();
        Decision playBitcoin = new PlayCardDecision(new Card(Card.Type.BITCOIN, 1));
        Decision playUnitTest = new PlayCardDecision(new Card(Card.Type.UNIT_TEST, 2));
        Decision playDogecoin = new PlayCardDecision(new Card(Card.Type.DOGECOIN, 3));

        Decision chosen = player.makeDecision(null, ImmutableList.of(playBitcoin, playDogecoin, playUnitTest));

        assertSame(playUnitTest, chosen);
    }

    @Test
    public void makeDecision_choosesBugToTrashBeforeOtherCards() {
        PriorityAutoplayer player = new PriorityAutoplayer();
        Decision trashMethod = new TrashCardDecision(new Card(Card.Type.METHOD, 1));
        Decision trashBug = new TrashCardDecision(new Card(Card.Type.BUG, 2));
        Decision trashBitcoin = new TrashCardDecision(new Card(Card.Type.BITCOIN, 3));

        Decision chosen = player.makeDecision(null, ImmutableList.of(trashMethod, trashBitcoin, trashBug));

        assertSame(trashBug, chosen);
    }

    @Test
    public void makeDecision_choosesBitcoinBeforeDogecoinToTrash() {
        PriorityAutoplayer player = new PriorityAutoplayer();
        Decision trashDogecoin = new TrashCardDecision(new Card(Card.Type.DOGECOIN, 3));
        Decision trashBitcoin = new TrashCardDecision(new Card(Card.Type.BITCOIN, 3));

        Decision chosen = player.makeDecision(null, ImmutableList.of(trashDogecoin, trashBitcoin));

        assertSame(trashBitcoin, chosen);
    }

    @Test
    public void makeDecision_skipsTrashWhenOnlyNonPriorityTrashCardsAreAvailable() {
        PriorityAutoplayer player = new PriorityAutoplayer();
        Decision endAction = new EndPhaseDecision(GameState.TurnPhase.ACTION);
        Decision trashMethod = new TrashCardDecision(new Card(Card.Type.METHOD, 1));
        Decision trashDailyScrum = new TrashCardDecision(new Card(Card.Type.DAILY_SCRUM, 3));

        Decision chosen = player.makeDecision(null, ImmutableList.of(trashMethod, trashDailyScrum, endAction));

        assertSame(endAction, chosen);
    }

    @Test
    public void makeDecision_mandatoryTrashFallsBackToLowestCostCard() {
        PriorityAutoplayer player = new PriorityAutoplayer();
        Decision trashMethod = new TrashCardDecision(new Card(Card.Type.METHOD, 1)); // cost 2
        Decision trashSprintPlanning = new TrashCardDecision(new Card(Card.Type.SPRINT_PLANNING, 3)); // cost 3
        Decision trashDailyScrum = new TrashCardDecision(new Card(Card.Type.DAILY_SCRUM, 4)); // cost 5

        Decision chosen = player.makeDecision(null,
            ImmutableList.of(trashDailyScrum, trashSprintPlanning, trashMethod));

        assertSame(trashMethod, chosen);
    }

    @Test
    public void makeDecision_choosesBugToDiscardBeforeOtherCards() {
        PriorityAutoplayer player = new PriorityAutoplayer();
        Decision discardMethod = new DiscardCardDecision(new Card(Card.Type.METHOD, 1));
        Decision discardBug = new DiscardCardDecision(new Card(Card.Type.BUG, 2));
        Decision discardBitcoin = new DiscardCardDecision(new Card(Card.Type.BITCOIN, 3));

        Decision chosen = player.makeDecision(null, ImmutableList.of(discardMethod, discardBitcoin, discardBug));

        assertSame(discardBug, chosen);
    }

    @Test
    public void makeDecision_usesPlusTwoMoneyWhenSpendableMoneyIsBetweenSixAndEight() {
        PriorityAutoplayer player = new PriorityAutoplayer();
        Decision plusActions = new ChooseEffectDecision(ChooseEffectDecision.Effect.UNIT_TEST_PLUS_TWO_ACTIONS);
        Decision plusMoney = new ChooseEffectDecision(ChooseEffectDecision.Effect.UNIT_TEST_PLUS_TWO_MONEY);
        Decision plusCards = new ChooseEffectDecision(ChooseEffectDecision.Effect.UNIT_TEST_PLUS_TWO_CARDS);

        Decision chosen = player.makeDecision(createGameState(ImmutableList.of(), ImmutableList.of(), 0, 7),
            ImmutableList.of(plusActions, plusMoney, plusCards));

        assertSame(plusMoney, chosen);
    }

    @Test
    public void makeDecision_usesDrawTwoCardsWhenVisibleCardCountExceedsTwenty() {
        PriorityAutoplayer player = new PriorityAutoplayer();
        Decision plusActions = new ChooseEffectDecision(ChooseEffectDecision.Effect.UNIT_TEST_PLUS_TWO_ACTIONS);
        Decision plusMoney = new ChooseEffectDecision(ChooseEffectDecision.Effect.UNIT_TEST_PLUS_TWO_MONEY);
        Decision plusCards = new ChooseEffectDecision(ChooseEffectDecision.Effect.UNIT_TEST_PLUS_TWO_CARDS);

        ImmutableList<Card> visibleCards = ImmutableList.of(
            new Card(Card.Type.METHOD, 1), new Card(Card.Type.METHOD, 2), new Card(Card.Type.METHOD, 3),
            new Card(Card.Type.METHOD, 4), new Card(Card.Type.METHOD, 5), new Card(Card.Type.METHOD, 6),
            new Card(Card.Type.METHOD, 7), new Card(Card.Type.METHOD, 8), new Card(Card.Type.METHOD, 9),
            new Card(Card.Type.METHOD, 10), new Card(Card.Type.METHOD, 11), new Card(Card.Type.METHOD, 12),
            new Card(Card.Type.METHOD, 13), new Card(Card.Type.METHOD, 14), new Card(Card.Type.METHOD, 15),
            new Card(Card.Type.METHOD, 16), new Card(Card.Type.METHOD, 17), new Card(Card.Type.METHOD, 18),
            new Card(Card.Type.METHOD, 19), new Card(Card.Type.METHOD, 20), new Card(Card.Type.METHOD, 21)
        );

        Decision chosen = player.makeDecision(createGameState(ImmutableList.of(), visibleCards, 0, 6),
            ImmutableList.of(plusActions, plusMoney, plusCards));

        assertSame(plusCards, chosen);
    }

    @Test
    public void makeDecision_usesPlusTwoActionsWhenActionCardExistsAndNoActionsRemain() {
        PriorityAutoplayer player = new PriorityAutoplayer();
        Decision plusActions = new ChooseEffectDecision(ChooseEffectDecision.Effect.UNIT_TEST_PLUS_TWO_ACTIONS);
        Decision plusMoney = new ChooseEffectDecision(ChooseEffectDecision.Effect.UNIT_TEST_PLUS_TWO_MONEY);
        Decision plusCards = new ChooseEffectDecision(ChooseEffectDecision.Effect.UNIT_TEST_PLUS_TWO_CARDS);

        Decision chosen = player.makeDecision(createGameState(ImmutableList.of(),
                ImmutableList.of(new Card(Card.Type.UNIT_TEST, 2)), 0, 6),
            ImmutableList.of(plusActions, plusMoney, plusCards));

        assertSame(plusActions, chosen);
    }

    @Test
    public void makeDecision_defaultsToPlusTwoMoneyWhenOtherRulesDoNotApply() {
        PriorityAutoplayer player = new PriorityAutoplayer();
        Decision plusActions = new ChooseEffectDecision(ChooseEffectDecision.Effect.UNIT_TEST_PLUS_TWO_ACTIONS);
        Decision plusMoney = new ChooseEffectDecision(ChooseEffectDecision.Effect.UNIT_TEST_PLUS_TWO_MONEY);
        Decision plusCards = new ChooseEffectDecision(ChooseEffectDecision.Effect.UNIT_TEST_PLUS_TWO_CARDS);

        Decision chosen = player.makeDecision(createGameState(ImmutableList.of(),
                ImmutableList.of(new Card(Card.Type.BITCOIN, 1)), 1, 6),
            ImmutableList.of(plusActions, plusMoney, plusCards));

        assertSame(plusMoney, chosen);
    }

    @Test
    public void makeDecision_prefersEndingBuyPhaseWhenOnlyNonPriorityBuyExists() {
        PriorityAutoplayer player = new PriorityAutoplayer();
        Decision end = new EndPhaseDecision(GameState.TurnPhase.BUY);
        Decision playDogecoin = new PlayCardDecision(new Card(Card.Type.DOGECOIN, 1));
        Decision buyMethod = new BuyDecision(Card.Type.METHOD);

        Decision chosen = player.makeDecision(null, ImmutableList.of(end, playDogecoin, buyMethod));

        assertSame(end, chosen);
    }
    private GameState createGameState(ImmutableList<Card> playedCards,
                                      ImmutableList<Card> unplayedCards,
                                      int availableActions,
                                      int spendableMoney) {
        Hand hand = new Hand(ImmutableSet.copyOf(playedCards), ImmutableSet.copyOf(unplayedCards));
        CardStacks stacks = new CardStacks(ImmutableMap.of(Card.Type.BITCOIN, 10));
        return new GameState(
            "TestPlayer",
            hand,
            GameState.TurnPhase.ACTION,
            availableActions,
            spendableMoney,
            1,
            stacks
        );
    }

}
