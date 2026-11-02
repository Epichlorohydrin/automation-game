package edu.brandeis.cosi103a.groupa;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PlayerRatingHarnessTest {

    @Test(expected = IllegalArgumentException.class)
    public void parseSelection_rejectsEmptyInput() {
        PlayerRatingHarness.parseSelection("   ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseSelection_rejectsSinglePlayer() {
        PlayerRatingHarness.parseSelection("1");
    }

    @Test
    public void parseSelection_acceptsMultipleSelections() throws Exception {
        List<?> selected = PlayerRatingHarness.parseSelection("1, 2, 2, 4");

        assertEquals(4, selected.size());
        assertEquals(1, invokeMenuNumber(selected.get(0)));
        assertEquals(2, invokeMenuNumber(selected.get(1)));
        assertEquals(2, invokeMenuNumber(selected.get(2)));
        assertEquals(4, invokeMenuNumber(selected.get(3)));
    }

    @Test
    public void parseSelection_acceptsMoreThanFourPlayers() throws Exception {
        List<?> selected = PlayerRatingHarness.parseSelection("1,2,3,4,1");

        assertEquals(5, selected.size());
    }

    @Test
    public void runHarness_exitsAfterSingleSimulation() {
        Scanner input = new Scanner("1,2\n1\n3\n");
        ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        PrintStream output = new PrintStream(outputBuffer);

        PlayerRatingHarness.runHarness(input, output, new Random(0));

        String outputText = outputBuffer.toString();
        assertTrue(outputText.contains("Automation: Player Rating Harness"));
        assertTrue(outputText.contains("Thanks for using the Player Rating Harness!"));
    }

    @Test
    public void runHarness_printsRatingReport() {
        Scanner input = new Scanner("1,2\n5\n3\n");
        ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        PrintStream output = new PrintStream(outputBuffer);

        PlayerRatingHarness.runHarness(input, output, new Random(0));

        String outputText = outputBuffer.toString();
        assertTrue(outputText.contains("PLAYER RATING HARNESS REPORT"));
        assertTrue(outputText.contains("WinRate"));
        assertTrue(outputText.contains("AvgScore"));
        assertTrue(outputText.contains("Simulations: 5"));
    }

    @Test
    public void runHarness_withMoreThanFourPlayers_includesAllPlayersInReport() {
        Scanner input = new Scanner("1,2,3,4,1\n3\n3\n");
        ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        PrintStream output = new PrintStream(outputBuffer);

        PlayerRatingHarness.runHarness(input, output, new Random(0));

        String outputText = outputBuffer.toString();
        assertTrue(outputText.contains("PLAYER RATING HARNESS REPORT"));
        assertTrue(outputText.contains("Selected players: 5"));
        assertTrue(outputText.contains("Scheduling mode: rotating 4-player pods"));
        // All 5 players should appear in the report with at least some games played
        assertTrue(outputText.contains("Games"));
        assertTrue(outputText.contains("WinRate"));
    }

    private int invokeMenuNumber(Object playerOption) throws Exception {
        Method menuNumberMethod = playerOption.getClass().getDeclaredMethod("menuNumber");
        menuNumberMethod.setAccessible(true);
        return (Integer) menuNumberMethod.invoke(playerOption);
    }
}
