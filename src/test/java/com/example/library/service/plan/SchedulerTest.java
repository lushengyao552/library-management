package com.example.library.service.plan;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchedulerTest {

    private final Scheduler scheduler = new Scheduler();

    private Scheduler.PointInput pt(String title, double weight, double estHours, boolean mustLearn, String... prereq) {
        return new Scheduler.PointInput(title, title + " 摘要", weight, estHours, mustLearn, List.of(prereq));
    }

    @Test
    void emptyPoints_returnsWarning() {
        Scheduler.Result r = scheduler.schedule(List.of(), 2, LocalDate.of(2026, 1, 1), null, null);
        assertTrue(r.points().isEmpty());
        assertTrue(r.warnings().stream().anyMatch(w -> w.contains("没有可排的学习点")));
    }

    @Test
    void endDate_schedulesWithinLearnDays_reservesLast20Percent() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 10); // 10 天 → 最后 2 天复习
        List<Scheduler.PointInput> pts = List.of(
                pt("A", 2.0, 1, true),
                pt("B", 2.0, 1, true),
                pt("C", 2.0, 1, true));
        Scheduler.Result r = scheduler.schedule(pts, 2, start, end, null);
        long learnDays = 8;
        for (Scheduler.ScheduledPoint p : r.points()) {
            assertFalse(p.date().isAfter(start.plusDays(learnDays - 1)),
                    "学习点不应排进复习冲刺段: " + p.title());
        }
    }

    @Test
    void topologicalOrder_placesPrereqFirst() {
        List<Scheduler.PointInput> pts = List.of(
                pt("C", 2.0, 1, true, "B"),
                pt("B", 2.0, 1, true, "A"),
                pt("A", 2.0, 1, true));
        Scheduler.Result r = scheduler.schedule(pts, 3, LocalDate.of(2026, 1, 1), null, null);
        List<String> titles = r.points().stream().map(Scheduler.ScheduledPoint::title).toList();
        assertTrue(titles.indexOf("A") < titles.indexOf("B"));
        assertTrue(titles.indexOf("B") < titles.indexOf("C"));
    }

    @Test
    void dailyLoad_neverExceedsDailyHours() {
        List<Scheduler.PointInput> pts = List.of(
                pt("A", 2.0, 0.5, true),
                pt("B", 2.0, 0.5, true),
                pt("C", 2.0, 0.5, true),
                pt("D", 2.0, 0.5, true));
        double dailyHours = 1.0;
        Scheduler.Result r = scheduler.schedule(pts, dailyHours, LocalDate.of(2026, 1, 1), null, null);
        Map<LocalDate, Double> load = r.points().stream()
                .collect(Collectors.groupingBy(Scheduler.ScheduledPoint::date,
                        Collectors.summingDouble(Scheduler.ScheduledPoint::estHours)));
        for (double h : load.values()) {
            assertTrue(h <= dailyHours + 0.01, "某天负荷超上限: " + h);
        }
    }

    @Test
    void tightTime_compressesLowWeightFirst() {
        // A 权重 1.0（低）、B 权重 2.0（高），时间不足时只压缩低权重的 A
        List<Scheduler.PointInput> pts = List.of(
                pt("A", 1.0, 2, true),
                pt("B", 2.0, 2, true));
        Scheduler.Result r = scheduler.schedule(pts, 0.5, LocalDate.of(2026, 1, 1), null, null);
        Scheduler.ScheduledPoint a = r.points().stream().filter(p -> p.title().equals("A")).findFirst().orElseThrow();
        Scheduler.ScheduledPoint b = r.points().stream().filter(p -> p.title().equals("B")).findFirst().orElseThrow();
        assertEquals(1.0, a.estHours(), 0.001);
        assertEquals(2.0, b.estHours(), 0.001);
        assertTrue(r.warnings().stream().anyMatch(w -> w.contains("已压缩低权重点《A》")));
    }

    @Test
    void tightTime_deletesNonMustLearn() {
        List<Scheduler.PointInput> pts = List.of(
                pt("A", 1.0, 2, false), // 非必学
                pt("B", 2.0, 2, true),
                pt("C", 2.0, 2, true));
        Scheduler.Result r = scheduler.schedule(pts, 0.5, LocalDate.of(2026, 1, 1), null, null);
        List<String> titles = r.points().stream().map(Scheduler.ScheduledPoint::title).toList();
        assertFalse(titles.contains("A"));
        assertTrue(r.warnings().stream().anyMatch(w -> w.contains("已删除非必学点《A》")));
    }

    @Test
    void borrowDueDate_shortensScheduleAndWarns() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 30);
        LocalDate due = LocalDate.of(2026, 1, 15);
        List<Scheduler.PointInput> pts = List.of(pt("A", 2.0, 1, true));
        Scheduler.Result r = scheduler.schedule(pts, 2, start, end, due);
        assertTrue(r.warnings().stream().anyMatch(w -> w.contains("晚于借书到期日")));
        for (Scheduler.ScheduledPoint p : r.points()) {
            assertFalse(p.date().isAfter(due));
        }
    }

    @Test
    void endDateBeforeStartDate_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                scheduler.schedule(List.of(pt("A", 2.0, 1, true)), 2,
                        LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 1), null));
    }

    @Test
    void cyclicPrereq_fallsBackAndWarns() {
        List<Scheduler.PointInput> pts = List.of(
                pt("A", 2.0, 1, true, "B"),
                pt("B", 2.0, 1, true, "A"));
        Scheduler.Result r = scheduler.schedule(pts, 2, LocalDate.of(2026, 1, 1), null, null);
        assertEquals(2, r.points().size());
        assertTrue(r.warnings().stream().anyMatch(w -> w.contains("环")));
    }
}
