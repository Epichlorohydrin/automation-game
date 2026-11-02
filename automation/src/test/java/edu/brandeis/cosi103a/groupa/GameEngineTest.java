package edu.brandeis.cosi103a.groupa;

import com.google.common.collect.ImmutableList;
import edu.brandeis.cosi.atg.cards.Card;
import edu.brandeis.cosi.atg.decisions.BuyDecision;
import edu.brandeis.cosi.atg.decisions.Decision;
import edu.brandeis.cosi.atg.decisions.EndPhaseDecision;
import edu.brandeis.cosi.atg.decisions.PlayCardDecision;
import edu.brandeis.cosi.atg.engine.PlayerViolationException;
import edu.brandeis.cosi.atg.event.Event;
import edu.brandeis.cosi.atg.event.EndTurnEvent;
import edu.brandeis.cosi.atg.event.GameEvent;
import edu.brandeis.cosi.atg.event.GameEndEvent;
import edu.brandeis.cosi.atg.event.GameObserver;
import edu.brandeis.cosi.atg.event.GameStartEvent;
import edu.brandeis.cosi.atg.event.GainCardEvent;
import edu.brandeis.cosi.atg.event.PlayCardEvent;
import edu.brandeis.cosi.atg.player.Player;
import edu.brandeis.cosi.atg.state.GameResult;
import edu.brandeis.cosi.atg.state.GameState;
import edu.brandeis.cosi.atg.state.PlayerResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the ATG-compatible GameEngine.
 */
public class GameEngineTest {

    @Test
    public void testPlayReturnsOfficialGameResult() throws PlayerViolationException {
        GameEngine engine = new GameEngine(players(), actionCards(), new Random(42));

        GameResult result = engine.play();

        assertNotNull(result, "play should return a GameResult");
        assertEquals(2, result.playerResults().size(), "result should contain one entry per player");
    }

    @Test
    public void testResultsAreSortedByScoreDescending() throws PlayerViolationException {
        GameEngine engine = new GameEngine(players(), actionCards(), new Random(42));

        GameResult result = engine.play();

        assertTrue(result.playerResults().get(0).score() >= result.playerResults().get(1).score());
    }

    @Test
    public void testEndingDecksAreIncluded() throws PlayerViolationException {
        GameEngine engine = new GameEngine(players(), actionCards(), new Random(42));

        GameResult result = engine.play();

        for (PlayerResult playerResult : result.playerResults()) {
            assertFalse(playerResult.endingDeck().isEmpty(), "ending deck should not be empty");
        }
    }

    @Test
    public void testRejectsWrongNumberOfActionCards() {
        assertThrows(IllegalArgumentException.class,
            () -> new GameEngine(players(), List.of(Card.Type.CODE_REVIEW), new Random(42)));
    }

    @Test
    public void testRejectsNonActionCards() {
        assertThrows(IllegalArgumentException.class, () -> new GameEngine(players(), List.of(
            Card.Type.BITCOIN,
            Card.Type.DAILY_SCRUM,
            Card.Type.IPO,
            Card.Type.HACK,
            Card.Type.MONITORING,
            Card.Type.TECH_DEBT,
            Card.Type.REFACTOR,
            Card.Type.PARALLELIZATION,
            Card.Type.CODE_REVIEW,
            Card.Type.EVERGREEN_TEST
        ), new Random(42)));
    }

    @Test
    public void testCreateCardDecisionOptionsFiltersAndPreservesOrder() throws Exception {
        GameEngine engine = new GameEngine(players(), actionCards(), new Random(42));
        List<Card> cards = List.of(
            card(Card.Type.BITCOIN, 1),
            card(Card.Type.MONITORING, 2),
            card(Card.Type.ETHEREUM, 3)
        );

        Method method = GameEngine.class.getDeclaredMethod(
            "createCardDecisionOptions",
            Iterable.class,
            java.util.function.Predicate.class,
            java.util.function.Function.class
        );
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Decision> options = (List<Decision>) method.invoke(
            engine,
            cards,
            (java.util.function.Predicate<Card>) card -> card.category() == Card.Type.Category.MONEY,
            (java.util.function.Function<Card, Decision>) PlayCardDecision::new
        );

        assertEquals(2, options.size(), "helper should only include matching cards");
        assertTrue(options.get(0) instanceof PlayCardDecision, "first option should be a play decision");
        assertTrue(options.get(1) instanceof PlayCardDecision, "second option should be a play decision");
        assertEquals(card(Card.Type.BITCOIN, 1), ((PlayCardDecision) options.get(0)).card(),
            "helper should preserve input order for matching cards");
        assertEquals(card(Card.Type.ETHEREUM, 3), ((PlayCardDecision) options.get(1)).card(),
            "helper should preserve input order for matching cards");
    }

    @Test
    public void testHasMonitoringOnlyReturnsTrueInReactionPhase() {
        GameEngine engine = new GameEngine(players(), actionCards(), new Random(42));
        DeckState state = state(card(Card.Type.MONITORING, 1), card(Card.Type.BITCOIN, 2));

        assertTrue(engine.hasMonitoring(state), "default monitoring check should use reaction phase");
        assertTrue(engine.hasMonitoring(state, GameState.TurnPhase.REACTION),
            "monitoring should be available during reaction phase");
        assertFalse(engine.hasMonitoring(state, GameState.TurnPhase.ACTION),
            "monitoring should not be available during action phase");
        assertFalse(engine.hasMonitoring(state, GameState.TurnPhase.BUY),
            "monitoring should not be available during buy phase");
    }

    @Test
    public void testHasMonitoringReturnsFalseWhenCardMissing() {
        GameEngine engine = new GameEngine(players(), actionCards(), new Random(42));
        DeckState state = state(card(Card.Type.BITCOIN, 1), card(Card.Type.ETHEREUM, 2));

        assertFalse(engine.hasMonitoring(state), "should return false when monitoring is absent");
        assertFalse(engine.hasMonitoring(state, GameState.TurnPhase.REACTION),
            "should return false when monitoring is absent even in reaction phase");
    }

    /**
     * Verifies that an engine-level observer receives lifecycle boundary events.
     *
     * <p>The first emitted event should be {@link GameStartEvent} and the final
     * emitted event should be {@link GameEndEvent}.</p>
     */
    @Test
    public void testEngineObserverReceivesStartAndEndEvents() throws PlayerViolationException {
        GameEngine engine = new GameEngine(players(), actionCards(), new Random(42));
        RecordingObserver observer = new RecordingObserver();
        engine.setObserver(observer);

        engine.play();

        assertFalse(observer.events.isEmpty(), "observer should receive events");
        assertTrue(observer.events.get(0) instanceof GameStartEvent, "first event should be game start");
        assertTrue(observer.events.get(observer.events.size() - 1) instanceof GameEndEvent,
            "last event should be game end");
    }

    /**
     * Verifies that per-player observers are notified during gameplay.
     *
     * <p>Each player returns its own observer, and both should receive at least one event.</p>
     */
    @Test
    public void testPlayerObserversReceiveNotifications() throws PlayerViolationException {
        RecordingObserver p1Observer = new RecordingObserver();
        RecordingObserver p2Observer = new RecordingObserver();
        List<Player> players = List.of(
            new FirstOptionPlayer("P1", p1Observer),
            new FirstOptionPlayer("P2", p2Observer)
        );
        GameEngine engine = new GameEngine(players, actionCards(), new Random(42));

        engine.play();

        assertFalse(p1Observer.events.isEmpty(), "player 1 observer should receive events");
        assertFalse(p2Observer.events.isEmpty(), "player 2 observer should receive events");
    }

    /**
     * Verifies that the engine passes the previously emitted event as the decision reason.
     *
     * <p>For the first player prompt, the reason should be the initial {@link GameStartEvent}.</p>
     */
    @Test
    public void testDecisionReasonIncludesLastEvent() throws PlayerViolationException {
        ReasonCapturingPlayer p1 = new ReasonCapturingPlayer("P1");
        ReasonCapturingPlayer p2 = new ReasonCapturingPlayer("P2");
        GameEngine engine = new GameEngine(List.of(p1, p2), actionCards(), new Random(42));

        engine.play();

        assertNotNull(p1.firstReasonSeen, "player should capture first decision reason");
        assertNotNull(p2.firstReasonSeen, "player should capture first decision reason");
        assertTrue(p1.firstReasonSeen instanceof GameEvent,
            "first reason for first player should be turn-start event");
        assertTrue(p1.firstReasonSeen.getDescription() != null
                && p1.firstReasonSeen.getDescription().startsWith("Turn started for "),
            "first reason should carry turn-start description");
    }

    /**
     * Verifies observer exceptions are isolated and do not crash the game loop.
     */
    @Test
    public void testObserverExceptionsDoNotCrashGame() throws PlayerViolationException {
        GameEngine engine = new GameEngine(players(), actionCards(), new Random(42));
        engine.setObserver(new ThrowingObserver());

        GameResult result = engine.play();

        assertNotNull(result, "game should still complete even if observer throws");
    }

    /**
     * Verifies that core per-turn event categories are emitted during normal gameplay.
     */
    @Test
    public void testObserverReceivesPlayGainAndEndTurnEvents() throws PlayerViolationException {
        GameEngine engine = new GameEngine(players(), actionCards(), new Random(42));
        RecordingObserver observer = new RecordingObserver();
        engine.setObserver(observer);

        engine.play();

        assertTrue(observer.events.stream().anyMatch(e -> e instanceof PlayCardEvent),
            "observer should receive at least one PlayCardEvent");
        assertTrue(observer.events.stream().anyMatch(e -> e instanceof GainCardEvent),
            "observer should receive at least one GainCardEvent");
        assertTrue(observer.events.stream().anyMatch(e -> e instanceof EndTurnEvent),
            "observer should receive at least one EndTurnEvent");
    }

    /**
     * Verifies that a turn-start event is emitted at the beginning of each turn.
     */
    @Test
    public void testObserverReceivesTurnStartEvent() throws PlayerViolationException {
        GameEngine engine = new GameEngine(players(), actionCards(), new Random(42));
        RecordingObserver observer = new RecordingObserver();
        engine.setObserver(observer);

        engine.play();

        assertTrue(observer.events.stream().anyMatch(e -> e instanceof GameEvent
                && e.getDescription() != null
                && e.getDescription().startsWith("Turn started for ")),
            "observer should receive a turn-start GameEvent");
    }

    @Test
    public void testTurnResourcesResetAtStartOfTurn() throws Exception {
        CapturingScriptedPlayer player = new CapturingScriptedPlayer("P1",
            new EndPhaseDecision(GameState.TurnPhase.ACTION),
            new EndPhaseDecision(GameState.TurnPhase.MONEY),
            new EndPhaseDecision(GameState.TurnPhase.BUY));
        GameEngine engine = new GameEngine(List.of(player, new FirstOptionPlayer("P2")), actionCards(), new Random(42));
        DeckState active = state(card(Card.Type.BITCOIN, 1));
        active.actions = 5;
        active.buys = 4;
        active.money = 9;
        active.turnCostReduction = 3;
        replaceDecks(engine, active, state());

        invokeTakeTurn(engine, 0);

        GameState firstPrompt = player.promptStates.get(0);
        assertEquals(GameState.TurnPhase.ACTION, firstPrompt.phase(), "turn should start in action phase");
        assertEquals(1, firstPrompt.availableActions(), "unused actions must not carry over");
        assertEquals(1, firstPrompt.availableBuys(), "unused buys must not carry over");
        assertEquals(0, firstPrompt.spendableMoney(), "unused money must not carry over");
        assertEquals(Card.Type.CODE_REVIEW.cost(), effectiveCost(engine, Card.Type.CODE_REVIEW, active),
            "turn cost reduction must not carry over");
    }

    @Test
    public void testActionPhaseCanEndVoluntarilyWithActionsInHand() throws Exception {
        CapturingScriptedPlayer player = new CapturingScriptedPlayer("P1",
            new EndPhaseDecision(GameState.TurnPhase.ACTION));
        GameEngine engine = new GameEngine(List.of(player), actionCards(), new Random(42));
        DeckState active = state(card(Card.Type.CODE_REVIEW, 1));
        active.actions = 1;
        replaceDecks(engine, active);

        invokePlayActionPhase(engine, 0, active);

        ImmutableList<Decision> offered = player.promptOptions.get(0);
        assertTrue(containsDecisionType(offered, PlayCardDecision.class),
            "action card should be offered while action count is available");
        assertTrue(containsDecisionType(offered, EndPhaseDecision.class),
            "player should be allowed to end the action phase voluntarily");
        assertTrue(active.unplayedHand.contains(card(Card.Type.CODE_REVIEW, 1)),
            "ending the action phase should leave the action card unplayed");
    }

    @Test
    public void testNoActionCountPreventsActionPrompt() throws Exception {
        CapturingScriptedPlayer player = new CapturingScriptedPlayer("P1");
        GameEngine engine = new GameEngine(List.of(player), actionCards(), new Random(42));
        DeckState active = state(card(Card.Type.CODE_REVIEW, 1));
        active.actions = 0;
        replaceDecks(engine, active);

        invokePlayActionPhase(engine, 0, active);

        assertTrue(player.promptOptions.isEmpty(), "engine should not prompt to play actions when no actions remain");
        assertTrue(active.unplayedHand.contains(card(Card.Type.CODE_REVIEW, 1)),
            "action should remain unplayed when no actions are available");
    }

    @Test
    public void testNonPlusActionStopsFurtherActionPrompts() throws Exception {
        CapturingScriptedPlayer player = new CapturingScriptedPlayer("P1",
            new PlayCardDecision(card(Card.Type.MONITORING, 1)),
            new EndPhaseDecision(GameState.TurnPhase.MONEY),
            new EndPhaseDecision(GameState.TurnPhase.BUY));
        GameEngine engine = new GameEngine(List.of(player, new FirstOptionPlayer("P2")), actionCards(), new Random(42));
        DeckState active = state(card(Card.Type.MONITORING, 1), card(Card.Type.CODE_REVIEW, 2));
        active.drawPile.addAll(List.of(
            card(Card.Type.BITCOIN, 3),
            card(Card.Type.BITCOIN, 4),
            card(Card.Type.BITCOIN, 5),
            card(Card.Type.BITCOIN, 6),
            card(Card.Type.BITCOIN, 7),
            card(Card.Type.BITCOIN, 8),
            card(Card.Type.BITCOIN, 9)
        ));
        active.actions = 7;
        replaceDecks(engine, active, state());

        invokeTakeTurn(engine, 0);

        assertEquals(GameState.TurnPhase.ACTION, player.promptStates.get(0).phase(),
            "first prompt should be the action play");
        assertEquals(GameState.TurnPhase.MONEY, player.promptStates.get(1).phase(),
            "after a non +Action card consumes the only action, engine should advance to money");
        assertEquals(1, player.promptStates.stream()
            .filter(state -> state.phase() == GameState.TurnPhase.ACTION)
            .count(), "remaining action cards should not trigger another action prompt with zero actions");
        assertTrue(active.discardPile.contains(card(Card.Type.CODE_REVIEW, 2)),
            "unplayed action should be cleaned up without being played");
    }

    @Test
    public void testPlusBuyAllowsTwoPurchasesInOneBuyPhase() throws Exception {
        CapturingScriptedPlayer player = new CapturingScriptedPlayer("P1",
            new BuyDecision(Card.Type.ETHEREUM),
            new BuyDecision(Card.Type.METHOD));
        GameEngine engine = new GameEngine(List.of(player), actionCards(), new Random(42));
        DeckState active = state();
        active.buys = 1;
        active.money = 4;
        active.playedHand.add(card(Card.Type.DEPLOYMENT_PIPELINE, 1));
        replaceDecks(engine, active);

        new ActionCardResolver().resolve(engine, 0, active, card(Card.Type.DEPLOYMENT_PIPELINE, 1));
        int ethereumBefore = engine.supplyCounts().get(Card.Type.ETHEREUM);
        int methodBefore = engine.supplyCounts().get(Card.Type.METHOD);

        invokePlayBuyPhase(engine, 0, active);

        assertEquals(ethereumBefore - 1, engine.supplyCounts().get(Card.Type.ETHEREUM));
        assertEquals(methodBefore - 1, engine.supplyCounts().get(Card.Type.METHOD));
        assertTrue(containsType(active.discardPile, Card.Type.ETHEREUM),
            "first purchased card should be gained to discard pile");
        assertTrue(containsType(active.discardPile, Card.Type.METHOD),
            "second purchased card should be gained to discard pile");
        assertEquals(0, active.buys, "both buys should be consumed");
    }

    @Test
    public void testBoughtCardGoesToDiscardPile() throws Exception {
        CapturingScriptedPlayer player = new CapturingScriptedPlayer("P1",
            new BuyDecision(Card.Type.ETHEREUM));
        GameEngine engine = new GameEngine(List.of(player), actionCards(), new Random(42));
        DeckState active = state();
        active.buys = 1;
        active.money = Card.Type.ETHEREUM.cost();
        replaceDecks(engine, active);

        invokePlayBuyPhase(engine, 0, active);

        assertTrue(containsType(active.discardPile, Card.Type.ETHEREUM),
            "bought card should be placed in discard pile");
        assertFalse(containsType(active.unplayedHand, Card.Type.ETHEREUM),
            "bought card should not go directly to hand");
        assertFalse(containsType(active.playedHand, Card.Type.ETHEREUM),
            "bought card should not go to played cards");
        assertFalse(containsType(active.drawPile, Card.Type.ETHEREUM),
            "bought card should not go directly to draw pile");
    }

    @Test
    public void testGameEndsWhenFrameworkPileIsEmpty() throws PlayerViolationException {
        CapturingScriptedPlayer player = new CapturingScriptedPlayer("P1",
            new EndPhaseDecision(GameState.TurnPhase.ACTION),
            new PlayCardDecision(card(Card.Type.DOGECOIN, 1)),
            new PlayCardDecision(card(Card.Type.DOGECOIN, 2)),
            new PlayCardDecision(card(Card.Type.DOGECOIN, 3)),
            new EndPhaseDecision(GameState.TurnPhase.MONEY),
            new BuyDecision(Card.Type.FRAMEWORK));
        GameEngine engine = new GameEngine(List.of(player, new FirstOptionPlayer("P2")), actionCards(), new Random(42));
        DeckState active = state(
            card(Card.Type.DOGECOIN, 1),
            card(Card.Type.DOGECOIN, 2),
            card(Card.Type.DOGECOIN, 3)
        );
        replaceDecks(engine, active, state());
        engine.supplyCounts().put(Card.Type.FRAMEWORK, 1);
        RecordingObserver observer = new RecordingObserver();
        engine.setObserver(observer);

        GameResult result = engine.play();

        assertNotNull(result, "game should return results when final Framework is purchased");
        assertEquals(0, engine.supplyCounts().get(Card.Type.FRAMEWORK).intValue(),
            "Framework pile should be empty");
        assertFalse(observer.events.isEmpty(), "observer should receive game events");
        assertTrue(observer.events.get(observer.events.size() - 1) instanceof GameEndEvent,
            "game should end when Framework pile becomes empty");
    }

    private static List<Player> players() {
        return List.of(new FirstOptionPlayer("P1"), new FirstOptionPlayer("P2"));
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

    private static DeckState state(Card... cards) {
        DeckState state = new DeckState();
        state.unplayedHand.addAll(List.of(cards));
        return state;
    }

    private static Card card(Card.Type type, int id) {
        return new Card(type, id);
    }

    private static void replaceDecks(GameEngine engine, DeckState... states) {
        engine.deckStates().clear();
        engine.deckStates().addAll(List.of(states));
    }

    private static boolean containsType(Iterable<Card> cards, Card.Type type) {
        for (Card card : cards) {
            if (card.type() == type) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsDecisionType(Iterable<Decision> decisions, Class<?> decisionType) {
        for (Decision decision : decisions) {
            if (decisionType.isInstance(decision)) {
                return true;
            }
        }
        return false;
    }

    private static int effectiveCost(GameEngine engine, Card.Type type, DeckState state) throws Exception {
        Method method = GameEngine.class.getDeclaredMethod("effectiveCost", Card.Type.class, DeckState.class);
        method.setAccessible(true);
        return (Integer) method.invoke(engine, type, state);
    }

    private static void invokeTakeTurn(GameEngine engine, int playerIndex) throws Exception {
        Method method = GameEngine.class.getDeclaredMethod("takeTurn", int.class);
        method.setAccessible(true);
        method.invoke(engine, playerIndex);
    }

    private static void invokePlayActionPhase(GameEngine engine, int playerIndex, DeckState state) throws Exception {
        Method method = GameEngine.class.getDeclaredMethod("playActionPhase", int.class, DeckState.class);
        method.setAccessible(true);
        method.invoke(engine, playerIndex, state);
    }

    private static void invokePlayBuyPhase(GameEngine engine, int playerIndex, DeckState state) throws Exception {
        Method method = GameEngine.class.getDeclaredMethod("playBuyPhase", int.class, DeckState.class);
        method.setAccessible(true);
        method.invoke(engine, playerIndex, state);
    }

    private static final class CapturingScriptedPlayer implements Player {
        private final String name;
        private final List<Decision> decisions;
        private final List<GameState> promptStates = new ArrayList<>();
        private final List<ImmutableList<Decision>> promptOptions = new ArrayList<>();

        private CapturingScriptedPlayer(String name, Decision... decisions) {
            this.name = name;
            this.decisions = new ArrayList<>(List.of(decisions));
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Decision makeDecision(GameState state, ImmutableList<Decision> options, Optional<Event> reason) {
            promptStates.add(state);
            promptOptions.add(options);
            if (decisions.isEmpty()) {
                throw new IllegalStateException("No scripted decision left for " + name);
            }
            Decision chosen = decisions.remove(0);
            if (!options.contains(chosen)) {
                throw new IllegalStateException("Scripted decision was not offered: " + chosen);
            }
            return chosen;
        }

        @Override
        public Optional<GameObserver> getObserver() {
            return Optional.empty();
        }
    }

    private static final class FirstOptionPlayer implements Player {
        private final String name;
        private final Optional<GameObserver> observer;

        private FirstOptionPlayer(String name) {
            this(name, null);
        }

        private FirstOptionPlayer(String name, GameObserver observer) {
            this.name = name;
            this.observer = Optional.ofNullable(observer);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Decision makeDecision(GameState state, ImmutableList<Decision> options, Optional<Event> reason) {
            return options.get(0);
        }

        @Override
        public Optional<GameObserver> getObserver() {
            return observer;
        }
    }

    private static final class ReasonCapturingPlayer implements Player {
        private final String name;
        private Event firstReasonSeen;

        private ReasonCapturingPlayer(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Decision makeDecision(GameState state, ImmutableList<Decision> options, Optional<Event> reason) {
            if (firstReasonSeen == null && reason.isPresent()) {
                firstReasonSeen = reason.get();
            }
            return options.get(0);
        }

        @Override
        public Optional<GameObserver> getObserver() {
            return Optional.empty();
        }
    }

    private static final class RecordingObserver implements GameObserver {
        private final List<Event> events = new ArrayList<>();

        @Override
        public void notifyEvent(GameState state, Event event) {
            events.add(event);
        }
    }

    private static final class ThrowingObserver implements GameObserver {
        @Override
        public void notifyEvent(GameState state, Event event) {
            throw new RuntimeException("observer failure");
        }
    }
}
