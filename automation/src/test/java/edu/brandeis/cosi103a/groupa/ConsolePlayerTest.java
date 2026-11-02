package edu.brandeis.cosi103a.groupa;

import com.google.common.collect.ImmutableList;
import edu.brandeis.cosi.atg.decisions.Decision;
import edu.brandeis.cosi.atg.decisions.EndPhaseDecision;
import edu.brandeis.cosi.atg.state.GameState;
import edu.brandeis.cosi103a.groupa.players.ConsolePlayer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.Assert.assertSame;

public class ConsolePlayerTest {

    private InputStream originalIn;

    @Before
    public void setUp() {
        originalIn = System.in;
    }

    @After
    public void tearDown() {
        System.setIn(originalIn);
    }

    @Test
    public void makeDecision_returnsSecondOption_whenInputIs1() {
        System.setIn(new ByteArrayInputStream("1\n".getBytes()));

        ConsolePlayer player = new ConsolePlayer();

        Decision d0 = new EndPhaseDecision(GameState.TurnPhase.MONEY);
        Decision d1 = new EndPhaseDecision(GameState.TurnPhase.BUY);
        ImmutableList<Decision> options = ImmutableList.of(d0, d1);

        // state is unused by your ConsolePlayer, so null is fine
        Decision chosen = player.makeDecision(null, options);

        assertSame(d1, chosen);
    }

    @Test
    public void makeDecision_repromptsUntilValidIndex() {
        // "abc" (not int) -> discarded
        // "5"   (out of range) -> reprompt
        // "0"   (valid)
        System.setIn(new ByteArrayInputStream("abc\n5\n0\n".getBytes()));

        ConsolePlayer player = new ConsolePlayer();

        Decision d0 = new EndPhaseDecision(GameState.TurnPhase.MONEY);
        Decision d1 = new EndPhaseDecision(GameState.TurnPhase.BUY);
        ImmutableList<Decision> options = ImmutableList.of(d0, d1);

        Decision chosen = player.makeDecision(null, options);

        assertSame(d0, chosen);
    }
}
