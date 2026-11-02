package edu.brandeis.cosi103a.groupa;

import com.google.common.collect.ImmutableList;
import edu.brandeis.cosi.atg.cards.Card;
import edu.brandeis.cosi.atg.decisions.ChooseEffectDecision;
import edu.brandeis.cosi.atg.decisions.Decision;
import edu.brandeis.cosi.atg.decisions.DiscardCardDecision;
import edu.brandeis.cosi.atg.decisions.EndPhaseDecision;
import edu.brandeis.cosi.atg.decisions.GainCardDecision;
import edu.brandeis.cosi.atg.decisions.PlayCardDecision;
import edu.brandeis.cosi.atg.decisions.TrashCardDecision;
import edu.brandeis.cosi.atg.engine.PlayerViolationException;
import edu.brandeis.cosi.atg.event.GameEvent;
import edu.brandeis.cosi.atg.event.Event;
import edu.brandeis.cosi.atg.event.GameObserver;
import edu.brandeis.cosi.atg.player.Player;
import edu.brandeis.cosi.atg.state.GameState;
import org.junit.Test;

import java.util.ArrayDeque;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ActionCardResolverTest {

    private final ActionCardResolver resolver = new ActionCardResolver();

    @Test
    public void backlog_discardsChosenCardAndDrawsReplacement() throws PlayerViolationException {
        GameEngine engine = engine(player("P1", new DiscardCardDecision(card(Card.Type.ETHEREUM, 2)),
            new EndPhaseDecision(GameState.TurnPhase.DISCARD)));
        DeckState active = state(card(Card.Type.BACKLOG, 1), card(Card.Type.ETHEREUM, 2), card(Card.Type.DOGECOIN, 3));
        seedDrawPile(active, card(Card.Type.BITCOIN, 4));
        replaceDecks(engine, active);
        playCard(active, card(Card.Type.BACKLOG, 1));
        active.actions = 1;

        resolver.resolve(engine, 0, active, card(Card.Type.BACKLOG, 1));

        assertEquals(2, active.actions);
        assertFalse(active.unplayedHand.contains(card(Card.Type.ETHEREUM, 2)));
        assertTrue(active.discardPile.contains(card(Card.Type.ETHEREUM, 2)));
        assertTrue(active.unplayedHand.contains(card(Card.Type.BITCOIN, 4)));
        assertTrue(active.playedHand.contains(card(Card.Type.BACKLOG, 1)));
    }

    @Test
    public void backlog_offersDiscardAndEndPhaseAndCanStopImmediately() throws PlayerViolationException {
        ScriptedPlayer scriptedPlayer = player("P1", new EndPhaseDecision(GameState.TurnPhase.DISCARD));
        GameEngine engine = engine(scriptedPlayer);
        DeckState active = state(card(Card.Type.BACKLOG, 1), card(Card.Type.ETHEREUM, 2), card(Card.Type.DOGECOIN, 3));
        replaceDecks(engine, active);
        playCard(active, card(Card.Type.BACKLOG, 1));

        resolver.resolve(engine, 0, active, card(Card.Type.BACKLOG, 1));

        assertTrue(containsDecisionType(scriptedPlayer.promptOptions().get(0), DiscardCardDecision.class));
        assertTrue(containsDecisionType(scriptedPlayer.promptOptions().get(0), EndPhaseDecision.class));
        assertEquals(2, active.unplayedHand.size());
        assertTrue(active.discardPile.isEmpty());
        assertTrue(active.playedHand.contains(card(Card.Type.BACKLOG, 1)));
    }

    @Test
    public void backlog_discardMultipleCardsInSequenceThenDrawsSameNumber() throws PlayerViolationException {
        ScriptedPlayer scriptedPlayer = player("P1",
            new DiscardCardDecision(card(Card.Type.BITCOIN, 2)),
            new DiscardCardDecision(card(Card.Type.ETHEREUM, 3)),
            new EndPhaseDecision(GameState.TurnPhase.DISCARD));
        GameEngine engine = engine(scriptedPlayer);
        DeckState active = state(
            card(Card.Type.BACKLOG, 1),
            card(Card.Type.BITCOIN, 2),
            card(Card.Type.ETHEREUM, 3),
            card(Card.Type.DOGECOIN, 4)
        );
        seedDrawPile(active, card(Card.Type.METHOD, 5), card(Card.Type.MODULE, 6));
        replaceDecks(engine, active);
        playCard(active, card(Card.Type.BACKLOG, 1));
        active.actions = 1;

        resolver.resolve(engine, 0, active, card(Card.Type.BACKLOG, 1));

        assertEquals(2, active.actions);
        assertEquals(2, active.discardPile.size());
        assertTrue(active.unplayedHand.contains(card(Card.Type.METHOD, 5)));
        assertTrue(active.unplayedHand.contains(card(Card.Type.MODULE, 6)));
        assertEquals(3, active.unplayedHand.size());
        assertEquals(3, scriptedPlayer.promptOptions().size());
        assertTrue(containsDecisionType(scriptedPlayer.promptOptions().get(0), EndPhaseDecision.class));
        assertTrue(containsDecisionType(scriptedPlayer.promptOptions().get(1), EndPhaseDecision.class));
        assertTrue(containsDecisionType(scriptedPlayer.promptOptions().get(2), EndPhaseDecision.class));
    }

    @Test
    public void backlog_zeroDiscardLeavesHandAndDoesNotDraw() throws PlayerViolationException {
        ScriptedPlayer scriptedPlayer = player("P1", new EndPhaseDecision(GameState.TurnPhase.DISCARD));
        GameEngine engine = engine(scriptedPlayer);
        DeckState active = state(card(Card.Type.BACKLOG, 1), card(Card.Type.ETHEREUM, 2), card(Card.Type.DOGECOIN, 3));
        seedDrawPile(active, card(Card.Type.METHOD, 4));
        replaceDecks(engine, active);
        playCard(active, card(Card.Type.BACKLOG, 1));
        active.actions = 1;

        resolver.resolve(engine, 0, active, card(Card.Type.BACKLOG, 1));

        assertEquals(2, active.actions);
        assertTrue(active.discardPile.isEmpty());
        assertEquals(2, active.unplayedHand.size());
        assertFalse(active.unplayedHand.contains(card(Card.Type.METHOD, 4)));
    }

    @Test
    public void dailyScrum_drawsFourForActiveAndOneForEachOtherPlayer() throws PlayerViolationException {
        GameEngine engine = engine(player("P1"), player("P2"));
        DeckState active = state(card(Card.Type.DAILY_SCRUM, 1));
        DeckState other = state();
        seedDrawPile(active,
            card(Card.Type.BITCOIN, 10), card(Card.Type.BITCOIN, 11), card(Card.Type.BITCOIN, 12),
            card(Card.Type.BITCOIN, 13)
        );
        seedDrawPile(other, card(Card.Type.ETHEREUM, 20));
        replaceDecks(engine, active, other);
        playCard(active, card(Card.Type.DAILY_SCRUM, 1));

        resolver.resolve(engine, 0, active, card(Card.Type.DAILY_SCRUM, 1));

        assertEquals(4, active.unplayedHand.size());
        assertEquals(1, other.unplayedHand.size());
        assertEquals(1, active.buys);
    }

    @Test
    public void dailyScrum_eachOtherPlayerDrawsExactlyOneInThreePlayerGame() throws PlayerViolationException {
        GameEngine engine = engine(player("P1"), player("P2"), player("P3"));
        DeckState active = state(card(Card.Type.DAILY_SCRUM, 1));
        DeckState otherOne = state();
        DeckState otherTwo = state();

        seedDrawPile(active,
            card(Card.Type.BITCOIN, 30), card(Card.Type.BITCOIN, 31),
            card(Card.Type.BITCOIN, 32), card(Card.Type.BITCOIN, 33)
        );
        seedDrawPile(otherOne, card(Card.Type.ETHEREUM, 40));
        seedDrawPile(otherTwo, card(Card.Type.DOGECOIN, 50));

        replaceDecks(engine, active, otherOne, otherTwo);
        playCard(active, card(Card.Type.DAILY_SCRUM, 1));

        resolver.resolve(engine, 0, active, card(Card.Type.DAILY_SCRUM, 1));

        assertEquals(4, active.unplayedHand.size());
        assertEquals(1, active.buys);
        assertEquals(1, otherOne.unplayedHand.size());
        assertEquals(1, otherTwo.unplayedHand.size());
    }

    @Test
    public void ipo_drawsCardsAndAddsActionAndMoney() throws PlayerViolationException {
        GameEngine engine = engine(player("P1"));
        DeckState active = state(card(Card.Type.IPO, 1));
        seedDrawPile(active, card(Card.Type.BITCOIN, 2), card(Card.Type.ETHEREUM, 3));
        replaceDecks(engine, active);
        playCard(active, card(Card.Type.IPO, 1));

        resolver.resolve(engine, 0, active, card(Card.Type.IPO, 1));

        assertEquals(1, active.actions);
        assertEquals(2, active.money);
        assertEquals(2, active.unplayedHand.size());
    }

    @Test
    public void hack_forcesOtherPlayersWithoutMonitoringToDiscardToThree() throws PlayerViolationException {
        ScriptedPlayer victimPlayer = player("P2",
            new DiscardCardDecision(card(Card.Type.BITCOIN, 20)),
            new DiscardCardDecision(card(Card.Type.ETHEREUM, 21)));
        GameEngine engine = engine(
            player("P1"),
            victimPlayer,
            player("P3", new PlayCardDecision(card(Card.Type.MONITORING, 30)))
        );
        DeckState active = state(card(Card.Type.HACK, 1));
        DeckState victim = state(
            card(Card.Type.BITCOIN, 20),
            card(Card.Type.ETHEREUM, 21),
            card(Card.Type.DOGECOIN, 22),
            card(Card.Type.METHOD, 23),
            card(Card.Type.MODULE, 24)
        );
        DeckState protectedPlayer = state(card(Card.Type.MONITORING, 30), card(Card.Type.BITCOIN, 31), card(Card.Type.BITCOIN, 32));
        replaceDecks(engine, active, victim, protectedPlayer);
        playCard(active, card(Card.Type.HACK, 1));

        RecordingObserver observer = new RecordingObserver();
        engine.setObserver(observer);

        resolver.resolve(engine, 0, active, card(Card.Type.HACK, 1));

        assertEquals(2, active.money);
        assertEquals(3, victim.unplayedHand.size());
        assertEquals(2, victim.discardPile.size());
        assertEquals(3, protectedPlayer.unplayedHand.size());
        assertTrue(protectedPlayer.discardPile.isEmpty());
        assertFalse(containsDecisionType(victimPlayer.promptOptions().get(0), EndPhaseDecision.class));
        assertTrue(protectedPlayer.unplayedHand.contains(card(Card.Type.MONITORING, 30)));
        assertTrue(observer.containsDescription("HACK"));
        assertTrue(observer.containsDescription("Monitoring"));
    }

    @Test
    public void monitoring_drawsTwoCards() throws PlayerViolationException {
        GameEngine engine = engine(player("P1"));
        DeckState active = state(card(Card.Type.MONITORING, 1));
        seedDrawPile(active, card(Card.Type.BITCOIN, 2), card(Card.Type.ETHEREUM, 3));
        replaceDecks(engine, active);
        playCard(active, card(Card.Type.MONITORING, 1));

        resolver.resolve(engine, 0, active, card(Card.Type.MONITORING, 1));

        assertEquals(2, active.unplayedHand.size());
        assertTrue(active.playedHand.contains(card(Card.Type.MONITORING, 1)));
    }

    @Test
    public void monitoringReactionLeavesCardInHandAndLogsAvoidance() throws PlayerViolationException {
        RecordingObserver observer = new RecordingObserver();
        ScriptedPlayer protectedPlayer = player("P2",
            new PlayCardDecision(card(Card.Type.MONITORING, 30)));
        GameEngine engine = engine(player("P1"), protectedPlayer);
        engine.setObserver(observer);
        DeckState active = state(card(Card.Type.HACK, 1));
        DeckState protectedState = state(card(Card.Type.MONITORING, 30), card(Card.Type.BITCOIN, 31));
        replaceDecks(engine, active, protectedState);
        playCard(active, card(Card.Type.HACK, 1));

        resolver.resolve(engine, 0, active, card(Card.Type.HACK, 1));

        assertTrue(protectedState.unplayedHand.contains(card(Card.Type.MONITORING, 30)));
        assertFalse(protectedState.playedHand.contains(card(Card.Type.MONITORING, 30)));
        assertTrue(observer.containsDescription("avoided HACK"));
    }

    @Test
    public void monitoringReactionCanBeDeclinedAndAttackStillApplies() throws PlayerViolationException {
        ScriptedPlayer protectedPlayer = player("P2",
            new EndPhaseDecision(GameState.TurnPhase.REACTION),
            new DiscardCardDecision(card(Card.Type.BITCOIN, 31)));
        GameEngine engine = engine(player("P1"), protectedPlayer);
        DeckState active = state(card(Card.Type.HACK, 1));
        DeckState protectedState = state(
            card(Card.Type.MONITORING, 30),
            card(Card.Type.BITCOIN, 31),
            card(Card.Type.ETHEREUM, 32),
            card(Card.Type.DOGECOIN, 33)
        );
        replaceDecks(engine, active, protectedState);
        playCard(active, card(Card.Type.HACK, 1));

        resolver.resolve(engine, 0, active, card(Card.Type.HACK, 1));

        assertEquals(3, protectedState.unplayedHand.size());
        assertEquals(1, protectedState.discardPile.size());
        assertTrue(protectedState.unplayedHand.contains(card(Card.Type.MONITORING, 30)));
        assertEquals(2, protectedPlayer.promptOptions().size());
        assertTrue(containsDecisionType(protectedPlayer.promptOptions().get(0), PlayCardDecision.class));
        assertTrue(containsDecisionType(protectedPlayer.promptOptions().get(0), EndPhaseDecision.class));
    }

    @Test
    public void techDebt_withOneEmptySupplyPileForcesOneDiscard()
        throws PlayerViolationException {
        ScriptedPlayer scriptedPlayer = player("P1",
            new DiscardCardDecision(card(Card.Type.BITCOIN, 2)));
        GameEngine engine = engine(scriptedPlayer);
        DeckState active = state(
            card(Card.Type.TECH_DEBT, 1),
            card(Card.Type.BITCOIN, 2),
            card(Card.Type.ETHEREUM, 3),
            card(Card.Type.DOGECOIN, 4)
        );
        seedDrawPile(active, card(Card.Type.METHOD, 5));
        replaceDecks(engine, active);
        playCard(active, card(Card.Type.TECH_DEBT, 1));
        engine.supplyCounts().put(Card.Type.BUG, 0);

        resolver.resolve(engine, 0, active, card(Card.Type.TECH_DEBT, 1));

        assertEquals(1, active.actions);
        assertEquals(1, active.money);
        assertEquals(1, active.discardPile.size());
        assertEquals(3, active.unplayedHand.size());
        assertTrue(containsType(active.unplayedHand, Card.Type.METHOD));
        assertFalse(containsDecisionType(scriptedPlayer.promptOptions().get(0), EndPhaseDecision.class));
    }

    @Test
    public void techDebt_withNoEmptySupplyPilesDoesNotPromptDiscard()
        throws PlayerViolationException {
        ScriptedPlayer scriptedPlayer = player("P1");
        GameEngine engine = engine(scriptedPlayer);
        DeckState active = state(
            card(Card.Type.TECH_DEBT, 1),
            card(Card.Type.BITCOIN, 2),
            card(Card.Type.ETHEREUM, 3)
        );
        seedDrawPile(active, card(Card.Type.DOGECOIN, 4));
        replaceDecks(engine, active);
        playCard(active, card(Card.Type.TECH_DEBT, 1));

        resolver.resolve(engine, 0, active, card(Card.Type.TECH_DEBT, 1));

        assertEquals(1, active.actions);
        assertEquals(1, active.money);
        assertEquals(0, active.discardPile.size());
        assertEquals(3, active.unplayedHand.size());
        assertTrue(scriptedPlayer.promptOptions().isEmpty());
    }

    @Test
    public void techDebt_withMultipleEmptySupplyPilesForcesMatchingDiscardCount()
        throws PlayerViolationException {
        ScriptedPlayer scriptedPlayer = player("P1",
            new DiscardCardDecision(card(Card.Type.BITCOIN, 2)),
            new DiscardCardDecision(card(Card.Type.ETHEREUM, 3)));
        GameEngine engine = engine(scriptedPlayer);
        DeckState active = state(
            card(Card.Type.TECH_DEBT, 1),
            card(Card.Type.BITCOIN, 2),
            card(Card.Type.ETHEREUM, 3),
            card(Card.Type.DOGECOIN, 4)
        );
        seedDrawPile(active, card(Card.Type.METHOD, 5));
        replaceDecks(engine, active);
        playCard(active, card(Card.Type.TECH_DEBT, 1));
        engine.supplyCounts().put(Card.Type.BUG, 0);
        engine.supplyCounts().put(Card.Type.MODULE, 0);

        resolver.resolve(engine, 0, active, card(Card.Type.TECH_DEBT, 1));

        assertEquals(2, scriptedPlayer.promptOptions().size());
        assertEquals(2, active.discardPile.size());
        assertEquals(2, active.unplayedHand.size());
        assertTrue(containsType(active.unplayedHand, Card.Type.DOGECOIN));
        assertTrue(containsType(active.unplayedHand, Card.Type.METHOD));
    }

    @Test
    public void refactor_requiresTrashAndOffersMandatoryGainChoices() throws PlayerViolationException {
        ScriptedPlayer scriptedPlayer = player("P1",
            new TrashCardDecision(card(Card.Type.METHOD, 2)),
            new GainCardDecision(Card.Type.MONITORING));
        GameEngine engine = engine(scriptedPlayer);
        DeckState active = state(
            card(Card.Type.REFACTOR, 1),
            card(Card.Type.METHOD, 2),
            card(Card.Type.BITCOIN, 3)
        );
        replaceDecks(engine, active);
        playCard(active, card(Card.Type.REFACTOR, 1));

        resolver.resolve(engine, 0, active, card(Card.Type.REFACTOR, 1));

        assertTrue(containsDecisionType(scriptedPlayer.promptOptions().get(0), TrashCardDecision.class));
        assertFalse(containsDecisionType(scriptedPlayer.promptOptions().get(0), EndPhaseDecision.class));
        assertTrue(containsDecisionType(scriptedPlayer.promptOptions().get(1), GainCardDecision.class));
        assertFalse(containsDecisionType(scriptedPlayer.promptOptions().get(1), EndPhaseDecision.class));
        assertFalse(active.allCards().contains(card(Card.Type.METHOD, 2)));
        assertTrue(containsType(active.discardPile, Card.Type.MONITORING));
    }

    @Test
    public void refactor_gainPromptDoesNotAllowSkipping() throws PlayerViolationException {
        ScriptedPlayer scriptedPlayer = player("P1",
            new TrashCardDecision(card(Card.Type.METHOD, 2)),
            new GainCardDecision(Card.Type.MONITORING));
        GameEngine engine = engine(scriptedPlayer);
        DeckState active = state(
            card(Card.Type.REFACTOR, 1),
            card(Card.Type.METHOD, 2),
            card(Card.Type.BITCOIN, 3)
        );
        replaceDecks(engine, active);
        playCard(active, card(Card.Type.REFACTOR, 1));

        resolver.resolve(engine, 0, active, card(Card.Type.REFACTOR, 1));

        assertFalse(active.allCards().contains(card(Card.Type.METHOD, 2)));
        assertFalse(containsDecisionType(scriptedPlayer.promptOptions().get(1), EndPhaseDecision.class));
        assertEquals(9, engine.supplyCounts().get(Card.Type.MONITORING).intValue());
    }

    @Test
    public void refactor_trashesCardAndGainsEligibleCard() throws PlayerViolationException {
        GameEngine engine = engine(player("P1",
            new TrashCardDecision(card(Card.Type.METHOD, 2)),
            new GainCardDecision(Card.Type.MONITORING)));
        DeckState active = state(
            card(Card.Type.REFACTOR, 1),
            card(Card.Type.METHOD, 2),
            card(Card.Type.BITCOIN, 3)
        );
        replaceDecks(engine, active);
        playCard(active, card(Card.Type.REFACTOR, 1));

        resolver.resolve(engine, 0, active, card(Card.Type.REFACTOR, 1));

        assertFalse(active.allCards().contains(card(Card.Type.METHOD, 2)));
        assertTrue(containsType(active.discardPile, Card.Type.MONITORING));
        assertEquals(9, engine.supplyCounts().get(Card.Type.MONITORING).intValue());
    }

    @Test
    public void refactor_filtersGainChoicesByTrashCostPlusTwo() throws PlayerViolationException {
        ScriptedPlayer scriptedPlayer = player("P1",
            new TrashCardDecision(card(Card.Type.METHOD, 2)),
            new GainCardDecision(Card.Type.MONITORING));
        GameEngine engine = engine(scriptedPlayer);
        DeckState active = state(
            card(Card.Type.REFACTOR, 1),
            card(Card.Type.METHOD, 2),
            card(Card.Type.BITCOIN, 3)
        );
        replaceDecks(engine, active);
        playCard(active, card(Card.Type.REFACTOR, 1));

        resolver.resolve(engine, 0, active, card(Card.Type.REFACTOR, 1));

        List<Decision> gainOptions = scriptedPlayer.promptOptions().get(1);
        assertTrue(gainOptions.stream().allMatch(decision ->
            decision instanceof GainCardDecision && ((GainCardDecision) decision).cardType().cost() <= 4));
        assertFalse(containsDecisionType(gainOptions, EndPhaseDecision.class));
        assertFalse(gainOptions.stream().anyMatch(decision ->
            decision instanceof GainCardDecision && ((GainCardDecision) decision).cardType() == Card.Type.DOGECOIN));
    }

    @Test
    public void parallelization_playsChosenActionTwice() throws PlayerViolationException {
        GameEngine engine = engine(player("P1", new PlayCardDecision(card(Card.Type.IPO, 2))));
        DeckState active = state(card(Card.Type.PARALLELIZATION, 1), card(Card.Type.IPO, 2), card(Card.Type.BITCOIN, 3));
        seedDrawPile(active,
            card(Card.Type.BITCOIN, 4), card(Card.Type.BITCOIN, 5), card(Card.Type.BITCOIN, 6), card(Card.Type.BITCOIN, 7)
        );
        replaceDecks(engine, active);
        playCard(active, card(Card.Type.PARALLELIZATION, 1));

        resolver.resolve(engine, 0, active, card(Card.Type.PARALLELIZATION, 1));

        assertTrue(active.playedHand.contains(card(Card.Type.PARALLELIZATION, 1)));
        assertTrue(active.playedHand.contains(card(Card.Type.IPO, 2)));
        assertEquals(2, active.actions);
        assertEquals(4, active.money);
        assertEquals(5, active.unplayedHand.size());
    }

    @Test
    public void codeReview_drawsOneAndAddsTwoActions() throws PlayerViolationException {
        GameEngine engine = engine(player("P1"));
        DeckState active = state(card(Card.Type.CODE_REVIEW, 1));
        seedDrawPile(active, card(Card.Type.BITCOIN, 2));
        replaceDecks(engine, active);
        playCard(active, card(Card.Type.CODE_REVIEW, 1));

        resolver.resolve(engine, 0, active, card(Card.Type.CODE_REVIEW, 1));

        assertEquals(2, active.actions);
        assertEquals(1, active.unplayedHand.size());
    }

    @Test
    public void evergreenTest_givesBugToUnprotectedPlayers() throws PlayerViolationException {
        GameEngine engine = engine(player("P1"), player("P2"),
            player("P3", new PlayCardDecision(card(Card.Type.MONITORING, 3))));
        DeckState active = state(card(Card.Type.EVERGREEN_TEST, 1));
        DeckState victim = state(card(Card.Type.BITCOIN, 2));
        DeckState protectedPlayer = state(card(Card.Type.MONITORING, 3));
        seedDrawPile(active, card(Card.Type.BITCOIN, 4), card(Card.Type.ETHEREUM, 5));
        replaceDecks(engine, active, victim, protectedPlayer);
        playCard(active, card(Card.Type.EVERGREEN_TEST, 1));

        resolver.resolve(engine, 0, active, card(Card.Type.EVERGREEN_TEST, 1));

        assertEquals(2, active.unplayedHand.size());
        assertTrue(containsType(victim.discardPile, Card.Type.BUG));
        assertFalse(containsType(protectedPlayer.discardPile, Card.Type.BUG));
    }

    @Test
    public void sprintPlanning_drawsOneAndAddsActionAndBuy() throws PlayerViolationException {
        GameEngine engine = engine(player("P1"));
        DeckState active = state(card(Card.Type.SPRINT_PLANNING, 1));
        seedDrawPile(active, card(Card.Type.BITCOIN, 2));
        replaceDecks(engine, active);
        playCard(active, card(Card.Type.SPRINT_PLANNING, 1));

        resolver.resolve(engine, 0, active, card(Card.Type.SPRINT_PLANNING, 1));

        assertEquals(1, active.actions);
        assertEquals(1, active.buys);
        assertEquals(1, active.unplayedHand.size());
    }

    @Test
    public void mergeConflict_trashesCardAndDrawsByCost() throws PlayerViolationException {
        GameEngine engine = engine(player("P1", new TrashCardDecision(card(Card.Type.ETHEREUM, 2))));
        DeckState active = state(card(Card.Type.MERGE_CONFLICT, 1), card(Card.Type.ETHEREUM, 2));
        seedDrawPile(active,
            card(Card.Type.BITCOIN, 3), card(Card.Type.BITCOIN, 4), card(Card.Type.BITCOIN, 5)
        );
        replaceDecks(engine, active);
        playCard(active, card(Card.Type.MERGE_CONFLICT, 1));

        resolver.resolve(engine, 0, active, card(Card.Type.MERGE_CONFLICT, 1));

        assertFalse(active.allCards().contains(card(Card.Type.ETHEREUM, 2)));
        assertEquals(3, active.unplayedHand.size());
        assertTrue(active.unplayedHand.contains(card(Card.Type.BITCOIN, 3)));
        assertTrue(active.unplayedHand.contains(card(Card.Type.BITCOIN, 4)));
        assertTrue(active.unplayedHand.contains(card(Card.Type.BITCOIN, 5)));
    }

    @Test
    public void mergeConflict_drawCountMatchesTrashedCardCost() throws PlayerViolationException {
        GameEngine engine = engine(player("P1", new TrashCardDecision(card(Card.Type.METHOD, 2))));
        DeckState active = state(card(Card.Type.MERGE_CONFLICT, 1), card(Card.Type.METHOD, 2));
        seedDrawPile(active, card(Card.Type.BITCOIN, 3), card(Card.Type.ETHEREUM, 4));
        replaceDecks(engine, active);
        playCard(active, card(Card.Type.MERGE_CONFLICT, 1));

        resolver.resolve(engine, 0, active, card(Card.Type.MERGE_CONFLICT, 1));

        assertFalse(active.allCards().contains(card(Card.Type.METHOD, 2)));
        assertEquals(2, active.unplayedHand.size());
        assertTrue(active.unplayedHand.contains(card(Card.Type.BITCOIN, 3)));
        assertTrue(active.unplayedHand.contains(card(Card.Type.ETHEREUM, 4)));
    }

    @Test
    public void mergeConflict_forcedTrashDoesNotOfferEndPhase() throws PlayerViolationException {
        ScriptedPlayer scriptedPlayer = player("P1", new TrashCardDecision(card(Card.Type.ETHEREUM, 2)));
        GameEngine engine = engine(scriptedPlayer);
        DeckState active = state(card(Card.Type.MERGE_CONFLICT, 1), card(Card.Type.ETHEREUM, 2));
        seedDrawPile(active, card(Card.Type.BITCOIN, 3), card(Card.Type.BITCOIN, 4));
        replaceDecks(engine, active);
        playCard(active, card(Card.Type.MERGE_CONFLICT, 1));

        resolver.resolve(engine, 0, active, card(Card.Type.MERGE_CONFLICT, 1));

        assertTrue(containsDecisionType(scriptedPlayer.promptOptions().get(0), TrashCardDecision.class));
        assertFalse(containsDecisionType(scriptedPlayer.promptOptions().get(0), EndPhaseDecision.class));
        assertEquals(2, active.unplayedHand.size());
    }

    @Test
    public void parallelization_offersOtherUnplayedActionCardsAndAllowsSkipping()
        throws PlayerViolationException {
        ScriptedPlayer scriptedPlayer = player("P1", new EndPhaseDecision(GameState.TurnPhase.ACTION));
        GameEngine engine = engine(scriptedPlayer);
        DeckState active = state(
            card(Card.Type.PARALLELIZATION, 1),
            card(Card.Type.IPO, 2),
            card(Card.Type.BITCOIN, 3),
            card(Card.Type.FRAMEWORK, 4)
        );
        seedDrawPile(active,
            card(Card.Type.BITCOIN, 5), card(Card.Type.BITCOIN, 6), card(Card.Type.BITCOIN, 7), card(Card.Type.BITCOIN, 8)
        );
        replaceDecks(engine, active);
        playCard(active, card(Card.Type.PARALLELIZATION, 1));

        resolver.resolve(engine, 0, active, card(Card.Type.PARALLELIZATION, 1));

        List<Decision> options = scriptedPlayer.promptOptions().get(0);
        assertEquals(2, options.size());
        assertTrue(containsDecisionType(options, PlayCardDecision.class));
        assertTrue(containsDecisionType(options, EndPhaseDecision.class));
        assertEquals(0, active.actions);
        assertFalse(active.playedHand.contains(card(Card.Type.IPO, 2)));
    }

    @Test
    public void parallelization_withNoOtherActionCardsDoesNotPromptForChoice() throws PlayerViolationException {
        ScriptedPlayer scriptedPlayer = player("P1");
        GameEngine engine = engine(scriptedPlayer);
        DeckState active = state(
            card(Card.Type.PARALLELIZATION, 1),
            card(Card.Type.BITCOIN, 2),
            card(Card.Type.FRAMEWORK, 3)
        );
        replaceDecks(engine, active);
        playCard(active, card(Card.Type.PARALLELIZATION, 1));

        resolver.resolve(engine, 0, active, card(Card.Type.PARALLELIZATION, 1));

        assertTrue(scriptedPlayer.promptOptions().isEmpty());
    }

    @Test
    public void unitTest_offersExactlyThreeChoices() throws PlayerViolationException {
        ScriptedPlayer scriptedPlayer = player("P1",
            new ChooseEffectDecision(ChooseEffectDecision.Effect.UNIT_TEST_PLUS_TWO_MONEY));
        GameEngine engine = engine(scriptedPlayer);
        DeckState active = state(card(Card.Type.UNIT_TEST, 1));
        replaceDecks(engine, active);

        resolver.resolve(engine, 0, active, card(Card.Type.UNIT_TEST, 1));

        List<Decision> options = scriptedPlayer.promptOptions().get(0);
        assertEquals(3, options.size());
        for (Decision option : options) {
            assertTrue(option instanceof ChooseEffectDecision);
        }
    }

    @Test
    public void unitTest_plusTwoActionsBranchAddsExactlyTwoActions() throws PlayerViolationException {
        GameEngine engine = engine(player("P1",
            new ChooseEffectDecision(ChooseEffectDecision.Effect.UNIT_TEST_PLUS_TWO_ACTIONS)));
        DeckState active = state(card(Card.Type.UNIT_TEST, 1));
        replaceDecks(engine, active);
        playCard(active, card(Card.Type.UNIT_TEST, 1));

        resolver.resolve(engine, 0, active, card(Card.Type.UNIT_TEST, 1));

        assertEquals(2, active.actions);
        assertEquals(0, active.money);
        assertEquals(0, active.unplayedHand.size());
    }

    @Test
    public void unitTest_plusTwoCardsBranchDrawsExactlyTwoCards() throws PlayerViolationException {
        GameEngine engine = engine(player("P1",
            new ChooseEffectDecision(ChooseEffectDecision.Effect.UNIT_TEST_PLUS_TWO_CARDS)));
        DeckState active = state(card(Card.Type.UNIT_TEST, 1));
        seedDrawPile(active, card(Card.Type.BITCOIN, 2), card(Card.Type.ETHEREUM, 3));
        replaceDecks(engine, active);
        playCard(active, card(Card.Type.UNIT_TEST, 1));

        resolver.resolve(engine, 0, active, card(Card.Type.UNIT_TEST, 1));

        assertEquals(2, active.unplayedHand.size());
        assertTrue(active.unplayedHand.contains(card(Card.Type.BITCOIN, 2)));
        assertTrue(active.unplayedHand.contains(card(Card.Type.ETHEREUM, 3)));
    }

    @Test
    public void deploymentPipeline_reducesCardCostButNeverBelowZero() throws Exception {
        GameEngine engine = engine(player("P1"));
        DeckState active = state(card(Card.Type.DEPLOYMENT_PIPELINE, 1));
        replaceDecks(engine, active);
        playCard(active, card(Card.Type.DEPLOYMENT_PIPELINE, 1));

        resolver.resolve(engine, 0, active, card(Card.Type.DEPLOYMENT_PIPELINE, 1));

        assertEquals(1, active.turnCostReduction);
        assertEquals(2, effectiveCost(engine, Card.Type.CODE_REVIEW, active));

        active.turnCostReduction = 5;
        assertEquals(0, effectiveCost(engine, Card.Type.BACKLOG, active));
    }

    @Test
    public void deploymentPipeline_storesTurnDiscountThatEffectiveCostUsesLater() throws Exception {
        GameEngine engine = engine(player("P1"));
        DeckState active = state(card(Card.Type.DEPLOYMENT_PIPELINE, 1));
        replaceDecks(engine, active);
        playCard(active, card(Card.Type.DEPLOYMENT_PIPELINE, 1));

        int printedCost = Card.Type.CODE_REVIEW.cost();
        assertEquals(printedCost, effectiveCost(engine, Card.Type.CODE_REVIEW, active));

        resolver.resolve(engine, 0, active, card(Card.Type.DEPLOYMENT_PIPELINE, 1));

        assertEquals(1, active.turnCostReduction);
        assertEquals(printedCost, Card.Type.CODE_REVIEW.cost());
        assertEquals(printedCost - 1, effectiveCost(engine, Card.Type.CODE_REVIEW, active));
    }

    @Test
    public void hack_withMonitoringDoesNotPromptDiscardAndLogsAvoidance() throws PlayerViolationException {
        RecordingObserver observer = new RecordingObserver();
        ScriptedPlayer protectedPlayer = player("P2",
            new PlayCardDecision(card(Card.Type.MONITORING, 30)));
        GameEngine engine = engine(player("P1"), protectedPlayer);
        engine.setObserver(observer);
        DeckState active = state(card(Card.Type.HACK, 1));
        DeckState protectedState = state(
            card(Card.Type.MONITORING, 30),
            card(Card.Type.BITCOIN, 31),
            card(Card.Type.BITCOIN, 32)
        );
        replaceDecks(engine, active, protectedState);
        playCard(active, card(Card.Type.HACK, 1));

        resolver.resolve(engine, 0, active, card(Card.Type.HACK, 1));

        assertTrue(protectedState.unplayedHand.contains(card(Card.Type.MONITORING, 30)));
        assertEquals(1, protectedPlayer.promptOptions().size());
        assertTrue(observer.containsDescription("avoided HACK"));
    }

    @Test
    public void evergreenTest_withMonitoringLogsAvoidanceAndLeavesCardInHand() throws PlayerViolationException {
        RecordingObserver observer = new RecordingObserver();
        ScriptedPlayer protectedPlayer = player("P2",
            new PlayCardDecision(card(Card.Type.MONITORING, 30)));
        GameEngine engine = engine(player("P1"), protectedPlayer);
        engine.setObserver(observer);
        DeckState active = state(card(Card.Type.EVERGREEN_TEST, 1));
        DeckState protectedState = state(card(Card.Type.MONITORING, 30));
        replaceDecks(engine, active, protectedState);
        playCard(active, card(Card.Type.EVERGREEN_TEST, 1));

        resolver.resolve(engine, 0, active, card(Card.Type.EVERGREEN_TEST, 1));

        assertTrue(protectedState.unplayedHand.contains(card(Card.Type.MONITORING, 30)));
        assertEquals(1, protectedPlayer.promptOptions().size());
        assertTrue(observer.containsDescription("avoided EVERGREEN_TEST"));
    }

    @Test
    public void ransomwarePromptOffersExactlyTwoChoicesAndCanDiscardWithOneCard() throws PlayerViolationException {
        ScriptedPlayer victimPlayer = player("P2",
            new ChooseEffectDecision(ChooseEffectDecision.Effect.RANSOMWARE_DISCARD_TWO),
            new DiscardCardDecision(card(Card.Type.BITCOIN, 20)));
        GameEngine engine = engine(player("P1"), victimPlayer);
        DeckState active = state(card(Card.Type.RANSOMWARE, 1));
        DeckState victim = state(card(Card.Type.BITCOIN, 20));
        replaceDecks(engine, active, victim);
        playCard(active, card(Card.Type.RANSOMWARE, 1));

        resolver.resolve(engine, 0, active, card(Card.Type.RANSOMWARE, 1));

        List<Decision> options = victimPlayer.promptOptions().get(0);
        assertEquals(2, options.size());
        assertTrue(containsDecisionType(options, ChooseEffectDecision.class));
        assertFalse(containsDecisionType(options, EndPhaseDecision.class));
        assertEquals(1, victim.discardPile.size());
        assertEquals(0, victim.unplayedHand.size());
    }

    @Test
    public void ransomwareGainBugBranchAddsBugToDiscardPile() throws PlayerViolationException {
        GameEngine engine = engine(player("P1"), player("P2",
            new ChooseEffectDecision(ChooseEffectDecision.Effect.RANSOMWARE_GAIN_BUG)));
        DeckState active = state(card(Card.Type.RANSOMWARE, 1));
        DeckState victim = state(card(Card.Type.BITCOIN, 20), card(Card.Type.ETHEREUM, 21));
        replaceDecks(engine, active, victim);
        playCard(active, card(Card.Type.RANSOMWARE, 1));

        resolver.resolve(engine, 0, active, card(Card.Type.RANSOMWARE, 1));

        assertTrue(containsType(victim.discardPile, Card.Type.BUG));
        assertEquals(2, victim.unplayedHand.size());
    }

    @Test
    public void ransomwareDiscardBranchWorksEvenWithFewerThanTwoCards() throws PlayerViolationException {
        ScriptedPlayer victimPlayer = player("P2",
            new ChooseEffectDecision(ChooseEffectDecision.Effect.RANSOMWARE_DISCARD_TWO),
            new DiscardCardDecision(card(Card.Type.BITCOIN, 20)));
        GameEngine engine = engine(player("P1"), victimPlayer);
        DeckState active = state(card(Card.Type.RANSOMWARE, 1));
        DeckState victim = state(card(Card.Type.BITCOIN, 20));
        replaceDecks(engine, active, victim);
        playCard(active, card(Card.Type.RANSOMWARE, 1));

        resolver.resolve(engine, 0, active, card(Card.Type.RANSOMWARE, 1));

        assertEquals(0, victim.unplayedHand.size());
        assertEquals(1, victim.discardPile.size());
    }

    @Test
    public void ransomware_drawsCardsAndAppliesChosenReaction() throws PlayerViolationException {
        GameEngine engine = engine(
            player("P1"),
            player("P2",
                new ChooseEffectDecision(ChooseEffectDecision.Effect.RANSOMWARE_DISCARD_TWO),
                new DiscardCardDecision(card(Card.Type.BITCOIN, 20)),
                new DiscardCardDecision(card(Card.Type.ETHEREUM, 21))),
            player("P3", new PlayCardDecision(card(Card.Type.MONITORING, 30)))
        );
        DeckState active = state(card(Card.Type.RANSOMWARE, 1));
        DeckState victim = state(
            card(Card.Type.BITCOIN, 20),
            card(Card.Type.ETHEREUM, 21),
            card(Card.Type.DOGECOIN, 22),
            card(Card.Type.METHOD, 23)
        );
        DeckState protectedPlayer = state(card(Card.Type.MONITORING, 30), card(Card.Type.BITCOIN, 31));
        seedDrawPile(active, card(Card.Type.BITCOIN, 40), card(Card.Type.BITCOIN, 41), card(Card.Type.BITCOIN, 42));
        replaceDecks(engine, active, victim, protectedPlayer);
        playCard(active, card(Card.Type.RANSOMWARE, 1));

        resolver.resolve(engine, 0, active, card(Card.Type.RANSOMWARE, 1));

        assertEquals(3, active.unplayedHand.size());
        assertEquals(2, victim.unplayedHand.size());
        assertEquals(2, victim.discardPile.size());
        assertEquals(2, protectedPlayer.unplayedHand.size());
        assertTrue(protectedPlayer.discardPile.isEmpty());
    }

    @Test
    public void deploymentPipeline_addsBuyMoneyAndCostReduction() throws PlayerViolationException {
        GameEngine engine = engine(player("P1"));
        DeckState active = state(card(Card.Type.DEPLOYMENT_PIPELINE, 1));
        replaceDecks(engine, active);

        resolver.resolve(engine, 0, active, card(Card.Type.DEPLOYMENT_PIPELINE, 1));

        assertEquals(1, active.buys);
        assertEquals(1, active.money);
        assertEquals(1, active.turnCostReduction);
    }

    @Test
    public void unitTest_appliesChosenBonusEffect() throws PlayerViolationException {
        GameEngine engine = engine(player("P1",
            new ChooseEffectDecision(ChooseEffectDecision.Effect.UNIT_TEST_PLUS_TWO_MONEY)));
        DeckState active = state(card(Card.Type.UNIT_TEST, 1));
        replaceDecks(engine, active);

        resolver.resolve(engine, 0, active, card(Card.Type.UNIT_TEST, 1));

        assertEquals(2, active.money);
    }

    private static GameEngine engine(Player... players) {
        return new GameEngine(Arrays.asList(players), actionCards(), new Random(42));
    }

    private static void replaceDecks(GameEngine engine, DeckState... states) {
        engine.deckStates().clear();
        engine.deckStates().addAll(Arrays.asList(states));
    }

    private static DeckState state(Card... cards) {
        DeckState state = new DeckState();
        for (Card card : cards) {
            state.unplayedHand.add(card);
        }
        return state;
    }

    private static void seedDrawPile(DeckState state, Card... cards) {
        state.drawPile.addAll(Arrays.asList(cards));
    }

    private static void playCard(DeckState state, Card card) {
        if (!state.unplayedHand.remove(card)) {
            throw new IllegalStateException("Card not present in hand: " + card);
        }
        state.playedHand.add(card);
    }

    private static ScriptedPlayer player(String name, Decision... decisions) {
        return new ScriptedPlayer(name, decisions);
    }

    private static int effectiveCost(GameEngine engine, Card.Type type, DeckState state) throws Exception {
        Method method = GameEngine.class.getDeclaredMethod("effectiveCost", Card.Type.class, DeckState.class);
        method.setAccessible(true);
        return (Integer) method.invoke(engine, type, state);
    }

    private static List<Card.Type> actionCards() {
        return List.of(
            Card.Type.BACKLOG,
            Card.Type.DAILY_SCRUM,
            Card.Type.IPO,
            Card.Type.HACK,
            Card.Type.MONITORING,
            Card.Type.TECH_DEBT,
            Card.Type.REFACTOR,
            Card.Type.PARALLELIZATION,
            Card.Type.CODE_REVIEW,
            Card.Type.EVERGREEN_TEST
        );
    }

    private static Card card(Card.Type type, int id) {
        return new Card(type, id);
    }

    private static boolean containsType(Iterable<Card> cards, Card.Type type) {
        for (Card card : cards) {
            if (card.type() == type) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsDecisionType(Collection<Decision> decisions, Class<?> decisionType) {
        for (Decision decision : decisions) {
            if (decisionType.isInstance(decision)) {
                return true;
            }
        }
        return false;
    }

    private static final class RecordingObserver implements GameObserver {
        private final List<GameEvent> events = new ArrayList<>();

        @Override
        public void notifyEvent(GameState state, Event event) {
            if (event instanceof GameEvent gameEvent) {
                events.add(gameEvent);
            }
        }

        private boolean containsDescription(String fragment) {
            for (GameEvent event : events) {
                if (event.getDescription().contains(fragment)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static final class ScriptedPlayer implements Player {
        private final String name;
        private final Deque<Decision> decisions;
        private final List<ImmutableList<Decision>> promptOptions = new ArrayList<>();

        private ScriptedPlayer(String name, Decision... decisions) {
            this.name = name;
            this.decisions = new ArrayDeque<>(Arrays.asList(decisions));
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Decision makeDecision(GameState state, ImmutableList<Decision> options, Optional<Event> reason) {
            promptOptions.add(options);
            if (decisions.isEmpty()) {
                throw new IllegalStateException("No scripted decision left for " + name);
            }

            Decision chosen = decisions.removeFirst();
            if (!options.contains(chosen)) {
                throw new IllegalStateException("Scripted decision was not offered: " + chosen);
            }
            return chosen;
        }

        @Override
        public Optional<GameObserver> getObserver() {
            return Optional.empty();
        }

        private List<ImmutableList<Decision>> promptOptions() {
            return promptOptions;
        }
    }
}
