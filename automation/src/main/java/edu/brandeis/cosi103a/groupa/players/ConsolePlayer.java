package edu.brandeis.cosi103a.groupa.players;

import com.google.common.collect.ImmutableList;
import edu.brandeis.cosi.atg.decisions.Decision;
import edu.brandeis.cosi.atg.event.Event;
import edu.brandeis.cosi.atg.event.GameObserver;
import edu.brandeis.cosi.atg.player.Player;
import edu.brandeis.cosi.atg.state.GameState;

import java.util.Optional;
import java.util.Scanner;

public class ConsolePlayer implements Player {

    private final Scanner scanner;

    /**
     * Constructs a console-driven player using standard input.
     */
    public ConsolePlayer() {
        this.scanner = new Scanner(System.in);
    }

    @Override
    public String getName() {
        return "Console Player";
    }

    /**
     * Displays options and returns the selected decision.
     *
     * @param state the current visible game state
     * @param options the legal decisions available this turn
     * @param lastEvent the most recent event, if any
     * @return the selected decision
     */
    @Override
    public Decision makeDecision(GameState state, ImmutableList<Decision> options, Optional<Event> lastEvent) {
        printOptions(options);
        int choice = promptForChoice(options.size());
        return options.get(choice);
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

    /* =======================
       Helper methods
       ======================= */

    private void printOptions(ImmutableList<Decision> options) {
        System.out.println("\nAvailable actions:");
        for (int i = 0; i < options.size(); i++) {
            System.out.println("[" + i + "] " + options.get(i));
        }
    }

    /**
     * Prompts until the user enters a valid option index.
     *
     * @param max exclusive upper bound for valid choices
     * @return the chosen option index
     */
    private int promptForChoice(int max) {
        int choice = -1;

        while (choice < 0 || choice >= max) {
            System.out.print("Choose an option (0-" + (max - 1) + "): ");
            if (scanner.hasNextInt()) {
                choice = scanner.nextInt();
            } else {
                scanner.next(); // discard invalid input
            }
        }

        return choice;
    }

    @Override
    public Optional<GameObserver> getObserver() {
        return Optional.empty();
    }
}
