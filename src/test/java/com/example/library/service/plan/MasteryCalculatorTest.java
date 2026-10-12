package com.example.library.service.plan;

import org.junit.jupiter.api.Test;

import static com.example.library.service.plan.MasteryCalculator.State.CONSOLIDATING;
import static com.example.library.service.plan.MasteryCalculator.State.DUE;
import static com.example.library.service.plan.MasteryCalculator.State.LEARNING;
import static com.example.library.service.plan.MasteryCalculator.State.MASTERED;
import static com.example.library.service.plan.MasteryCalculator.State.NOT_STARTED;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MasteryCalculatorTest {

    @Test
    void onLearnDone_addsTenCappedAt60() {
        assertEquals(10, MasteryCalculator.onLearnDone(0));
        assertEquals(60, MasteryCalculator.onLearnDone(55));
        assertEquals(60, MasteryCalculator.onLearnDone(59));
        assertEquals(60, MasteryCalculator.onLearnDone(60));
    }

    @Test
    void onReview_addsFive() {
        assertEquals(5, MasteryCalculator.onReview(0));
        assertEquals(75, MasteryCalculator.onReview(70));
    }

    @Test
    void onPass_addsFifteen() {
        assertEquals(15, MasteryCalculator.onPass(0));
        assertEquals(85, MasteryCalculator.onPass(70));
    }

    @Test
    void onFail_subtractsTwentyFloorZero() {
        assertEquals(0, MasteryCalculator.onFail(0));
        assertEquals(0, MasteryCalculator.onFail(15));
        assertEquals(10, MasteryCalculator.onFail(30));
    }

    @Test
    void onIdle3Days_subtractsThreeFloorZero() {
        assertEquals(0, MasteryCalculator.onIdle3Days(0));
        assertEquals(2, MasteryCalculator.onIdle3Days(5));
        assertEquals(0, MasteryCalculator.onIdle3Days(2));
    }

    @Test
    void state_mastered_whenScoreAtLeast80() {
        assertEquals(MASTERED, MasteryCalculator.state(80, false, true, true));
        assertEquals(MASTERED, MasteryCalculator.state(85, true, true, true));
    }

    @Test
    void state_notStarted() {
        assertEquals(NOT_STARTED, MasteryCalculator.state(0, false, false, false));
    }

    @Test
    void state_due_whenScheduledBeforeToday() {
        assertEquals(DUE, MasteryCalculator.state(30, true, true, false));
    }

    @Test
    void state_consolidating() {
        assertEquals(CONSOLIDATING, MasteryCalculator.state(45, false, true, true));
    }

    @Test
    void state_learning() {
        assertEquals(LEARNING, MasteryCalculator.state(10, false, true, false));
        assertEquals(LEARNING, MasteryCalculator.state(45, false, true, false));
    }
}
