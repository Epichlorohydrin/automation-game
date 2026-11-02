package edu.brandeis.cosi103a.groupa.players;

import com.google.common.collect.ImmutableList;
import edu.brandeis.cosi.atg.cards.Card;
import edu.brandeis.cosi.atg.decisions.BuyDecision;
import edu.brandeis.cosi.atg.decisions.Decision;
import edu.brandeis.cosi.atg.decisions.DiscardCardDecision;
import edu.brandeis.cosi.atg.decisions.EndPhaseDecision;
import edu.brandeis.cosi.atg.decisions.PlayCardDecision;
import edu.brandeis.cosi.atg.event.Event;
import edu.brandeis.cosi.atg.event.GameObserver;
import edu.brandeis.cosi.atg.event.GainCardEvent;
import edu.brandeis.cosi.atg.event.PlayCardEvent;
import edu.brandeis.cosi.atg.player.Player;
import edu.brandeis.cosi.atg.state.GameState;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Simple automated big-money player.
 *
 * <p>This player keeps the Big Money economy core, adds a compact action package,
 * adds late-game point buys when the Framework pile is low, and plays action cards
 * intelligently.</p>
 */
public class BigMoneyPlayerNew implements Player {

    private static final int MODULE_ENDGAME_THRESHOLD = 4;
    private static final int METHOD_ENDGAME_THRESHOLD = 1;
    private static final int MAX_ATTACK_BUYS = 4;
    private static final int MAX_IPO_BUYS = 2;
    private static final int MAX_CODE_REVIEW_BUYS = 1;
    private static final int MAX_SPRINT_PLANNING_BUYS = 1;
    private static final int MAX_MONITORING_BUYS = 2;

    private static final List<Card.Type> ATTACK_BUY_PRIORITY = List.of(
        Card.Type.HACK,
        Card.Type.RANSOMWARE,
        Card.Type.EVERGREEN_TEST
    );

    private static final List<Card.Type> ATTACK_PLAY_PRIORITY = List.of(
        Card.Type.HACK,
        Card.Type.EVERGREEN_TEST,
        Card.Type.RANSOMWARE
    );

    private int attackBuys;
    private int ipoBuys;
    private int codeReviewBuys;
    private int sprintPlanningBuys;
    private int monitoringBuys;
    private boolean observedOpposingAttack;

    /**
     * Returns the player name used by the game engine.
     *
     * @return the player name
     */
    @Override
    public String getName() {
        return "bigmoneyplayernew";
    }

    /**
     * Chooses one legal decision from the available options.
     *
     * @param state the current visible game state
     * @param options the legal decisions available this turn
     * @param lastEvent the most recent event, if any
     * @return the selected decision
     */
    @Override
    public Decision makeDecision(GameState state, ImmutableList<Decision> options, Optional<Event> lastEvent) {
        updateObservedOpposingAttack(lastEvent);

        if (options == null || options.isEmpty()) {
            throw new IllegalArgumentException("options must be non-empty");
        }

        if (options.size() == 1) {
            return finalizeDecision(options.get(0));
        }

        Decision decision = chooseBuyDecision(state, options)
            .or(() -> chooseActionPlayDecision(options))
            .or(() -> chooseMoneyPlayDecision(options))
            .or(() -> chooseDiscardDecision(state, options))
            .or(() -> chooseEndPhaseDecision(options))
            .orElse(options.get(0));
        return finalizeDecision(decision);
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
     * Chooses the highest-priority available buy.
     *
     * @param state the current visible game state
     * @param options the legal decisions available this turn
     * @return an optional buy decision
     */
    private Optional<Decision> chooseBuyDecision(GameState state, ImmutableList<Decision> options) {
        List<BuyDecision> buyOptions = options.stream()
            .filter(BuyDecision.class::isInstance)
            .map(BuyDecision.class::cast)
            .toList();

        if (shouldBuyFrameworkNow(state)) {
            Optional<Decision> frameworkBuy = findBuyDecisionFor(Card.Type.FRAMEWORK, buyOptions);
            if (frameworkBuy.isPresent()) {
                return frameworkBuy;
            }
        }

        if (shouldBuyModuleNow(state)) {
            Optional<Decision> moduleBuy = findBuyDecisionFor(Card.Type.MODULE, buyOptions);
            if (moduleBuy.isPresent()) {
                return moduleBuy;
            }
        }

        Optional<Decision> dogecoinBuy = findBuyDecisionFor(Card.Type.DOGECOIN, buyOptions);
        if (dogecoinBuy.isPresent()) {
            return dogecoinBuy;
        }

        Optional<Decision> monitoringBuy = chooseMonitoringBuy(state, buyOptions);
        if (monitoringBuy.isPresent()) {
            return monitoringBuy;
        }

        Optional<Decision> attackBuy = chooseAttackBuy(buyOptions);
        if (attackBuy.isPresent()) {
            return attackBuy;
        }

        Optional<Decision> ipoBuy = chooseIpoBuy(buyOptions);
        if (ipoBuy.isPresent()) {
            return ipoBuy;
        }

        Optional<Decision> ethereumBuy = findBuyDecisionFor(Card.Type.ETHEREUM, buyOptions);
        if (ethereumBuy.isPresent()) {
            return ethereumBuy;
        }

        Optional<Decision> supportActionBuy = chooseSupportActionBuy(buyOptions);
        if (supportActionBuy.isPresent()) {
            return supportActionBuy;
        }

        if (shouldBuyMethodNow(state)) {
            Optional<Decision> methodBuy = findBuyDecisionFor(Card.Type.METHOD, buyOptions);
            if (methodBuy.isPresent()) {
                return methodBuy;
            }
        }

        return findBuyDecisionFor(Card.Type.BITCOIN, buyOptions);
    }

    private Optional<Decision> chooseAttackBuy(List<BuyDecision> buyOptions) {
        if (attackBuys >= MAX_ATTACK_BUYS) {
            return Optional.empty();
        }

        for (Card.Type attackType : ATTACK_BUY_PRIORITY) {
            Optional<Decision> attackBuy = findBuyDecisionFor(attackType, buyOptions);
            if (attackBuy.isPresent()) {
                return attackBuy;
            }
        }

        return Optional.empty();
    }

    private Optional<Decision> chooseSupportActionBuy(List<BuyDecision> buyOptions) {
        if (codeReviewBuys < MAX_CODE_REVIEW_BUYS) {
            Optional<Decision> codeReviewBuy = findBuyDecisionFor(Card.Type.CODE_REVIEW, buyOptions);
            if (codeReviewBuy.isPresent()) {
                return codeReviewBuy;
            }
        }

        if (sprintPlanningBuys < MAX_SPRINT_PLANNING_BUYS) {
            Optional<Decision> sprintPlanningBuy = findBuyDecisionFor(Card.Type.SPRINT_PLANNING, buyOptions);
            if (sprintPlanningBuy.isPresent()) {
                return sprintPlanningBuy;
            }
        }

        return Optional.empty();
    }

    private Optional<Decision> chooseMonitoringBuy(GameState state, List<BuyDecision> buyOptions) {
        if (monitoringBuys >= MAX_MONITORING_BUYS || !observedOpposingAttack) {
            return Optional.empty();
        }

        return findBuyDecisionFor(Card.Type.MONITORING, buyOptions);
    }

    private Optional<Decision> chooseIpoBuy(List<BuyDecision> buyOptions) {
        if (ipoBuys >= MAX_IPO_BUYS) {
            return Optional.empty();
        }

        return findBuyDecisionFor(Card.Type.IPO, buyOptions);
    }

    private boolean shouldBuyFrameworkNow(GameState state) {
        if (state == null) {
            return false;
        }

        if (state.spendableMoney() < Card.Type.FRAMEWORK.cost()) {
            return false;
        }

        return true;
    }

    private boolean shouldBuyModuleNow(GameState state) {
        return state != null
            && state.spendableMoney() >= Card.Type.MODULE.cost()
            && frameworksRemaining(state) <= MODULE_ENDGAME_THRESHOLD;
    }

    private boolean shouldBuyMethodNow(GameState state) {
        return state != null
            && state.spendableMoney() >= Card.Type.METHOD.cost()
            && frameworksRemaining(state) <= METHOD_ENDGAME_THRESHOLD;
    }

    private int frameworksRemaining(GameState state) {
        if (state == null || state.buyableCards() == null) {
            return Integer.MAX_VALUE;
        }

        return state.buyableCards().getNumAvailable(Card.Type.FRAMEWORK);
    }

    private void updateObservedOpposingAttack(Optional<Event> lastEvent) {
        if (observedOpposingAttack || lastEvent == null || lastEvent.isEmpty()) {
            return;
        }

        Event event = lastEvent.get();
        if (event instanceof GainCardEvent gainEvent
            && isOpposingPlayer(gainEvent.playerName())
            && ATTACK_PLAY_PRIORITY.contains(gainEvent.cardType())) {
            observedOpposingAttack = true;
            return;
        }

        if (event instanceof PlayCardEvent playEvent
            && isOpposingPlayer(playEvent.playerName())
            && ATTACK_PLAY_PRIORITY.contains(playEvent.card().type())) {
            observedOpposingAttack = true;
        }
    }

    private boolean isOpposingPlayer(String playerName) {
        return playerName != null && !playerName.equals(getName());
    }

    private Optional<Decision> findBuyDecisionFor(Card.Type cardType, List<BuyDecision> buyOptions) {
        return buyOptions.stream()
            .filter(decision -> decision.cardType() == cardType)
            .findFirst()
            .map(Decision.class::cast);
    }

    /**
     * Plays +Action action cards first, then action cards that draw the most cards.
     *
     * @param options the legal decisions available this turn
     * @return an optional play-card decision
     */
    private Optional<Decision> chooseActionPlayDecision(ImmutableList<Decision> options) {
        return options.stream()
            .filter(PlayCardDecision.class::isInstance)
            .map(PlayCardDecision.class::cast)
            .filter(decision -> decision.card().category() == Card.Type.Category.ACTION)
            .min(Comparator.comparingInt(this::actionAddsActionsDescendingKey)
                .thenComparingInt(this::actionsAddedDescendingKey)
                .thenComparingInt(this::attackPlayPriorityIndex)
                .thenComparingInt(this::cardsDrawnDescendingKey)
                .thenComparingInt(this::actionCostDescendingKey)
                .thenComparing(this::cardDescription))
            .map(Decision.class::cast);
    }

    /**
     * Plays the highest-value money card offered.
     *
     * @param options the legal decisions available this turn
     * @return an optional play-card decision
     */
    private Optional<Decision> chooseMoneyPlayDecision(ImmutableList<Decision> options) {
        return options.stream()
            .filter(PlayCardDecision.class::isInstance)
            .map(PlayCardDecision.class::cast)
            .filter(decision -> decision.card().category() == Card.Type.Category.MONEY)
            .min(Comparator.comparingInt(this::moneyValueDescendingKey)
                .thenComparing(this::cardDescription))
            .map(Decision.class::cast);
    }

    /**
     * Discards junk for optional filtering, and the least valuable card when discard is forced.
     *
     * @param state the current visible game state
     * @param options the legal decisions available this turn
     * @return an optional discard decision
     */
    private Optional<Decision> chooseDiscardDecision(GameState state, ImmutableList<Decision> options) {
        List<DiscardCardDecision> discardOptions = options.stream()
            .filter(DiscardCardDecision.class::isInstance)
            .map(DiscardCardDecision.class::cast)
            .toList();

        if (discardOptions.isEmpty()) {
            return Optional.empty();
        }

        boolean optionalDiscard = options.stream()
            .filter(EndPhaseDecision.class::isInstance)
            .map(EndPhaseDecision.class::cast)
            .anyMatch(decision -> decision.phase() == GameState.TurnPhase.DISCARD);

        return discardOptions.stream()
            .filter(decision -> !optionalDiscard || shouldDiscardForFiltering(state, decision, discardOptions))
            .min(Comparator.comparingInt((DiscardCardDecision decision) -> discardPriorityKey(state, decision))
                .thenComparing(this::discardCardDescription))
            .map(Decision.class::cast);
    }

    private boolean shouldDiscardForFiltering(GameState state, DiscardCardDecision decision,
                                              List<DiscardCardDecision> discardOptions) {
        Card.Type cardType = decision.card().type();
        if (cardType == Card.Type.BUG) {
            return true;
        }

        if (cardType == Card.Type.BITCOIN) {
            return spendableMoneyAfterDiscard(discardOptions, decision) >= Card.Type.FRAMEWORK.cost();
        }

        return false;
    }

    /**
     * Chooses the first available end-phase decision.
     *
     * @param options the legal decisions available this turn
     * @return an optional end-phase decision
     */
    private Optional<Decision> chooseEndPhaseDecision(ImmutableList<Decision> options) {
        return options.stream()
            .filter(EndPhaseDecision.class::isInstance)
            .findFirst()
            .map(Decision.class::cast);
    }

    private int moneyValueDescendingKey(PlayCardDecision decision) {
        return -decision.card().value();
    }

    private String cardDescription(PlayCardDecision decision) {
        return decision.card().description();
    }

    private int actionAddsActionsDescendingKey(PlayCardDecision decision) {
        return actionsAddedBy(decision.card().type()) > 0 ? -1 : 0;
    }

    private int actionsAddedDescendingKey(PlayCardDecision decision) {
        return -actionsAddedBy(decision.card().type());
    }

    private int cardsDrawnDescendingKey(PlayCardDecision decision) {
        return -cardsDrawnBy(decision.card().type());
    }

    private int attackPlayPriorityIndex(PlayCardDecision decision) {
        int index = ATTACK_PLAY_PRIORITY.indexOf(decision.card().type());
        return index >= 0 ? index : ATTACK_PLAY_PRIORITY.size();
    }

    private int actionCostDescendingKey(PlayCardDecision decision) {
        return -decision.card().cost();
    }

    private int discardPriorityKey(GameState state, DiscardCardDecision decision) {
        Card.Type cardType = decision.card().type();
        return switch (cardType) {
            case BUG -> 0;
            case BITCOIN -> 10;
            case METHOD -> frameworksRemaining(state) <= MODULE_ENDGAME_THRESHOLD ? 80 : 20;
            case MONITORING -> 30;
            case BACKLOG, CODE_REVIEW, SPRINT_PLANNING -> 40;
            case HACK, EVERGREEN_TEST, RANSOMWARE -> 50;
            case ETHEREUM -> 60;
            case MODULE -> 70;
            case DOGECOIN -> 90;
            case FRAMEWORK -> 100;
            default -> 45 + decision.card().value();
        };
    }

    private String discardCardDescription(DiscardCardDecision decision) {
        return decision.card().description();
    }

    private int spendableMoneyAfterDiscard(List<DiscardCardDecision> discardOptions,
                                           DiscardCardDecision discardedDecision) {
        int money = 0;
        for (DiscardCardDecision decision : discardOptions) {
            if (!decision.card().equals(discardedDecision.card())
                && decision.card().category() == Card.Type.Category.MONEY) {
                money += decision.card().value();
            }
        }
        return money;
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

    private Decision finalizeDecision(Decision decision) {
        if (decision instanceof BuyDecision buyDecision) {
            if (ATTACK_BUY_PRIORITY.contains(buyDecision.cardType())) {
                attackBuys++;
            } else if (buyDecision.cardType() == Card.Type.IPO) {
                ipoBuys++;
            } else if (buyDecision.cardType() == Card.Type.CODE_REVIEW) {
                codeReviewBuys++;
            } else if (buyDecision.cardType() == Card.Type.SPRINT_PLANNING) {
                sprintPlanningBuys++;
            } else if (buyDecision.cardType() == Card.Type.MONITORING) {
                monitoringBuys++;
            }
        }
        return decision;
    }

    @Override
    public Optional<GameObserver> getObserver() {
        return Optional.empty();
    }
}
