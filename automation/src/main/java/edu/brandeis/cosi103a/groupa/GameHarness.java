package edu.brandeis.cosi103a.groupa;

import edu.brandeis.cosi.atg.engine.PlayerViolationException;
import edu.brandeis.cosi.atg.player.Player;
import edu.brandeis.cosi.atg.state.GameResult;
import edu.brandeis.cosi.atg.state.PlayerResult;
import edu.brandeis.cosi103a.groupa.players.ConsolePlayer;
import edu.brandeis.cosi103a.groupa.players.PriorityAutoplayer;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

/**
 * Main harness for running games of Automation: The Game.
 *
 * <p>Provides a console-based interface that:
 * <ul>
 *   <li>Gets user input for game configuration</li>
 *   <li>Supports all-automated, all-human, or mixed player games</li>
 *   <li>Prints final game results</li>
 * </ul>
 */
public final class GameHarness {

    private static final Random RNG = new Random();

    /**
     * Runs the game harness.
     *
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {
        runGame(new Scanner(System.in), System.out);
    }

    /**
     * Runs a complete game with the given input and output streams.
     * This method is extracted for testability.
     *
     * @param input the input scanner
     * @param output the output print stream
     */
    /**
     * Collects configuration, runs a game, and prints results.
     *
     * @param input scanner used for user input
     * @param output stream used for console output
     */
    public static void runGame(Scanner input, PrintStream output) {
        output.println("Welcome to Automation: The Game!");
        output.println();

        GameConfig config = getUserConfiguration(input, output);
        List<Player> players = new ArrayList<>();
        for (int i = 0; i < config.numPlayers; i++) {
            players.add(config.playerTypes[i] == PlayerType.AUTOMATED
                ? new PriorityAutoplayer()
                : new ConsolePlayer());
        }

        GameEngine engine = new GameEngine(players, ActionCards.chooseActionCards(RNG));
        engine.setObserver(new ConsoleGameObserver());
        GameResult result;
        try {
            result = engine.play();
        } catch (PlayerViolationException e) {
            output.println("Game failed: " + e.getMessage());
            return;
        }

        output.println();
        output.println("=====================================");
        output.println("           GAME RESULTS              ");
        output.println("=====================================");
        printGameResults(result, output);
    }

    /**
     * Prompts for player configuration and builds a game config.
     *
     * @param input scanner used for user input
     * @param output stream used for console output
     * @return the selected game configuration
     */
    private static GameConfig getUserConfiguration(Scanner input, PrintStream output) {
        output.println("Game Configuration:");
        output.println("------------------");

        int numPlayers = 2;
        output.println("Number of players: " + numPlayers);

        PlayerType[] playerTypes = new PlayerType[numPlayers];

        for (int i = 0; i < numPlayers; i++) {
            playerTypes[i] = getPlayerType(input, output, i + 1);
        }

        output.println();
        output.println("Game starting...");
        output.println();

        return new GameConfig(numPlayers, playerTypes);
    }

    /**
     * Prompts for a single player's type until valid input is provided.
     *
     * @param input scanner used for user input
     * @param output stream used for console output
     * @param playerNumber one-based player number shown in prompts
     * @return the selected player type
     */
    private static PlayerType getPlayerType(Scanner input, PrintStream output, int playerNumber) {
        output.println("Player " + playerNumber + " type:");
        output.println("  [A] Automated");
        output.println("  [H] Human");
        output.print("Choose [A/H]: ");
        output.flush();

        String choice = input.nextLine().trim().toUpperCase();

        while (!choice.equals("A") && !choice.equals("H")) {
            output.print("Invalid choice. Please enter A or H: ");
            output.flush();
            choice = input.nextLine().trim().toUpperCase();
        }

        return choice.equals("A") ? PlayerType.AUTOMATED : PlayerType.HUMAN;
    }

    /**
     * Prints player scores and the winner.
     *
     * @param result completed game result
     * @param output stream used for console output
     */
    private static void printGameResults(GameResult result, PrintStream output) {
        output.println();
        output.println("Final Scores:");
        for (PlayerResult playerResult : result.playerResults()) {
            output.println("  " + playerResult.playerName() + ": " + playerResult.score() + " Automation Points");
        }
        output.println();
        output.println("Winner: " + result.playerResults().get(0).playerName());
        output.println();
        output.println("Thanks for playing!");
    }

    private static class GameConfig {
        final int numPlayers;
        final PlayerType[] playerTypes;

        GameConfig(int numPlayers, PlayerType[] playerTypes) {
            this.numPlayers = numPlayers;
            this.playerTypes = playerTypes;
        }
    }

    private enum PlayerType {
        AUTOMATED,
        HUMAN
    }
}