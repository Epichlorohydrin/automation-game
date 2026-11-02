package edu.brandeis.cosi103a.groupa;

import edu.brandeis.cosi.atg.engine.PlayerViolationException;
import edu.brandeis.cosi.atg.player.Player;
import edu.brandeis.cosi.atg.state.GameResult;
import edu.brandeis.cosi.atg.state.PlayerResult;
import edu.brandeis.cosi103a.groupa.players.BigMoneyAdv1Player;
import edu.brandeis.cosi103a.groupa.players.PriorityAutoplayer;

import java.util.List;
import java.util.Random;

/**
 * Entry point for running a local game between automated players.
 */
public final class App {

    /**
     * Initializes players, runs one game, and prints final results.
     *
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {
        Random rng = new Random();

        List<Player> players = List.of(new PriorityAutoplayer(), new BigMoneyAdv1Player());
        GameEngine engine = new GameEngine(players, ActionCards.chooseActionCards(rng));
        engine.setObserver(new ConsoleGameObserver());

        try {
            GameResult result = engine.play();
            System.out.println("Game Over!");
            for (PlayerResult playerResult : result.playerResults()) {
                System.out.println(playerResult.playerName() + ": " + playerResult.score());
            }
            System.out.println("Winner: " + result.playerResults().get(0).playerName());
        } catch (PlayerViolationException e) {
            System.out.println("Game failed: " + e.getMessage());
        }
    }
}
