package edu.brandeis.cosi103a.groupa;

import edu.brandeis.cosi.atg.cards.Card;
import edu.brandeis.cosi.atg.decisions.ChooseEffectDecision;
import edu.brandeis.cosi.atg.decisions.Decision;
import edu.brandeis.cosi.atg.decisions.DiscardCardDecision;
import edu.brandeis.cosi.atg.decisions.EndPhaseDecision;
import edu.brandeis.cosi.atg.decisions.GainCardDecision;
import edu.brandeis.cosi.atg.decisions.PlayCardDecision;
import edu.brandeis.cosi.atg.engine.PlayerViolationException;
import edu.brandeis.cosi.atg.state.GameState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Resolves action card effects for {@link GameEngine}.
 */
final class ActionCardResolver {

    /**
     * Resolves an action card effect based on its card type.
     *
     * @param engine engine-provided state and helper operations
     * @param playerIndex zero-based player index
     * @param state mutable deck state for the active player
     * @param card action card being resolved
     * @throws PlayerViolationException if a player makes an invalid decision
     */
    void resolve(GameEngine engine, int playerIndex, DeckState state, Card card)
        throws PlayerViolationException {
        switch (card.type()) {
            case BACKLOG -> resolveBacklog(engine, playerIndex, state);
            case DAILY_SCRUM -> resolveDailyScrum(engine, playerIndex, state);
            case IPO -> resolveIpo(engine, state);
            case HACK -> resolveHack(engine, playerIndex, state);
            case MONITORING -> resolveMonitoring(engine, state);
            case TECH_DEBT -> resolveTechDebt(engine, playerIndex, state);
            case REFACTOR -> resolveRefactor(engine, playerIndex, state);
            case PARALLELIZATION -> resolveParallelization(engine, playerIndex, state, card);
            case CODE_REVIEW -> resolveCodeReview(engine, state);
            case EVERGREEN_TEST -> resolveEvergreenTest(engine, playerIndex, state);
            case SPRINT_PLANNING -> resolveSprintPlanning(engine, state);
            case MERGE_CONFLICT -> resolveMergeConflict(engine, playerIndex, state);
            case RANSOMWARE -> resolveRansomware(engine, playerIndex, state);
            case DEPLOYMENT_PIPELINE -> resolveDeploymentPipeline(state);
            case UNIT_TEST -> resolveUnitTest(engine, playerIndex, state);
            default -> {
                // No additional effect for unsupported action types.
            }
        }
    }

    /**
     * Resolves Daily Scrum by drawing cards and giving other players a card.
     */
    private void resolveDailyScrum(GameEngine engine, int playerIndex, DeckState state) {
        engine.drawCards(state, 4);
        state.buys++;
        for (int i = 0; i < engine.deckStates().size(); i++) {
            if (i != playerIndex) {
                engine.drawCards(engine.deckStates().get(i), 1);
            }
        }
    }

    /**
     * Resolves IPO by drawing cards and increasing actions and money.
     */
    private void resolveIpo(GameEngine engine, DeckState state) {
        state.money += 2;
        engine.drawCards(state, 2);
        state.actions++;
        
    }

    /**
     * Resolves Hack.
     *
     * <p>The active player gets +$2 for the turn. Each other player is then attacked in turn.
     * A player with {@link Card.Type#MONITORING} in {@code unplayedHand} is first prompted in
     * {@link GameState.TurnPhase#REACTION} to reveal it. Revealing Monitoring avoids the attack,
     * logs a {@code GameEvent}, and leaves the Monitoring card in hand instead of moving it to
     * {@code playedHand}. If the target does not reveal Monitoring, the engine forces discard
     * decisions until that player's hand is reduced to 3 cards.</p >
     */
    private void resolveHack(GameEngine engine, int playerIndex, DeckState state)
        throws PlayerViolationException {
        state.money += 2;
        for (int i = 0; i < engine.deckStates().size(); i++) {
            if (i == playerIndex) {
                continue;
            }

            DeckState target = engine.deckStates().get(i);
            if (revealMonitoringIfChosen(engine, i, target, "HACK")) {
                continue;
            }

            engine.forceDiscardDownTo(i, 3);
        }
    }

    /**
     * Resolves Monitoring when it is played as a normal action card.
     *
     * <p>This played effect is simply +2 Cards. The reaction behavior for Monitoring is handled
     * separately by {@link #revealMonitoringIfChosen(GameEngine, int, DeckState, String)} when an
     * attack card is resolving.</p >
     */
    private void resolveMonitoring(GameEngine engine, DeckState state) {
        engine.drawCards(state, 2);
    }

    /**
     * Resolves Tech Debt.
     *
     * <p>The active player gets +1 Card, +1 Action, and +$1. After those immediate gains, check
     * the player's own card source, not the public buyable supply. In the repository's code,
     * {@code drawPile} is the pile the player draws from and {@code discardPile} is the pile that
     * gets shuffled back into {@code drawPile} when more cards are needed. Because that reshuffle
     * happens before the player's source is considered empty, Tech Debt forces a discard only when
     * both {@code drawPile} and {@code discardPile} are empty after the +1 Card is resolved. That
     * discard is mandatory and therefore uses the engine's forced discard helper, which does not
     * offer an {@code EndPhaseDecision}.</p >
     */
    private void resolveTechDebt(GameEngine engine, int playerIndex, DeckState state)
        throws PlayerViolationException {
        engine.drawCards(state, 1);
        state.actions++;
        state.money++;

        int emptySupplyPiles = engine.countEmptyPiles();
        if (emptySupplyPiles > 0) {
            engine.discardUpTo(playerIndex, emptySupplyPiles);
        }
    }

    /**
     * Resolves Code Review.
     *
     * <p>The active player immediately draws 1 card into {@code unplayedHand} and gains
     * 2 additional actions for the current turn.</p >
     */
    private void resolveCodeReview(GameEngine engine, DeckState state) {
        engine.drawCards(state, 1);
        state.actions += 2;
    }

    /**
     * Resolves Evergreen Test.
     *
     * <p>The active player gets +2 Cards. Each other player is then attacked. A target who has a
     * {@link Card.Type#MONITORING} in {@code unplayedHand} is prompted in the reaction phase to
     * reveal it. Revealing Monitoring avoids the Bug gain, logs a {@code GameEvent}, and keeps
     * Monitoring in hand. If the target does not reveal Monitoring, the engine gains a
     * {@link Card.Type#BUG} to that player's {@code discardPile}.</p >
     */
    private void resolveEvergreenTest(GameEngine engine, int playerIndex, DeckState state)
        throws PlayerViolationException {
        engine.drawCards(state, 2);
        for (int i = 0; i < engine.deckStates().size(); i++) {
            if (i == playerIndex) {
                continue;
            }

            DeckState target = engine.deckStates().get(i);
            if (revealMonitoringIfChosen(engine, i, target, "EVERGREEN_TEST")) {
                continue;
            }

            engine.gainBug(target);
        }
    }

    /**
     * Resolves Sprint Planning.
     *
     * <p>The active player draws 1 card, gains 1 additional action for the turn, and gains
     * 1 additional buy for the turn.</p >
     */
    private void resolveSprintPlanning(GameEngine engine, DeckState state) {
        engine.drawCards(state, 1);
        state.actions++;
        state.buys++;
    }

    /**
     * Resolves Deployment Pipeline by improving the current turn economy.
     */
    private void resolveDeploymentPipeline(DeckState state) {
        state.buys++;
        state.money++;
        state.turnCostReduction++;
    }

    /**
     * Resolves Backlog by discarding any number of cards and drawing the same amount.
     */
    private void resolveBacklog(GameEngine engine, int playerIndex, DeckState state)
        throws PlayerViolationException {
        state.actions++;
        List<Card> selectedDiscards = new ArrayList<>();
        List<Card> availableHand = new ArrayList<>(state.unplayedHand);
        while (true) {
            List<Decision> options = new ArrayList<>();
            for (Card handCard : availableHand) {
                options.add(new DiscardCardDecision(handCard));
            }
            options.add(new EndPhaseDecision(GameState.TurnPhase.DISCARD));

            Decision choice = engine.promptPlayer(playerIndex, GameState.TurnPhase.DISCARD, options);
            if (choice instanceof EndPhaseDecision) {
                break;
            }

            DiscardCardDecision discard = (DiscardCardDecision) choice;
            if (availableHand.remove(discard.card())) {
                selectedDiscards.add(discard.card());
            }
        }
        for (Card card : selectedDiscards) {
            if (state.unplayedHand.remove(card)) {
                state.discardPile.add(card);
            }
        }
        engine.drawCards(state, selectedDiscards.size());
    }
/**
     * Resolves Refactor.
     *
     * <p>The active player must first trash exactly one card from {@code unplayedHand} using the
     * engine's forced trash helper. That prompt contains only {@code TrashCardDecision}s and does
     * not allow an {@code EndPhaseDecision}. The trashed card is removed from the deck entirely:
     * it does not move to {@code discardPile} or {@code playedHand}.</p >
     *
     * <p>After trashing, the engine builds {@code GainCardDecision}s for every in-stock supply
     * card whose cost is at most {@code trashed.cost() + 2}. Gaining is mandatory whenever legal
     * options exist, so the prompt contains only gain decisions and no
     * {@code EndPhaseDecision}. Any gained card is created as a fresh card instance and placed
     * into {@code discardPile}.</p >
     */
    private void resolveRefactor(GameEngine engine, int playerIndex, DeckState state)
        throws PlayerViolationException {
        Card trashed = engine.forceTrash(playerIndex, state);
        if (trashed == null) {
            return;
        }

        int maxCost = trashed.cost() + 2;
        List<Decision> options = new ArrayList<>();
        for (Map.Entry<Card.Type, Integer> entry : engine.supplyCounts().entrySet()) {
            if (entry.getValue() > 0 && entry.getKey().cost() <= maxCost) {
                options.add(new GainCardDecision(entry.getKey()));
            }
        }
        if (options.isEmpty()) {
            return;
        }

        Decision choice = engine.promptPlayer(playerIndex, GameState.TurnPhase.GAIN, options);
        GainCardDecision gain = (GainCardDecision) choice;
        engine.supplyCounts().put(gain.cardType(), engine.supplyCounts().get(gain.cardType()) - 1);
        state.discardPile.add(engine.newCard(gain.cardType()));
    }

    /**
     * Resolves Parallelization.
     *
     * <p>The engine offers {@code PlayCardDecision}s for every other action card still present in
     * {@code unplayedHand}. The currently resolving Parallelization card is excluded from those
     * options. Because the card text says "may," the player may also decline by choosing
     * {@code EndPhaseDecision(ACTION)}. If an action card is chosen, that selected card is removed
     * from {@code unplayedHand}, moved to {@code playedHand}, and then its effect is resolved
     * twice. Those two extra resolutions do not consume additional actions beyond the action
     * already spent to play Parallelization.</p >
     */
    private void resolveParallelization(GameEngine engine, int playerIndex, DeckState state, Card card)
        throws PlayerViolationException {
        List<Decision> options = new ArrayList<>();
        for (Card handCard : state.unplayedHand) {
            if (!handCard.equals(card) && handCard.category() == Card.Type.Category.ACTION) {
                options.add(new PlayCardDecision(handCard));
            }
        }
        if (options.isEmpty()) {
            return;
        }

        options.add(new EndPhaseDecision(GameState.TurnPhase.ACTION));
        Decision choice = engine.promptPlayer(playerIndex, GameState.TurnPhase.ACTION, options);
        if (choice instanceof EndPhaseDecision) {
            return;
        }
        PlayCardDecision play = (PlayCardDecision) choice;
        if (!state.unplayedHand.remove(play.card())) {
            throw new PlayerViolationException("Tried to parallelize a card not in hand");
        }

        state.playedHand.add(play.card());
        resolve(engine, playerIndex, state, play.card());
        resolve(engine, playerIndex, state, play.card());
    }

    /**
     * Resolves Merge Conflict.
     *
     * <p>The active player must trash exactly one card from {@code unplayedHand}. As with other
     * forced trash effects, the prompt offers only {@code TrashCardDecision}s and no
     * {@code EndPhaseDecision}. The trashed card is removed from the deck entirely.</p >
     *
     * <p>After the trash decision, the engine saves the trashed card's {@code cost()} and draws
     * exactly that many cards into {@code unplayedHand}. The draw count is based on the cost of
     * the one trashed card, not on the number of cards trashed. For example, trashing one card
     * with cost 3 draws 3 cards.</p >
     */
    private void resolveMergeConflict(GameEngine engine, int playerIndex, DeckState state)
        throws PlayerViolationException {
        Card trashed = engine.forceTrash(playerIndex, state);
        if (trashed != null) {
            int cardsToDraw = trashed.cost();
            engine.drawCards(state, cardsToDraw);
        }
    }

    /**
     * Resolves Ransomware.
     *
     * <p>The active player gets +3 Cards. Each other player is then attacked. A player with
     * {@link Card.Type#MONITORING} in hand is first prompted to reveal it in the reaction phase.
     * Revealing Monitoring avoids the attack, logs a {@code GameEvent}, and does not move the
     * Monitoring card out of hand.</p >
     *
     * <p>If the player does not reveal Monitoring, they must choose one of two
     * {@code ChooseEffectDecision}s: discard up to 2 cards or gain a Bug. Choosing the discard
     * branch uses the engine's mandatory discard helper for up to two discards. Choosing the Bug
     * branch gains a {@link Card.Type#BUG} to {@code discardPile}.</p >
     */
    private void resolveRansomware(GameEngine engine, int playerIndex, DeckState state)
        throws PlayerViolationException {
        engine.drawCards(state, 3);
        for (int i = 0; i < engine.deckStates().size(); i++) {
            if (i == playerIndex) {
                continue;
            }

            DeckState target = engine.deckStates().get(i);
            if (revealMonitoringIfChosen(engine, i, target, "RANSOMWARE")) {
                continue;
            }

            List<Decision> options = List.of(
                new ChooseEffectDecision(ChooseEffectDecision.Effect.RANSOMWARE_DISCARD_TWO),
                new ChooseEffectDecision(ChooseEffectDecision.Effect.RANSOMWARE_GAIN_BUG)
            );
            Decision choice = engine.promptPlayer(i, GameState.TurnPhase.REACTION, options);
            ChooseEffectDecision effect = (ChooseEffectDecision) choice;
            if (effect.effect() == ChooseEffectDecision.Effect.RANSOMWARE_GAIN_BUG) {
                engine.gainBug(target);
            } else {
                engine.discardUpTo(i, 2);
            }
        }
    }

    /**
     * Prompts a target player to decide whether to reveal Monitoring from hand.
     *
     * <p>If at least one {@link Card.Type#MONITORING} is present in {@code unplayedHand}, the
     * player receives a reaction prompt containing a {@code PlayCardDecision} for each Monitoring
     * card plus {@code EndPhaseDecision(REACTION)}. Choosing a Monitoring decision represents
     * revealing the card to avoid the named attack. The revealed card intentionally remains in
     * {@code unplayedHand}; it is not treated as a normal played action. Choosing the end-phase
     * option means the player declines to reveal Monitoring and the attack continues to resolve.</p >
     *
     * @return {@code true} when the player reveals Monitoring and avoids the attack
     */
    private boolean revealMonitoringIfChosen(GameEngine engine, int playerIndex, DeckState state,
                                             String attackName)
        throws PlayerViolationException {
        List<Decision> options = new ArrayList<>();
        for (Card handCard : state.unplayedHand) {
            if (handCard.type() == Card.Type.MONITORING) {
                options.add(new PlayCardDecision(handCard));
            }
        }
        if (options.isEmpty()) {
            return false;
        }

        options.add(new EndPhaseDecision(GameState.TurnPhase.REACTION));
        Decision choice = engine.promptPlayer(playerIndex, GameState.TurnPhase.REACTION, options);
        if (choice instanceof PlayCardDecision) {
            engine.notifyEvent(playerIndex, GameState.TurnPhase.REACTION,
                "Player avoided " + attackName + " with Monitoring");
            return true;
        }
        return false;
    }

    /**
     * Resolves Unit Test by applying the chosen bonus effect.
     */
    private void resolveUnitTest(GameEngine engine, int playerIndex, DeckState state)
        throws PlayerViolationException {
        List<Decision> options = List.of(
            new ChooseEffectDecision(ChooseEffectDecision.Effect.UNIT_TEST_PLUS_TWO_ACTIONS),
            new ChooseEffectDecision(ChooseEffectDecision.Effect.UNIT_TEST_PLUS_TWO_MONEY),
            new ChooseEffectDecision(ChooseEffectDecision.Effect.UNIT_TEST_PLUS_TWO_CARDS)
        );

        Decision choice = engine.promptPlayer(playerIndex, GameState.TurnPhase.ACTION, options);
        ChooseEffectDecision effect = (ChooseEffectDecision) choice;
        switch (effect.effect()) {
            case UNIT_TEST_PLUS_TWO_ACTIONS -> state.actions += 2;
            case UNIT_TEST_PLUS_TWO_MONEY -> state.money += 2;
            case UNIT_TEST_PLUS_TWO_CARDS -> engine.drawCards(state, 2);
            default -> throw new PlayerViolationException("Unsupported Unit Test choice");
        }
    }
}