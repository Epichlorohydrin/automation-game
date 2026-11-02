package edu.brandeis.cosi103a.groupa;

import edu.brandeis.cosi.atg.cards.Card;
import org.junit.Test;

import java.util.EnumSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the official ATG card model used by the current codebase.
 */
public class CardTest {

    @Test
    public void moneyCardTypesExposeExpectedCostAndValue() {
        assertEquals(0, Card.Type.BITCOIN.cost());
        assertEquals(1, Card.Type.BITCOIN.value());
        assertEquals(3, Card.Type.ETHEREUM.cost());
        assertEquals(2, Card.Type.ETHEREUM.value());
        assertEquals(6, Card.Type.DOGECOIN.cost());
        assertEquals(3, Card.Type.DOGECOIN.value());
    }

    @Test
    public void victoryCardTypesExposeExpectedCostAndValue() {
        assertEquals(0, Card.Type.BUG.cost());
        assertEquals(-1, Card.Type.BUG.value());
        assertEquals(2, Card.Type.METHOD.cost());
        assertEquals(1, Card.Type.METHOD.value());
        assertEquals(5, Card.Type.MODULE.cost());
        assertEquals(3, Card.Type.MODULE.value());
        assertEquals(8, Card.Type.FRAMEWORK.cost());
        assertEquals(6, Card.Type.FRAMEWORK.value());
    }

    @Test
    public void newerActionCardTypesExposeExpectedCategoryAndCost() {
        assertEquals(Card.Type.Category.ACTION, Card.Type.BACKLOG.category());
        assertEquals(2, Card.Type.BACKLOG.cost());
        assertEquals(Card.Type.Category.ACTION, Card.Type.DAILY_SCRUM.category());
        assertEquals(5, Card.Type.DAILY_SCRUM.cost());
        assertEquals(Card.Type.Category.ACTION, Card.Type.IPO.category());
        assertEquals(5, Card.Type.IPO.cost());
        assertEquals(Card.Type.Category.ACTION, Card.Type.HACK.category());
        assertEquals(4, Card.Type.HACK.cost());
        assertEquals(Card.Type.Category.ACTION, Card.Type.MONITORING.category());
        assertEquals(2, Card.Type.MONITORING.cost());
        assertEquals(Card.Type.Category.ACTION, Card.Type.TECH_DEBT.category());
        assertEquals(4, Card.Type.TECH_DEBT.cost());
        assertEquals(Card.Type.Category.ACTION, Card.Type.REFACTOR.category());
        assertEquals(4, Card.Type.REFACTOR.cost());
        assertEquals(Card.Type.Category.ACTION, Card.Type.PARALLELIZATION.category());
        assertEquals(4, Card.Type.PARALLELIZATION.cost());
        assertEquals(Card.Type.Category.ACTION, Card.Type.CODE_REVIEW.category());
        assertEquals(3, Card.Type.CODE_REVIEW.cost());
        assertEquals(Card.Type.Category.ACTION, Card.Type.EVERGREEN_TEST.category());
        assertEquals(5, Card.Type.EVERGREEN_TEST.cost());
        assertEquals(Card.Type.Category.ACTION, Card.Type.SPRINT_PLANNING.category());
        assertEquals(3, Card.Type.SPRINT_PLANNING.cost());
        assertEquals(Card.Type.Category.ACTION, Card.Type.MERGE_CONFLICT.category());
        assertEquals(2, Card.Type.MERGE_CONFLICT.cost());
        assertEquals(Card.Type.Category.ACTION, Card.Type.RANSOMWARE.category());
        assertEquals(6, Card.Type.RANSOMWARE.cost());
        assertEquals(Card.Type.Category.ACTION, Card.Type.DEPLOYMENT_PIPELINE.category());
        assertEquals(5, Card.Type.DEPLOYMENT_PIPELINE.cost());
        assertEquals(Card.Type.Category.ACTION, Card.Type.UNIT_TEST.category());
        assertEquals(3, Card.Type.UNIT_TEST.cost());
    }

    @Test
    public void everyCardTypeHasDescriptionAndNonNegativeCostExceptBugPenaltyValue() {
        for (Card.Type type : Card.Type.values()) {
            assertFalse(type.description().trim().isEmpty());
            assertTrue(type.cost() >= 0);
            if (type != Card.Type.BUG) {
                assertTrue(type.value() >= 0);
            }
        }
    }

    @Test
    public void actionCardSetContainsNewestAtgCards() {
        EnumSet<Card.Type> expectedActions = EnumSet.of(
            Card.Type.BACKLOG,
            Card.Type.DAILY_SCRUM,
            Card.Type.IPO,
            Card.Type.HACK,
            Card.Type.MONITORING,
            Card.Type.TECH_DEBT,
            Card.Type.REFACTOR,
            Card.Type.PARALLELIZATION,
            Card.Type.CODE_REVIEW,
            Card.Type.EVERGREEN_TEST,
            Card.Type.SPRINT_PLANNING,
            Card.Type.MERGE_CONFLICT,
            Card.Type.RANSOMWARE,
            Card.Type.DEPLOYMENT_PIPELINE,
            Card.Type.UNIT_TEST
        );

        for (Card.Type type : expectedActions) {
            assertEquals(Card.Type.Category.ACTION, type.category());
        }
        assertEquals(15, expectedActions.size());
    }

    @Test
    public void cardDelegatesPropertiesToItsType() {
        Card framework = new Card(Card.Type.FRAMEWORK, 99);
        Card unitTest = new Card(Card.Type.UNIT_TEST, 123);

        assertEquals(Card.Type.Category.VICTORY, framework.category());
        assertEquals(8, framework.cost());
        assertEquals(6, framework.value());
        assertEquals("Framework", framework.description());

        assertEquals(Card.Type.Category.ACTION, unitTest.category());
        assertEquals(3, unitTest.cost());
        assertEquals(0, unitTest.value());
        assertEquals("Unit Test", unitTest.description());
    }

    @Test
    public void cardsUseTypeAndIdForEquality() {
        Card first = new Card(Card.Type.BITCOIN, 1);
        Card same = new Card(Card.Type.BITCOIN, 1);
        Card differentId = new Card(Card.Type.BITCOIN, 2);
        Card differentType = new Card(Card.Type.ETHEREUM, 1);

        assertEquals(first, same);
        assertEquals(first.hashCode(), same.hashCode());
        assertNotEquals(first, differentId);
        assertNotEquals(first, differentType);
    }
}
