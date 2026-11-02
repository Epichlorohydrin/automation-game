package edu.brandeis.cosi103a.groupa.players;

import com.google.common.collect.ImmutableList;
import edu.brandeis.cosi.atg.cards.Card;
import edu.brandeis.cosi.atg.decisions.BuyDecision;
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

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Automated player that favors action development while preserving a money-heavy deck.
 *
 * <p>It follows the turn order of action phase, then money phase, then buy phase. On its first
 * buy round, it buys {@code Ethereum} when available. After that, it always buys {@code Framework}
 * when available. Otherwise, it prioritizes money buys
 * when money cards are not a majority of its estimated deck, prefers helpful action cards while
 * the deck is still money-heavy, prioritizes {@code Monitoring} when shared attack cards remain
 * available, and never buys more than three copies of the same action card. Among money cards,
 * it prefers {@code Dogecoin} over {@code Ethereum} over {@code Bitcoin}. In action play, it
 * plays {@code Parallelization} before any other action, then prioritizes actions that add
 * actions, then cards, then buys.</p>
 */
public class BalancedAutoPlayer implements Player {

    private static final int STARTING_BITCOINS = 7;
    private static final int STARTING_METHODS = 3;
    private static final int MAX_ACTION_CARD_COPIES = 3;
    private static final int MAX_PARALLELIZATION_COPIES = 2;

    private static final List<Card.Type> MONEY_BUY_PRIORITY = List.of(
        Card.Type.DOGECOIN,
        Card.Type.ETHEREUM,
        Card.Type.BITCOIN
    );

    private static final List<Card.Type> ATTACK_ACTION_CARDS = List.of(
        Card.Type.HACK,
        Card.Type.EVERGREEN_TEST,
        Card.Type.RANSOMWARE
    );

    private static final List<Card.Type> DEPLOYMENT_PIPELINE_OBJECTIVES = List.of(
        Card.Type.DOGECOIN,
        Card.Type.FRAMEWORK
    );

    private final Map<Card.Type, Integer> estimatedOwnedCounts = createStartingDeckEstimate();
    private boolean openingBuyRoundPending = true;

    @Override
    public String getName() {
        return "BalancedAutoPlayer";
    }

    @Override
    public Decision makeDecision(GameState state, ImmutableList<Decision> options, Optional<Event> lastEvent) {
        if (options == null || options.isEmpty()) {
            throw new IllegalArgumentException("options must be non-empty");
        }

        if (options.size() == 1) {
            return finalizeDecision(options.get(0));
        }

        Optional<GameState.TurnPhase> phase = determinePhase(state, options);
        if (phase.isPresent()) {
            return finalizeDecision(chooseDecisionForPhase(state, phase.get(), options));
        }

        Optional<Decision> actionPlay = chooseActionPlayDecision(state, options);
        if (actionPlay.isPresent()) {
            return finalizeDecision(actionPlay.get());
        }

        Optional<Decision> moneyPlay = chooseMoneyPlayDecision(options);
        if (moneyPlay.isPresent()) {
            return finalizeDecision(moneyPlay.get());
        }

        Optional<Decision> buyChoice = chooseBuyDecision(state, options);
        if (buyChoice.isPresent()) {
            return finalizeDecision(buyChoice.get());
        }

        return finalizeDecision(chooseAnyEndPhaseDecision(options).orElse(options.get(0)));
    }

    public Decision makeDecision(GameState state, ImmutableList<Decision> options) {
        return makeDecision(state, options, Optional.empty());
    }

    private static Map<Card.Type, Integer> createStartingDeckEstimate() {
        Map<Card.Type, Integer> counts = new EnumMap<>(Card.Type.class);
        counts.put(Card.Type.BITCOIN, STARTING_BITCOINS);
        counts.put(Card.Type.METHOD, STARTING_METHODS);
        return counts;
    }

    private Decision chooseDecisionForPhase(GameState state, GameState.TurnPhase phase,
                                            ImmutableList<Decision> options) {
        if (phase == GameState.TurnPhase.ACTION) {
            return chooseActionPlayDecision(state, options)
                .or(() -> chooseEndPhaseDecision(options, GameState.TurnPhase.ACTION))
                .orElse(options.get(0));
        }

        if (phase == GameState.TurnPhase.MONEY) {
            return chooseMoneyPlayDecision(options)
                .or(() -> chooseEndPhaseDecision(options, GameState.TurnPhase.MONEY))
                .orElse(options.get(0));
        }

        if (phase == GameState.TurnPhase.BUY) {
            return chooseBuyDecision(state, options)
                .or(() -> chooseEndPhaseDecision(options, GameState.TurnPhase.BUY))
                .orElse(options.get(0));
        }

        if (phase == GameState.TurnPhase.DISCARD) {
            return chooseBacklogDiscardDecision(options)
                .or(() -> chooseEndPhaseDecision(options, GameState.TurnPhase.DISCARD))
                .orElse(options.get(0));
        }

        return chooseEndPhaseDecision(options, phase).orElse(options.get(0));
    }

    private Optional<GameState.TurnPhase> determinePhase(GameState state, ImmutableList<Decision> options) {
        if (state != null && state.phase() != null) {
            return Optional.of(state.phase());
        }

        Optional<GameState.TurnPhase> endPhase = options.stream()
            .filter(EndPhaseDecision.class::isInstance)
            .map(EndPhaseDecision.class::cast)
            .map(EndPhaseDecision::phase)
            .findFirst();
        if (endPhase.isPresent()) {
            return endPhase;
        }

        boolean hasActionPlay = options.stream()
            .filter(PlayCardDecision.class::isInstance)
            .map(PlayCardDecision.class::cast)
            .anyMatch(decision -> decision.card().category() == Card.Type.Category.ACTION);
        if (hasActionPlay) {
            return Optional.of(GameState.TurnPhase.ACTION);
        }

        boolean hasMoneyPlay = options.stream()
            .filter(PlayCardDecision.class::isInstance)
            .map(PlayCardDecision.class::cast)
            .anyMatch(decision -> decision.card().category() == Card.Type.Category.MONEY);
        if (hasMoneyPlay) {
            return Optional.of(GameState.TurnPhase.MONEY);
        }

        boolean hasBuyDecision = options.stream().anyMatch(BuyDecision.class::isInstance);
        if (hasBuyDecision) {
            return Optional.of(GameState.TurnPhase.BUY);
        }

        return Optional.empty();
    }

    private Optional<Decision> chooseBacklogDiscardDecision(ImmutableList<Decision> options) {
        return options.stream()
            .filter(DiscardCardDecision.class::isInstance)
            .map(DiscardCardDecision.class::cast)
            .filter(decision -> decision.card().category() == Card.Type.Category.VICTORY)
            .min(Comparator.comparingInt(this::discardCardValueAscendingKey)
                .thenComparing(this::discardCardDescription))
            .map(Decision.class::cast);
    }

    private Optional<Decision> chooseBuyDecision(GameState state, ImmutableList<Decision> options) {
        List<BuyDecision> buyOptions = options.stream()
            .filter(BuyDecision.class::isInstance)
            .map(BuyDecision.class::cast)
            .toList();

        if (buyOptions.isEmpty()) {
            return Optional.empty();
        }

        Optional<Decision> openingEthereumBuy = chooseOpeningEthereumBuy(buyOptions);
        if (openingEthereumBuy.isPresent()) {
            return openingEthereumBuy;
        }

        Optional<Decision> frameworkBuy = chooseFrameworkBuy(buyOptions);
        if (frameworkBuy.isPresent()) {
            return frameworkBuy;
        }

        if (shouldPrioritizeMoneyBuy()) {
            Optional<Decision> moneyBuy = chooseMoneyBuy(buyOptions);
            if (moneyBuy.isPresent()) {
                return moneyBuy;
            }
        }

        Optional<Decision> monitoringBuy = chooseMonitoringDefenseBuy(state, buyOptions);
        if (monitoringBuy.isPresent()) {
            return monitoringBuy;
        }

        Optional<Decision> lowMoneyMonitoringBuy = chooseLowMoneyMonitoringBuy(buyOptions);
        if (lowMoneyMonitoringBuy.isPresent()) {
            return lowMoneyMonitoringBuy;
        }

        Optional<Decision> actionBuy = chooseStrategicActionBuy(buyOptions);
        if (actionBuy.isPresent()) {
            return actionBuy;
        }

        return chooseMoneyBuy(buyOptions);
    }

    private Optional<Decision> chooseOpeningEthereumBuy(List<BuyDecision> buyOptions) {
        if (!openingBuyRoundPending) {
            return Optional.empty();
        }

        return buyOptions.stream()
            .filter(decision -> decision.cardType() == Card.Type.ETHEREUM)
            .findFirst()
            .map(Decision.class::cast);
    }

    private Optional<Decision> chooseFrameworkBuy(List<BuyDecision> buyOptions) {
        return buyOptions.stream()
            .filter(decision -> decision.cardType() == Card.Type.FRAMEWORK)
            .findFirst()
            .map(Decision.class::cast);
    }

    private boolean shouldPrioritizeMoneyBuy() {
        int totalCards = estimatedTotalCardCount();
        int moneyCards = estimatedMoneyCardCount();
        return moneyCards * 2 < totalCards;
    }

    private Optional<Decision> chooseMonitoringDefenseBuy(GameState state, List<BuyDecision> buyOptions) {
        if (estimatedCopiesOf(Card.Type.MONITORING) >= MAX_ACTION_CARD_COPIES) {
            return Optional.empty();
        }

        if (!attackCardsRemainAvailable(state, buyOptions)) {
            return Optional.empty();
        }

        return buyOptions.stream()
            .filter(decision -> decision.cardType() == Card.Type.MONITORING)
            .findFirst()
            .map(Decision.class::cast);
    }

    private Optional<Decision> chooseLowMoneyMonitoringBuy(List<BuyDecision> buyOptions) {
        if (estimatedCopiesOf(Card.Type.MONITORING) >= MAX_ACTION_CARD_COPIES) {
            return Optional.empty();
        }

        boolean moreExpensiveBuyAvailable = buyOptions.stream()
            .anyMatch(decision -> decision.cardType().cost() > Card.Type.MONITORING.cost());
        if (moreExpensiveBuyAvailable) {
            return Optional.empty();
        }

        return buyOptions.stream()
            .filter(decision -> decision.cardType() == Card.Type.MONITORING)
            .findFirst()
            .map(Decision.class::cast);
    }

    private boolean attackCardsRemainAvailable(GameState state, List<BuyDecision> buyOptions) {
        if (state != null && state.buyableCards() != null) {
            for (Card.Type attackType : ATTACK_ACTION_CARDS) {
                if (state.buyableCards().getNumAvailable(attackType) > 0) {
                    return true;
                }
            }
        }

        return buyOptions.stream().anyMatch(decision -> ATTACK_ACTION_CARDS.contains(decision.cardType()));
    }

    private Optional<Decision> chooseStrategicActionBuy(List<BuyDecision> buyOptions) {
        return buyOptions.stream()
            .filter(decision -> decision.cardType().category() == Card.Type.Category.ACTION)
            .filter(decision -> isHelpfulActionCard(decision.cardType()))
            .filter(decision -> estimatedCopiesOf(decision.cardType()) < maxActionCardCopies(decision.cardType()))
            .min(Comparator.comparingInt(this::buyAddsActionsDescendingKey)
                .thenComparingInt(this::buyAddsCardsDescendingKey)
                .thenComparingInt(this::buyAddsBuysDescendingKey)
                .thenComparingInt(this::buyCostDescendingKey)
                .thenComparingInt(this::buyParallelizationPriorityKey)
                .thenComparing(this::buyCardDescription))
            .map(Decision.class::cast);
    }

    private int maxActionCardCopies(Card.Type cardType) {
        return cardType == Card.Type.PARALLELIZATION
            ? MAX_PARALLELIZATION_COPIES
            : MAX_ACTION_CARD_COPIES;
    }

    private Optional<Decision> chooseMoneyBuy(List<BuyDecision> buyOptions) {
        for (Card.Type cardType : MONEY_BUY_PRIORITY) {
            Optional<Decision> choice = buyOptions.stream()
                .filter(decision -> decision.cardType() == cardType)
                .findFirst()
                .map(Decision.class::cast);
            if (choice.isPresent()) {
                return choice;
            }
        }
        return Optional.empty();
    }

    private Optional<Decision> chooseActionPlayDecision(GameState state, ImmutableList<Decision> options) {
        Optional<Decision> parallelizationPlay = options.stream()
            .filter(PlayCardDecision.class::isInstance)
            .map(PlayCardDecision.class::cast)
            .filter(decision -> decision.card().type() == Card.Type.PARALLELIZATION)
            .findFirst()
            .map(Decision.class::cast);
        if (parallelizationPlay.isPresent()) {
            return parallelizationPlay;
        }

        List<PlayCardDecision> actionPlayOptions = playableActionOptions(state, options);
        if (isParallelizationComboChoice(state, actionPlayOptions)) {
            return chooseParallelizationComboDecision(actionPlayOptions);
        }

        return actionPlayOptions.stream()
            .min(Comparator.comparingInt(this::playAddsActionsDescendingKey)
                .thenComparingInt(decision -> evergreenAttackPriorityKey(state, decision))
                .thenComparingInt(decision -> hackAttackPriorityKey(state, decision))
                .thenComparingInt(this::playAddsCardsDescendingKey)
                .thenComparingInt(this::playAddsBuysDescendingKey)
                .thenComparingInt(this::playCardCostDescendingKey)
                .thenComparing(this::playCardDescription))
            .map(Decision.class::cast);
    }

    private List<PlayCardDecision> playableActionOptions(GameState state, ImmutableList<Decision> options) {
        return options.stream()
            .filter(PlayCardDecision.class::isInstance)
            .map(PlayCardDecision.class::cast)
            .filter(decision -> decision.card().category() == Card.Type.Category.ACTION)
            .filter(decision -> isHelpfulActionCard(decision.card().type())
                || decision.card().type() == Card.Type.HACK)
            .filter(decision -> shouldPlayActionCard(state, decision.card().type()))
            .toList();
    }

    private boolean isParallelizationComboChoice(GameState state, List<PlayCardDecision> actionPlayOptions) {
        return state != null
            && state.currentPlayerHand() != null
            && !actionPlayOptions.isEmpty()
            && state.currentPlayerHand().playedCards().stream()
                .anyMatch(card -> card.type() == Card.Type.PARALLELIZATION);
    }

    private Optional<Decision> chooseParallelizationComboDecision(List<PlayCardDecision> actionPlayOptions) {
        Comparator<PlayCardDecision> comparator = hasMultiplePlusActionChoices(actionPlayOptions)
            ? Comparator.comparingInt(this::playCardsDrawnDescendingKey)
                .thenComparingInt(this::playActionsAddedDescendingKey)
            : Comparator.comparingInt(this::playActionsAddedDescendingKey)
                .thenComparingInt(this::playCardsDrawnDescendingKey);

        return actionPlayOptions.stream()
            .min(comparator
                .thenComparingInt(this::playAddsBuysDescendingKey)
                .thenComparingInt(this::playCardCostDescendingKey)
                .thenComparing(this::playCardDescription))
            .map(Decision.class::cast);
    }

    private boolean hasMultiplePlusActionChoices(List<PlayCardDecision> actionPlayOptions) {
        long plusActionChoices = actionPlayOptions.stream()
            .filter(decision -> actionsAddedBy(decision.card().type()) > 0)
            .limit(2)
            .count();
        return plusActionChoices > 1;
    }

    private boolean shouldPlayActionCard(GameState state, Card.Type cardType) {
        if (isAttackCard(cardType) && attackPlayLimitReached(state)) {
            return false;
        }

        if (cardType != Card.Type.DEPLOYMENT_PIPELINE) {
            return cardType != Card.Type.HACK || shouldPlayHackAttack(state);
        }

        return isOneMoneyShortOfDeploymentPipelineObjective(state);
    }

    private boolean isOneMoneyShortOfDeploymentPipelineObjective(GameState state) {
        if (state == null) {
            return false;
        }

        int spendableMoney = state.spendableMoney();
        return DEPLOYMENT_PIPELINE_OBJECTIVES.stream()
            .filter(objective -> objectiveAvailable(state, objective))
            .anyMatch(objective -> spendableMoney == objective.cost() - 1);
    }

    private boolean objectiveAvailable(GameState state, Card.Type objective) {
        return state.buyableCards() == null || state.buyableCards().getNumAvailable(objective) > 0;
    }

    private int evergreenAttackPriorityKey(GameState state, PlayCardDecision decision) {
        return decision.card().type() == Card.Type.EVERGREEN_TEST
            && shouldPrioritizeEvergreenAttack(state) ? -1 : 0;
    }

    private int hackAttackPriorityKey(GameState state, PlayCardDecision decision) {
        return decision.card().type() == Card.Type.HACK
            && shouldPrioritizeHackAttack(state) ? -1 : 0;
    }

    private boolean shouldPrioritizeEvergreenAttack(GameState state) {
        if (state == null || state.buyableCards() == null) {
            return false;
        }

        boolean monitoringInGame = state.buyableCards().getCardTypes().contains(Card.Type.MONITORING);
        boolean bugsRemain = state.buyableCards().getNumAvailable(Card.Type.BUG) > 0;
        return !monitoringInGame && bugsRemain;
    }

    private boolean shouldPrioritizeHackAttack(GameState state) {
        return shouldPlayHackAttack(state) && !evergreenAttackPreferred(state);
    }

    private boolean shouldPlayHackAttack(GameState state) {
        if (state == null || state.buyableCards() == null) {
            return false;
        }

        return state.currentPlayerHand() == null
            || state.currentPlayerHand().playedCards().stream()
                .noneMatch(card -> card.type() == Card.Type.HACK);
    }

    private boolean evergreenAttackPreferred(GameState state) {
        return state != null
            && state.buyableCards() != null
            && state.buyableCards().getCardTypes().contains(Card.Type.EVERGREEN_TEST)
            && shouldPrioritizeEvergreenAttack(state);
    }

    private boolean monitoringInGame(GameState state) {
        return state.buyableCards().getCardTypes().contains(Card.Type.MONITORING);
    }

    private boolean attackPlayLimitReached(GameState state) {
        if (state == null || state.buyableCards() == null || !monitoringInGame(state)) {
            return false;
        }

        return state.currentPlayerHand() != null
            && state.currentPlayerHand().playedCards().stream()
                .anyMatch(card -> isAttackCard(card.type()));
    }

    private boolean isAttackCard(Card.Type cardType) {
        return ATTACK_ACTION_CARDS.contains(cardType);
    }

    private Optional<Decision> chooseMoneyPlayDecision(ImmutableList<Decision> options) {
        return options.stream()
            .filter(PlayCardDecision.class::isInstance)
            .map(PlayCardDecision.class::cast)
            .filter(decision -> decision.card().category() == Card.Type.Category.MONEY)
            .min(Comparator.comparingInt(this::playCardValueDescendingKey)
                .thenComparing(this::playCardDescription))
            .map(Decision.class::cast);
    }

    private Optional<Decision> chooseEndPhaseDecision(ImmutableList<Decision> options, GameState.TurnPhase phase) {
        return options.stream()
            .filter(EndPhaseDecision.class::isInstance)
            .map(EndPhaseDecision.class::cast)
            .filter(decision -> decision.phase() == phase)
            .findFirst()
            .map(Decision.class::cast);
    }

    private Optional<Decision> chooseAnyEndPhaseDecision(ImmutableList<Decision> options) {
        return options.stream()
            .filter(EndPhaseDecision.class::isInstance)
            .findFirst()
            .map(Decision.class::cast);
    }

    private Decision finalizeDecision(Decision decision) {
        updateOpeningBuyRound(decision);
        updateEstimatedDeck(decision);
        return decision;
    }

    private void updateOpeningBuyRound(Decision decision) {
        if (!openingBuyRoundPending) {
            return;
        }

        if (decision instanceof BuyDecision) {
            openingBuyRoundPending = false;
            return;
        }

        if (decision instanceof EndPhaseDecision endPhaseDecision
            && endPhaseDecision.phase() == GameState.TurnPhase.BUY) {
            openingBuyRoundPending = false;
        }
    }

    private void updateEstimatedDeck(Decision decision) {
        if (decision instanceof BuyDecision buyDecision) {
            incrementEstimatedCopies(buyDecision.cardType());
            return;
        }

        if (decision instanceof GainCardDecision gainDecision) {
            incrementEstimatedCopies(gainDecision.cardType());
            return;
        }

        if (decision instanceof TrashCardDecision trashDecision) {
            decrementEstimatedCopies(trashDecision.card().type());
        }
    }

    private void incrementEstimatedCopies(Card.Type cardType) {
        estimatedOwnedCounts.merge(cardType, 1, Integer::sum);
    }

    private void decrementEstimatedCopies(Card.Type cardType) {
        int updatedCount = Math.max(0, estimatedOwnedCounts.getOrDefault(cardType, 0) - 1);
        if (updatedCount == 0) {
            estimatedOwnedCounts.remove(cardType);
            return;
        }
        estimatedOwnedCounts.put(cardType, updatedCount);
    }

    private int estimatedCopiesOf(Card.Type cardType) {
        return estimatedOwnedCounts.getOrDefault(cardType, 0);
    }

    private int estimatedTotalCardCount() {
        int total = 0;
        for (int count : estimatedOwnedCounts.values()) {
            total += count;
        }
        return total;
    }

    private int estimatedMoneyCardCount() {
        int moneyCards = 0;
        for (Map.Entry<Card.Type, Integer> entry : estimatedOwnedCounts.entrySet()) {
            if (entry.getKey().category() == Card.Type.Category.MONEY) {
                moneyCards += entry.getValue();
            }
        }
        return moneyCards;
    }

    private boolean isHelpfulActionCard(Card.Type cardType) {
        return playAddsActions(cardType) || playAddsCards(cardType) || playAddsBuys(cardType);
    }

    private int buyAddsActionsDescendingKey(BuyDecision decision) {
        return playAddsActions(decision.cardType()) ? -1 : 0;
    }

    private int buyAddsCardsDescendingKey(BuyDecision decision) {
        return playAddsCards(decision.cardType()) ? -1 : 0;
    }

    private int buyAddsBuysDescendingKey(BuyDecision decision) {
        return playAddsBuys(decision.cardType()) ? -1 : 0;
    }

    private boolean playAddsActions(Card.Type cardType) {
        return actionsAddedBy(cardType) > 0;
    }

    private boolean playAddsCards(Card.Type cardType) {
        return cardsDrawnBy(cardType) > 0;
    }

    private boolean playAddsBuys(Card.Type cardType) {
        return switch (cardType) {
            case DAILY_SCRUM, SPRINT_PLANNING, DEPLOYMENT_PIPELINE -> true;
            default -> false;
        };
    }

    private int playAddsActionsDescendingKey(PlayCardDecision decision) {
        return playAddsActions(decision.card().type()) ? -1 : 0;
    }

    private int playActionsAddedDescendingKey(PlayCardDecision decision) {
        return -actionsAddedBy(decision.card().type());
    }

    private int playAddsCardsDescendingKey(PlayCardDecision decision) {
        return playAddsCards(decision.card().type()) ? -1 : 0;
    }

    private int playCardsDrawnDescendingKey(PlayCardDecision decision) {
        return -cardsDrawnBy(decision.card().type());
    }

    private int playAddsBuysDescendingKey(PlayCardDecision decision) {
        return playAddsBuys(decision.card().type()) ? -1 : 0;
    }

    private int buyCostDescendingKey(BuyDecision decision) {
        return -decision.cardType().cost();
    }

    private int buyParallelizationPriorityKey(BuyDecision decision) {
        return decision.cardType() == Card.Type.PARALLELIZATION ? -1 : 0;
    }

    private int playCardCostDescendingKey(PlayCardDecision decision) {
        return -decision.card().cost();
    }

    private String buyCardDescription(BuyDecision decision) {
        return decision.cardType().description();
    }

    private int playCardValueDescendingKey(PlayCardDecision decision) {
        return -decision.card().value();
    }

    private String playCardDescription(PlayCardDecision decision) {
        return decision.card().description();
    }

    private int actionsAddedBy(Card.Type cardType) {
        return switch (cardType) {
            case CODE_REVIEW, UNIT_TEST -> 2;
            case BACKLOG, IPO, TECH_DEBT, SPRINT_PLANNING -> 1;
            default -> 0;
        };
    }

    private int cardsDrawnBy(Card.Type cardType) {
        return switch (cardType) {
            case DAILY_SCRUM -> 4;
            case RANSOMWARE -> 3;
            case IPO, MONITORING, EVERGREEN_TEST, UNIT_TEST -> 2;
            case TECH_DEBT, CODE_REVIEW, SPRINT_PLANNING -> 1;
            case MERGE_CONFLICT, PARALLELIZATION -> 1;
            default -> 0;
        };
    }

    private int discardCardValueAscendingKey(DiscardCardDecision decision) {
        return decision.card().value();
    }

    private String discardCardDescription(DiscardCardDecision decision) {
        return decision.card().description();
    }

    @Override
    public Optional<GameObserver> getObserver() {
        return Optional.empty();
    }
}
