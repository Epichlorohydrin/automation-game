package edu.brandeis.cosi103a.groupa.players;

import com.google.common.collect.ImmutableList;
import edu.brandeis.cosi.atg.cards.Card;
import edu.brandeis.cosi.atg.decisions.BuyDecision;
import edu.brandeis.cosi.atg.decisions.ChooseEffectDecision;
import edu.brandeis.cosi.atg.decisions.Decision;
import edu.brandeis.cosi.atg.decisions.DiscardCardDecision;
import edu.brandeis.cosi.atg.decisions.EndPhaseDecision;
import edu.brandeis.cosi.atg.decisions.GainCardDecision;
import edu.brandeis.cosi.atg.decisions.PlayCardDecision;
import edu.brandeis.cosi.atg.decisions.TrashCardDecision;
import edu.brandeis.cosi.atg.event.Event;
import edu.brandeis.cosi.atg.event.GameObserver;
import edu.brandeis.cosi.atg.player.Player;
import edu.brandeis.cosi.atg.state.GameState;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Automated player with a fixed buy priority for high-impact action and point cards.
 *
 * <p>Primary buy order is based on descending card cost, with ties broken by the
 * requested order: Framework, Module, Refactor, Tech Debt, Sprint Planning,
 * Unit Test, Method.</p>
 *
 * <p>Outside the buy phase, this player also makes lightweight smarter choices for
 * play, trash, discard, and Unit Test effect decisions.</p>
 */
public class PriorityAutoplayer implements Player {

    private static final int UNLIMITED = -1;
    private static final int MODULE_MIN_CARDS = 26;
    private static final int MAX_REFACTOR = 2;
    private static final int MAX_TECH_DEBT = 3;
    private static final int MAX_SPRINT_PLANNING = 4;
    private static final int MAX_UNIT_TEST = 4;

    private static final List<Card.Type> BUY_PRIORITY = List.of(
        Card.Type.FRAMEWORK,
        Card.Type.DOGECOIN,
        Card.Type.MODULE,
        Card.Type.REFACTOR,
        Card.Type.TECH_DEBT,
        Card.Type.SPRINT_PLANNING,
        Card.Type.UNIT_TEST,
        Card.Type.ETHEREUM
    );

    private static final List<Card.Type> PLAY_PRIORITY = List.of(
        Card.Type.UNIT_TEST,
        Card.Type.SPRINT_PLANNING,
        Card.Type.TECH_DEBT,
        Card.Type.REFACTOR,
        Card.Type.DOGECOIN,
        Card.Type.ETHEREUM,
        Card.Type.BITCOIN
    );

    private static final List<Card.Type> TRASH_PRIORITY = List.of(
        Card.Type.BUG,
        Card.Type.BITCOIN
    );

    /**
     * Returns the display name for this player implementation.
     *
     * @return stable player name
     */
    @Override
    public String getName() {
        return "PriorityAutoplayer";
    }

    /**
     * Selects a decision using buy, play, trash, discard, and effect priorities.
     *
     * @param state the current visible game state
     * @param options the legal decisions available this turn
     * @param lastEvent the most recent event, if any
     * @return the selected decision
     */
    @Override
    public Decision makeDecision(GameState state, ImmutableList<Decision> options, Optional<Event> lastEvent) {
        if (options == null || options.isEmpty()) {
            throw new IllegalArgumentException("options must be non-empty");
        }

        if (options.size() == 1) {
            return options.get(0);
        }

        if (containsBuyDecision(options)) {
            return chooseCardAcquisitionDecision(state, options, BuyDecision.class, BuyDecision::cardType)
                .or(() -> chooseBuyPhaseEndDecision(options))
                .orElseThrow(() -> new IllegalStateException(
                    "Buy decisions were offered, but no prioritized buy or buy-phase end decision was available"
                ));
        }

        if (containsGainDecision(options)) {
            return chooseCardAcquisitionDecision(state, options, GainCardDecision.class, GainCardDecision::cardType)
                .or(() -> chooseGainPhaseEndDecision(options))
                .or(() -> chooseSpecificGainDecision(Card.Type.BITCOIN, options))
                .orElseThrow(() -> new IllegalStateException(
                    "Gain decisions were offered, but no prioritized gain, gain-phase end decision, "
                        + "or Bitcoin gain was available"
                ));
        }

        if (containsTrashDecision(options)) {
            Optional<Decision> prioritizedTrash = containsTrashPriorityTarget(options)
                ? chooseTrashDecision(options)
                : Optional.empty();

            return prioritizedTrash
                .or(() -> chooseActionPhaseEndDecision(options))
                .or(() -> chooseLowestCostTrashDecision(options))
                .orElseThrow(() -> new IllegalStateException(
                    "Trash decisions were offered, but no prioritized trash, action-phase end decision, "
                        + "or lowest-cost trash fallback was available"
                ));
        }

        return chooseCardAcquisitionDecision(state, options, GainCardDecision.class, GainCardDecision::cardType)
            .or(() -> chooseCardAcquisitionDecision(state, options, BuyDecision.class, BuyDecision::cardType))
            .or(() -> choosePlayDecision(options))
            .or(() -> chooseTrashDecision(options))
            .or(() -> chooseDiscardDecision(options))
            .or(() -> chooseEffectDecision(state, options))
            .orElse(options.get(0));
    }

    /**
     * Backward-compatible overload that delegates to the full decision method.
     *
     * @param state the current visible game state
     * @param options the legal decisions available this turn
     * @return the selected decision
     */
    public Decision makeDecision(GameState state, ImmutableList<Decision> options) {
        return makeDecision(state, options, Optional.empty());
    }

    /**
     * Selects a buy or gain decision using priority order and acquisition constraints.
     *
     * @param state the current visible game state
     * @param options the legal decisions available this turn
     * @param decisionClass acquisition decision type to evaluate
     * @param cardTypeExtractor function that extracts the candidate card type
     * @param <T> acquisition decision subtype
     * @return an optional selected acquisition decision
     */
    private <T extends Decision> Optional<Decision> chooseCardAcquisitionDecision(
        GameState state,
        ImmutableList<Decision> options,
        Class<T> decisionClass,
        java.util.function.Function<T, Card.Type> cardTypeExtractor
    ) {
        List<T> acquisitionOptions = options.stream()
            .filter(decisionClass::isInstance)
            .map(decisionClass::cast)
            .toList();

        for (Card.Type cardType : BUY_PRIORITY) {
            Optional<Decision> prioritized = acquisitionOptions.stream()
                .filter(decision -> cardTypeExtractor.apply(decision) == cardType)
                .filter(decision -> isBuyAllowed(state, cardTypeExtractor.apply(decision)))
                .findFirst()
                .map(Decision.class::cast);
            if (prioritized.isPresent()) {
                return prioritized;
            }
        }

        return Optional.empty();
    }

    /**
     * Selects the highest-priority play decision.
     *
     * @param options the legal decisions available this turn
     * @return an optional selected play decision
     */
    private Optional<Decision> choosePlayDecision(ImmutableList<Decision> options) {
        return options.stream()
            .filter(PlayCardDecision.class::isInstance)
            .map(PlayCardDecision.class::cast)
            .min(Comparator.comparingInt(this::playPriorityIndex)
                .thenComparingInt(this::cardValueDescendingKey))
            .map(Decision.class::cast);
    }

    /**
     * Selects the highest-priority trash decision.
     *
     * @param options the legal decisions available this turn
     * @return an optional selected trash decision
     */
    private Optional<Decision> chooseTrashDecision(ImmutableList<Decision> options) {
        return options.stream()
            .filter(TrashCardDecision.class::isInstance)
            .map(TrashCardDecision.class::cast)
            .filter(decision -> TRASH_PRIORITY.contains(decision.card().type()))
            .min(Comparator.comparingInt(this::trashPriorityIndex)
                .thenComparingInt(this::cardValueAscendingKey))
            .map(Decision.class::cast);
    }

    /**
     * Selects the lowest-cost trash decision from the offered legal options.
     *
     * @param options the legal decisions available this turn
     * @return an optional selected trash decision
     */
    private Optional<Decision> chooseLowestCostTrashDecision(ImmutableList<Decision> options) {
        return options.stream()
            .filter(TrashCardDecision.class::isInstance)
            .map(TrashCardDecision.class::cast)
            .min(Comparator.comparingInt((TrashCardDecision decision) -> decision.card().cost())
                .thenComparingInt(this::cardValueAscendingKey))
            .map(Decision.class::cast);
    }

    /**
     * Selects the highest-priority discard decision.
     *
     * @param options the legal decisions available this turn
     * @return an optional selected discard decision
     */
    private Optional<Decision> chooseDiscardDecision(ImmutableList<Decision> options) {
        return options.stream()
            .filter(DiscardCardDecision.class::isInstance)
            .map(DiscardCardDecision.class::cast)
            .min(Comparator.comparingInt(this::discardPriorityIndex)
                .thenComparingInt(this::cardValueAscendingKey))
            .map(Decision.class::cast);
    }

    /**
     * Selects the Unit Test effect using current state heuristics.
     *
     * @param state the current visible game state
     * @param options the legal decisions available this turn
     * @return an optional selected effect decision
     */
    private Optional<Decision> chooseEffectDecision(GameState state, ImmutableList<Decision> options) {
        Collection<Card> visibleCards = state == null || state.currentPlayerHand() == null
            ? List.of()
            : state.currentPlayerHand().getAllCards();

        if (state != null && state.spendableMoney() > 6 && state.spendableMoney() < 8) {
            return findEffectDecision(ChooseEffectDecision.Effect.UNIT_TEST_PLUS_TWO_MONEY, options);
        }

        if (visibleCards != null && visibleCards.size() > 20) {
            return findEffectDecision(ChooseEffectDecision.Effect.UNIT_TEST_PLUS_TWO_CARDS, options);
        }

        boolean hasActionCard = visibleCards != null
            && visibleCards.stream().anyMatch(card -> card.type().category() == Card.Type.Category.ACTION);
        if (state != null && state.availableActions() == 0 && hasActionCard) {
            return findEffectDecision(ChooseEffectDecision.Effect.UNIT_TEST_PLUS_TWO_ACTIONS, options);
        }

        return findEffectDecision(ChooseEffectDecision.Effect.UNIT_TEST_PLUS_TWO_MONEY, options);
    }

    /**
     * Finds the first effect decision matching the requested effect.
     *
     * @param effect target effect type
     * @param options the legal decisions available this turn
     * @return an optional matching effect decision
     */
    private Optional<Decision> findEffectDecision(ChooseEffectDecision.Effect effect,
                                                  ImmutableList<Decision> options) {
        return options.stream()
            .filter(ChooseEffectDecision.class::isInstance)
            .map(ChooseEffectDecision.class::cast)
            .filter(decision -> decision.effect() == effect)
            .findFirst()
            .map(Decision.class::cast);
    }

    /**
     * Finds a gain decision for a specific card type.
     *
     * @param cardType target gain card type
     * @param options the legal decisions available this turn
     * @return an optional matching gain decision
     */
    private Optional<Decision> chooseSpecificGainDecision(Card.Type cardType, ImmutableList<Decision> options) {
        return options.stream()
            .filter(GainCardDecision.class::isInstance)
            .map(GainCardDecision.class::cast)
            .filter(decision -> decision.cardType() == cardType)
            .findFirst()
            .map(Decision.class::cast);
    }

    /**
     * Checks whether buy decisions are present in the current option list.
     *
     * @param options the legal decisions available this turn
     * @return true when at least one buy decision exists
     */
    private boolean containsBuyDecision(ImmutableList<Decision> options) {
        return options.stream().anyMatch(BuyDecision.class::isInstance);
    }

    /**
     * Checks whether gain decisions are present in the current option list.
     *
     * @param options the legal decisions available this turn
     * @return true when at least one gain decision exists
     */
    private boolean containsGainDecision(ImmutableList<Decision> options) {
        return options.stream().anyMatch(GainCardDecision.class::isInstance);
    }

    /**
     * Checks whether trash decisions are present in the current option list.
     *
     * @param options the legal decisions available this turn
     * @return true when at least one trash decision exists
     */
    private boolean containsTrashDecision(ImmutableList<Decision> options) {
        return options.stream().anyMatch(TrashCardDecision.class::isInstance);
    }

    /**
     * Checks whether any offered trash option matches the configured trash priority list.
     *
     * @param options the legal decisions available this turn
     * @return true when a prioritized trash target is present
     */
    private boolean containsTrashPriorityTarget(ImmutableList<Decision> options) {
        return options.stream()
            .filter(TrashCardDecision.class::isInstance)
            .map(TrashCardDecision.class::cast)
            .anyMatch(decision -> TRASH_PRIORITY.contains(decision.card().type()));
    }

    /**
     * Selects an end-of-buy-phase decision when skipping a buy.
     *
     * @param options the legal decisions available this turn
     * @return an optional buy-phase end decision
     */
    private Optional<Decision> chooseBuyPhaseEndDecision(ImmutableList<Decision> options) {
        return options.stream()
            .filter(EndPhaseDecision.class::isInstance)
            .map(EndPhaseDecision.class::cast)
            .filter(decision -> decision.phase() == GameState.TurnPhase.BUY)
            .findFirst()
            .map(Decision.class::cast);
    }

    /**
     * Selects an end-of-gain-phase decision when skipping a gain.
     *
     * @param options the legal decisions available this turn
     * @return an optional gain-phase end decision
     */
    private Optional<Decision> chooseGainPhaseEndDecision(ImmutableList<Decision> options) {
        return options.stream()
            .filter(EndPhaseDecision.class::isInstance)
            .map(EndPhaseDecision.class::cast)
            .filter(decision -> decision.phase() == GameState.TurnPhase.GAIN)
            .findFirst()
            .map(Decision.class::cast);
    }

    /**
     * Selects an end-of-action-phase decision when skipping trashing.
     *
     * @param options the legal decisions available this turn
     * @return an optional action-phase end decision
     */
    private Optional<Decision> chooseActionPhaseEndDecision(ImmutableList<Decision> options) {
        return options.stream()
            .filter(EndPhaseDecision.class::isInstance)
            .map(EndPhaseDecision.class::cast)
            .filter(decision -> decision.phase() == GameState.TurnPhase.ACTION)
            .findFirst()
            .map(Decision.class::cast);
    }

    /**
     * Computes a sortable priority key for playable cards.
     *
     * @param decision candidate play decision
     * @return lower value means higher priority
     */
    private int playPriorityIndex(PlayCardDecision decision) {
        int configuredIndex = PLAY_PRIORITY.indexOf(decision.card().type());
        if (configuredIndex >= 0) {
            return configuredIndex;
        }

        return PLAY_PRIORITY.size() + 100 - decision.card().value();
    }

    /**
     * Computes a sortable priority key for trash decisions.
     *
     * @param decision candidate trash decision
     * @return lower value means higher priority
     */
    private int trashPriorityIndex(TrashCardDecision decision) {
        return removalPriorityIndex(decision.card().type(), decision.card().value());
    }

    /**
     * Computes a sortable priority key for discard decisions.
     *
     * @param decision candidate discard decision
     * @return lower value means higher priority
     */
    private int discardPriorityIndex(DiscardCardDecision decision) {
        return removalPriorityIndex(decision.card().type(), decision.card().value());
    }

    /**
     * Computes a shared removal priority key for trash and discard decisions.
     *
     * @param type card type being removed
     * @param value card point or money value
     * @return lower value means higher priority
     */
    private int removalPriorityIndex(Card.Type type, int value) {
        int configuredIndex = TRASH_PRIORITY.indexOf(type);
        if (configuredIndex >= 0) {
            return configuredIndex;
        }

        return TRASH_PRIORITY.size() + value;
    }

    /**
     * Provides a descending value tie-break key for play decisions.
     *
     * @param decision candidate play decision
     * @return negative card value for descending sorting
     */
    private int cardValueDescendingKey(PlayCardDecision decision) {
        return -decision.card().value();
    }

    /**
     * Provides an ascending value tie-break key for trash decisions.
     *
     * @param decision candidate trash decision
     * @return card value for ascending sorting
     */
    private int cardValueAscendingKey(TrashCardDecision decision) {
        return decision.card().value();
    }

    /**
     * Provides an ascending value tie-break key for discard decisions.
     *
     * @param decision candidate discard decision
     * @return card value for ascending sorting
     */
    private int cardValueAscendingKey(DiscardCardDecision decision) {
        return decision.card().value();
    }

    /**
     * Validates whether buying or gaining a card is allowed under strategy limits.
     *
     * @param state the current visible game state
     * @param cardType candidate card type to acquire
     * @return true when the acquisition is allowed
     */
    private boolean isBuyAllowed(GameState state, Card.Type cardType) {
        if (cardType == Card.Type.MODULE && playerCardCount(state) < MODULE_MIN_CARDS) {
            return false;
        }

        int maxCopies = maxCopies(cardType);
        if (maxCopies == UNLIMITED) {
            return true;
        }

        return ownedCardCount(state, cardType) < maxCopies;
    }

    /**
     * Returns the configured maximum number of copies for a card type.
     *
     * @param cardType card type to evaluate
     * @return maximum copy count, or UNLIMITED when uncapped
     */
    private int maxCopies(Card.Type cardType) {
        return switch (cardType) {
            case REFACTOR -> MAX_REFACTOR;
            case TECH_DEBT -> MAX_TECH_DEBT;
            case SPRINT_PLANNING -> MAX_SPRINT_PLANNING;
            case UNIT_TEST -> MAX_UNIT_TEST;
            default -> UNLIMITED;
        };
    }

    /**
     * Counts visible cards for the current player.
     *
     * @param state the current visible game state
     * @return visible card count, or zero when unavailable
     */
    private int playerCardCount(GameState state) {
        if (state == null || state.currentPlayerHand() == null) {
            return 0;
        }

        Collection<Card> visibleCards = state.currentPlayerHand().getAllCards();
        return visibleCards == null ? 0 : visibleCards.size();
    }

    /**
     * Counts visible cards of a specific type for the current player.
     *
     * @param state the current visible game state
     * @param cardType card type to count
     * @return number of visible cards matching the requested type
     */
    private int ownedCardCount(GameState state, Card.Type cardType) {
        if (state == null || state.currentPlayerHand() == null) {
            return 0;
        }

        Collection<Card> visibleCards = state.currentPlayerHand().getAllCards();
        if (visibleCards == null) {
            return 0;
        }

        int count = 0;
        for (Card card : visibleCards) {
            if (card.type() == cardType) {
                count++;
            }
        }
        return count;
    }

    /**
     * Returns an empty observer because this player does not publish events.
     *
     * @return empty observer optional
     */
    @Override
    public Optional<GameObserver> getObserver() {
        return Optional.empty();
    }
}
