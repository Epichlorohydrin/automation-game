package edu.brandeis.cosi103a.groupa;

import com.google.common.collect.ImmutableList;
import edu.brandeis.cosi.atg.decisions.Decision;
import edu.brandeis.cosi.atg.engine.PlayerViolationException;
import edu.brandeis.cosi.atg.event.Event;
import edu.brandeis.cosi.atg.event.GameObserver;
import edu.brandeis.cosi.atg.player.Player;
import edu.brandeis.cosi.atg.state.GameResult;
import edu.brandeis.cosi.atg.state.GameState;
import edu.brandeis.cosi.atg.state.PlayerResult;
import edu.brandeis.cosi103a.groupa.players.BalancedAutoPlayer;
import edu.brandeis.cosi103a.groupa.players.BigMoneyAdv1Player;
import edu.brandeis.cosi103a.groupa.players.BigMoneyPlayerNew;
import edu.brandeis.cosi103a.groupa.players.PriorityAutoplayer;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Scanner;
import java.util.function.Supplier;

/**
 * Console harness that compares automated players across many simulations.
 */
public final class PlayerRatingHarness {

    static final int DEFAULT_SIMULATIONS = 1000;
    private static final int MAX_PLAYERS_PER_GAME = 4;

    private static final List<PlayerOption> OPTIONS = List.of(
        new PlayerOption(1, "PriorityAutoplayer", "Fixed priority strategy", PriorityAutoplayer::new),
        new PlayerOption(2, "BigMoneyAdv1", "Baseline big money strategy", BigMoneyAdv1Player::new),
        new PlayerOption(3, "BalancedAutoPlayer", "Balanced money, action, and point strategy", BalancedAutoPlayer::new),
        new PlayerOption(4, "bigmoneyplayernew", "Big money strategy with early Monitoring defense", BigMoneyPlayerNew::new)
    );

    private PlayerRatingHarness() {
    }

    /**
     * Starts the rating harness using standard input, output, and a fresh random source.
     *
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {
        runHarness(new Scanner(System.in), System.out, new Random());
    }

    /**
     * Runs the harness with caller-provided input and output streams.
     *
     * @param input scanner used for interactive prompts
     * @param output stream used for status and report output
     */
    public static void runHarness(Scanner input, PrintStream output) {
        runHarness(input, output, new Random());
    }

    /**
     * Runs the complete interactive simulation loop for player rating.
     *
     * @param input scanner used for interactive prompts
     * @param output stream used for status and report output
     * @param rng random source used for action-card shuffling
     */
    static void runHarness(Scanner input, PrintStream output, Random rng) {
        output.println("Automation: Player Rating Harness");
        output.println();

        List<PlayerOption> selected = promptSelectedPlayers(input, output);
        List<PlayerSeat> selectedSeats = buildPlayerSeats(selected);

        Map<String, PlayerStats> stats = new HashMap<>();
        for (PlayerSeat seat : selectedSeats) {
            stats.put(seat.displayName(), new PlayerStats(seat.displayName()));
        }

        List<GameOutcome> allGameOutcomes = new ArrayList<>();
        int totalGames = 0;

        while (true) {
            int simulations = promptSimulationCount(input, output);
            totalGames += simulations;

            output.println();
            output.println("Running " + simulations + " simulated games...");

            for (int gameIndex = 0; gameIndex < simulations; gameIndex++) {
                List<PlayerSeat> pod = choosePod(selectedSeats, gameIndex);
                List<Player> players = new ArrayList<>();
                for (PlayerSeat seat : pod) {
                    players.add(seat.factory().get());
                }

                GameEngine engine = new GameEngine(players, ActionCards.chooseActionCards(rng));
                try {
                    GameResult result = engine.play();
                    GameOutcome outcome = recordGameOutcome(result, totalGames - simulations + gameIndex + 1);
                    allGameOutcomes.add(outcome);
                    updateStats(result, stats);
                } catch (PlayerViolationException e) {
                    output.println("Simulation failed on game " + (totalGames - simulations + gameIndex + 1) + ": " + e.getMessage());
                    return;
                }
            }

            printReport(selectedSeats, totalGames, stats, output);

            String nextAction = promptNextAction(input, output);
            if (nextAction.equals("more")) {
                output.println();
                continue;
            } else if (nextAction.equals("detail")) {
                printDetailedReport(allGameOutcomes, output);
                nextAction = promptNextAction(input, output);
                if (nextAction.equals("more")) {
                    output.println();
                    continue;
                } else {
                    output.println("Thanks for using the Player Rating Harness!");
                    return;
                }
            } else {
                output.println("Thanks for using the Player Rating Harness!");
                return;
            }
        }
    }

    /**
     * Prompts for and validates the selected automated players.
     *
     * @param input scanner used for interactive prompts
     * @param output stream used for prompt and validation messages
     * @return selected player options in seat order
     */
    private static List<PlayerOption> promptSelectedPlayers(Scanner input, PrintStream output) {
        while (true) {
            output.println("Available automated players:");
            for (PlayerOption option : OPTIONS) {
                output.println("  [" + option.menuNumber() + "] " + option.displayName() + " - " + option.description());
            }
            output.println("Repeat a menu number to include multiple copies of that player.");
            output.print("Select 2+ players (comma-separated numbers, e.g. 1,2,2,2): ");
            output.flush();

            String line = input.nextLine();
            try {
                List<PlayerOption> selected = parseSelection(line);
                if (selected.size() < 2) {
                    output.println("Please select at least 2 players.");
                    output.println();
                    continue;
                }
                return selected;
            } catch (IllegalArgumentException e) {
                output.println("Invalid selection: " + e.getMessage());
                output.println();
            }
        }
    }

    /**
     * Parses a comma-separated player menu selection string.
     *
     * @param line raw menu input
     * @return immutable list of selected player options
     */
    static List<PlayerOption> parseSelection(String line) {
        if (line == null || line.isBlank()) {
            throw new IllegalArgumentException("selection cannot be empty");
        }

        Map<Integer, PlayerOption> byNumber = new HashMap<>();
        for (PlayerOption option : OPTIONS) {
            byNumber.put(option.menuNumber(), option);
        }

        String[] pieces = line.split(",");
        List<PlayerOption> selected = new ArrayList<>();
        for (String piece : pieces) {
            String trimmed = piece.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            int number;
            try {
                number = Integer.parseInt(trimmed);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("not a number: " + trimmed);
            }

            PlayerOption option = byNumber.get(number);
            if (option == null) {
                throw new IllegalArgumentException("out of range: " + number);
            }
            selected.add(option);
        }

        if (selected.isEmpty()) {
            throw new IllegalArgumentException("no player selected");
        }

        if (selected.size() < 2) {
            throw new IllegalArgumentException("must select at least 2 players");
        }

        return List.copyOf(selected);
    }

    /**
     * Prompts for the number of simulations to run in the next batch.
     *
     * @param input scanner used for interactive prompts
     * @param output stream used for prompt and validation messages
     * @return validated positive simulation count
     */
    private static int promptSimulationCount(Scanner input, PrintStream output) {
        while (true) {
            output.print("Simulation count [default " + DEFAULT_SIMULATIONS + "]: ");
            output.flush();
            String line = input.nextLine().trim();
            if (line.isEmpty()) {
                return DEFAULT_SIMULATIONS;
            }

            try {
                int parsed = Integer.parseInt(line);
                if (parsed <= 0) {
                    output.println("Simulation count must be positive.");
                    continue;
                }
                return parsed;
            } catch (NumberFormatException e) {
                output.println("Please enter a positive integer.");
            }
        }
    }

    /**
     * Selects the players participating in a single game.
     *
     * @param selectedPlayers all selected seats
     * @param gameIndex zero-based game index
     * @return immutable pod of seats for the game
     */
    private static List<PlayerSeat> choosePod(List<PlayerSeat> selectedPlayers, int gameIndex) {
        if (selectedPlayers.size() <= MAX_PLAYERS_PER_GAME) {
            return selectedPlayers;
        }

        List<PlayerSeat> pod = new ArrayList<>();
        for (int offset = 0; offset < MAX_PLAYERS_PER_GAME; offset++) {
            int index = (gameIndex + offset) % selectedPlayers.size();
            pod.add(selectedPlayers.get(index));
        }
        return List.copyOf(pod);
    }

    /**
     * Prompts for the next top-level harness action.
     *
     * @param input scanner used for interactive prompts
     * @param output stream used for prompt and validation messages
     * @return one of more, detail, or exit
     */
    private static String promptNextAction(Scanner input, PrintStream output) {
        while (true) {
            output.println();
            output.println("What would you like to do?");
            output.println("  [1] Run more games");
            output.println("  [2] Show detailed report (all game outcomes)");
            output.println("  [3] Exit");
            output.print("Enter choice (1-3): ");
            output.flush();

            String line = input.nextLine().trim();
            if (line.equals("1")) {
                return "more";
            } else if (line.equals("2")) {
                return "detail";
            } else if (line.equals("3")) {
                return "exit";
            } else {
                output.println("Invalid choice. Please enter 1, 2, or 3.");
            }
        }
    }

    /**
     * Converts one game result into a report-friendly outcome record.
     *
     * @param result completed game result
     * @param gameNumber one-based game sequence number
     * @return recorded outcome with ranked player entries
     */
    private static GameOutcome recordGameOutcome(GameResult result, int gameNumber) {
        List<GameOutcome.PlayerOutcome> playerOutcomes = new ArrayList<>();
        int rank = 1;
        for (PlayerResult playerResult : result.playerResults()) {
            playerOutcomes.add(new GameOutcome.PlayerOutcome(
                playerResult.playerName(),
                rank,
                playerResult.score()
            ));
            rank++;
        }
        return new GameOutcome(gameNumber, playerOutcomes);
    }

    /**
     * Prints detailed per-game outcomes for all simulations run so far.
     *
     * @param outcomes ordered list of recorded game outcomes
     * @param output stream used for report output
     */
    private static void printDetailedReport(List<GameOutcome> outcomes, PrintStream output) {
        output.println();
        output.println("============================================");
        output.println("      DETAILED GAME REPORT");
        output.println("============================================");
        output.println("Total games: " + outcomes.size());
        output.println();

        for (GameOutcome outcome : outcomes) {
            output.println("Game " + outcome.gameNumber + ":");
            for (GameOutcome.PlayerOutcome po : outcome.playerOutcomes) {
                output.println(String.format("  %d. %-20s %6d points",
                    po.rank,
                    po.playerName,
                    po.score));
            }
            output.println();
        }
    }

    /**
     * Expands selected player options into named seats for simulation.
     *
     * @param selectedOptions selected player option entries
     * @return immutable ordered seat list
     */
    private static List<PlayerSeat> buildPlayerSeats(List<PlayerOption> selectedOptions) {
        Map<String, Integer> totalCounts = new HashMap<>();
        for (PlayerOption option : selectedOptions) {
            totalCounts.merge(option.displayName(), 1, Integer::sum);
        }

        Map<String, Integer> occurrenceCounts = new HashMap<>();
        List<PlayerSeat> seats = new ArrayList<>();
        for (PlayerOption option : selectedOptions) {
            int totalForName = totalCounts.get(option.displayName());
            int occurrence = occurrenceCounts.merge(option.displayName(), 1, Integer::sum);
            String seatName = totalForName > 1 ? option.displayName() + "#" + occurrence : option.displayName();
            seats.add(new PlayerSeat(seatName, option.description(), () -> new NamedPlayer(seatName, option.factory().get())));
        }

        return List.copyOf(seats);
    }

    /**
     * Accumulates game results into aggregate player statistics.
     *
     * @param result completed game result
     * @param statsByName stats map keyed by seat display name
     */
    private static void updateStats(GameResult result, Map<String, PlayerStats> statsByName) {
        int rank = 1;
        for (PlayerResult playerResult : result.playerResults()) {
            PlayerStats stats = statsByName.get(playerResult.playerName());
            if (stats == null) {
                throw new IllegalStateException("Unknown player in results: " + playerResult.playerName());
            }
            stats.gamesPlayed++;
            stats.totalScore += playerResult.score();
            if (rank == 1) {
                stats.wins++;
            }
            rank++;
        }
    }

    /**
     * Prints the current leaderboard report.
     *
     * @param selected selected seats for the harness
     * @param simulations total simulations completed
     * @param statsByName aggregate stats keyed by seat display name
     * @param output stream used for report output
     */
    private static void printReport(List<PlayerSeat> selected,
                                    int simulations,
                                    Map<String, PlayerStats> statsByName,
                                    PrintStream output) {
        output.println();
        output.println("===========================================");
        output.println("      PLAYER RATING HARNESS REPORT");
        output.println("===========================================");
        output.println("Selected players: " + selected.size());
        output.println("Simulations: " + simulations);
        output.println(selected.size() <= MAX_PLAYERS_PER_GAME
            ? "Scheduling mode: all selected players in every game"
            : "Scheduling mode: rotating 4-player pods");

        output.println();
        output.println("Selected automated players:");
        for (PlayerSeat seat : selected) {
            output.println("  - " + seat.displayName());
        }

        List<PlayerStats> ranking = new ArrayList<>();
        for (PlayerSeat seat : selected) {
            ranking.add(statsByName.get(seat.displayName()));
        }

        ranking.sort(Comparator
            .comparingDouble(PlayerStats::winRate).reversed()
            .thenComparingDouble(PlayerStats::averageScore).reversed()
            .thenComparing(PlayerStats::name));

        output.println();
        output.println(String.format("%-3s %-20s %7s %7s %10s %10s",
            "#", "Player", "Games", "Wins", "WinRate", "AvgScore"));

        int rank = 1;
        for (PlayerStats stats : ranking) {
            output.println(String.format("%-3d %-20s %7d %7d %9.2f%% %10.2f",
                rank,
                stats.name(),
                stats.gamesPlayed,
                stats.wins,
                stats.winRate() * 100.0,
                stats.averageScore()));
            rank++;
        }
    }

    /**
     * Mutable aggregate statistics for a single player seat.
     */
    static final class PlayerStats {
        private final String name;
        private int gamesPlayed;
        private int wins;
        private int totalScore;

        /**
         * Creates an empty stats accumulator for a player seat.
         *
         * @param name display name used in reports
         */
        private PlayerStats(String name) {
            this.name = name;
        }

        /**
         * Gets the display name for this stats entry.
         *
         * @return player display name
         */
        String name() {
            return name;
        }

        /**
         * Computes win rate over all recorded games.
         *
         * @return win rate in range 0.0 to 1.0
         */
        double winRate() {
            if (gamesPlayed == 0) {
                return 0.0;
            }
            return (double) wins / gamesPlayed;
        }

        /**
         * Computes average score over all recorded games.
         *
         * @return mean score value
         */
        double averageScore() {
            if (gamesPlayed == 0) {
                return 0.0;
            }
            return (double) totalScore / gamesPlayed;
        }
    }

    /**
     * Immutable menu option metadata for a player strategy.
     */
    static final class PlayerOption {
        private final int menuNumber;
        private final String displayName;
        private final String description;
        private final Supplier<Player> factory;

        /**
         * Creates a player menu option.
         *
         * @param menuNumber numeric menu key
         * @param displayName user-facing player name
         * @param description short strategy description
         * @param factory supplier for creating a fresh player instance
         */
        private PlayerOption(int menuNumber, String displayName, String description, Supplier<Player> factory) {
            this.menuNumber = menuNumber;
            this.displayName = displayName;
            this.description = description;
            this.factory = factory;
        }

        /**
         * Gets the numeric menu key.
         *
         * @return menu number
         */
        int menuNumber() {
            return menuNumber;
        }

        /**
         * Gets the display name used in prompts and reports.
         *
         * @return player display name
         */
        String displayName() {
            return displayName;
        }

        /**
         * Gets the short strategy description.
         *
         * @return menu description text
         */
        String description() {
            return description;
        }

        /**
         * Gets the factory used to instantiate player instances.
         *
         * @return player supplier
         */
        Supplier<Player> factory() {
            return factory;
        }
    }

    /**
     * Immutable configured seat entry used by the scheduler.
     */
    static final class PlayerSeat {
        private final String displayName;
        private final String description;
        private final Supplier<Player> factory;

        /**
         * Creates a player seat with naming and construction metadata.
         *
         * @param displayName seat display name
         * @param description seat strategy description
         * @param factory supplier for creating fresh player instances
         */
        private PlayerSeat(String displayName, String description, Supplier<Player> factory) {
            this.displayName = displayName;
            this.description = description;
            this.factory = factory;
        }

        /**
         * Gets the seat display name.
         *
         * @return seat display name
         */
        String displayName() {
            return displayName;
        }

        /**
         * Gets the seat strategy description.
         *
         * @return description text
         */
        String description() {
            return description;
        }

        /**
         * Gets the player factory for this seat.
         *
         * @return player supplier
         */
        Supplier<Player> factory() {
            return factory;
        }
    }

    /**
     * Player wrapper that overrides displayed name while delegating behavior.
     */
    static final class NamedPlayer implements edu.brandeis.cosi.atg.player.Player {
        private final String name;
        private final edu.brandeis.cosi.atg.player.Player delegate;

        /**
         * Creates a named player wrapper.
         *
         * @param name display name to expose
         * @param delegate underlying player implementation
         */
        private NamedPlayer(String name, edu.brandeis.cosi.atg.player.Player delegate) {
            this.name = name;
            this.delegate = delegate;
        }

        /**
         * Returns the configured display name for this wrapped player.
         *
         * @return player display name
         */
        @Override
        public String getName() {
            return name;
        }

        /**
         * Delegates decision-making to the wrapped player.
         *
         * @param state current visible game state
         * @param options legal decisions to choose from
         * @param lastEvent most recent game event, if any
         * @return selected decision from the wrapped player
         */
        @Override
        public Decision makeDecision(GameState state, ImmutableList<Decision> options, Optional<Event> lastEvent) {
            return delegate.makeDecision(state, options, lastEvent);
        }

        /**
         * Delegates observer lookup to the wrapped player.
         *
         * @return optional game observer
         */
        @Override
        public Optional<GameObserver> getObserver() {
            return delegate.getObserver();
        }
    }

    /**
     * Immutable container for one completed game outcome.
     */
    static final class GameOutcome {
        final int gameNumber;
        final List<PlayerOutcome> playerOutcomes;

        /**
         * Creates an immutable game outcome record.
         *
         * @param gameNumber one-based game sequence number
         * @param playerOutcomes ranked player results for the game
         */
        GameOutcome(int gameNumber, List<PlayerOutcome> playerOutcomes) {
            this.gameNumber = gameNumber;
            this.playerOutcomes = List.copyOf(playerOutcomes);
        }

        /**
         * Immutable player result row for a game outcome.
         */
        static final class PlayerOutcome {
            final String playerName;
            final int rank;
            final int score;

            /**
             * Creates one player result row in a game outcome.
             *
             * @param playerName player display name
             * @param rank finishing rank in the game
             * @param score final score for the player
             */
            PlayerOutcome(String playerName, int rank, int score) {
                this.playerName = playerName;
                this.rank = rank;
                this.score = score;
            }
        }
    }

}
