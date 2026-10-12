package com.example.library.service.plan;

import org.junit.jupiter.api.Test;

import static com.example.library.service.plan.FsrsCalculator.Rating.AGAIN;
import static com.example.library.service.plan.FsrsCalculator.Rating.EASY;
import static com.example.library.service.plan.FsrsCalculator.Rating.GOOD;
import static com.example.library.service.plan.FsrsCalculator.Rating.HARD;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FsrsCalculatorTest {

    @Test
    void fromScore_mapsToFourRatings() {
        assertEquals(EASY, FsrsCalculator.fromScore(100));
        assertEquals(EASY, FsrsCalculator.fromScore(90));
        assertEquals(GOOD, FsrsCalculator.fromScore(89));
        assertEquals(GOOD, FsrsCalculator.fromScore(75));
        assertEquals(HARD, FsrsCalculator.fromScore(74));
        assertEquals(HARD, FsrsCalculator.fromScore(60));
        assertEquals(AGAIN, FsrsCalculator.fromScore(59));
        assertEquals(AGAIN, FsrsCalculator.fromScore(0));
    }

    @Test
    void nextInterval_againAlwaysOneDay() {
        assertEquals(1, FsrsCalculator.nextInterval(AGAIN, 0));
        assertEquals(1, FsrsCalculator.nextInterval(AGAIN, 3));
    }

    @Test
    void nextInterval_hard() {
        assertEquals(2, FsrsCalculator.nextInterval(HARD, 0));
        assertEquals(4, FsrsCalculator.nextInterval(HARD, 1));
        assertEquals(4, FsrsCalculator.nextInterval(HARD, 5));
    }

    @Test
    void nextInterval_good_follows_1_3_7() {
        assertEquals(1, FsrsCalculator.nextInterval(GOOD, 0));
        assertEquals(3, FsrsCalculator.nextInterval(GOOD, 1));
        assertEquals(7, FsrsCalculator.nextInterval(GOOD, 2));
        assertEquals(7, FsrsCalculator.nextInterval(GOOD, 5));
    }

    @Test
    void nextInterval_easy_follows_3_7_15() {
        assertEquals(3, FsrsCalculator.nextInterval(EASY, 0));
        assertEquals(7, FsrsCalculator.nextInterval(EASY, 1));
        assertEquals(15, FsrsCalculator.nextInterval(EASY, 2));
        assertEquals(15, FsrsCalculator.nextInterval(EASY, 5));
    }
}
