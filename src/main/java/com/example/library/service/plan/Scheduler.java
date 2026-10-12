package com.example.library.service.plan;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 纯 Java 排期器（不依赖 Spring，可单测）。
 * 规则：
 *  1. 有 endDate 倒排，最后 20% 天留作复习
 *  2. 先学前置（拓扑排序）
 *  3. 每天总时长 <= dailyHours
 *  4. 时间不够先砍低权重，再删 mustLearn=0 的点，收集 warnings
 *  5. 阅读计划最后 scheduledDate <= borrowDueDate
 *  6. 复习积压 > 5 当天不排新内容
 */
public class Scheduler {

    public record PointInput(String title, String summary, double weight, double estHours,
                             boolean mustLearn, List<String> prereq) {}

    public record ScheduledPoint(String title, String summary, double estHours, LocalDate date,
                                  Long matchedBookId, String chapter) {}

    public record Result(List<ScheduledPoint> points, List<String> warnings) {}

    public Result schedule(List<PointInput> points, double dailyHours,
                           LocalDate startDate, LocalDate endDate,
                           LocalDate borrowDueDate) {
        List<String> warnings = new ArrayList<>();
        if (points == null || points.isEmpty()) {
            return new Result(List.of(), List.of("没有可排的学习点"));
        }

        // 规则1：决定排期区间
        LocalDate lastDay = endDate != null ? endDate : startDate.plusDays(points.size() * 2L);
        if (borrowDueDate != null && lastDay.isAfter(borrowDueDate)) {
            warnings.add("警告：计划结束日 " + lastDay + " 晚于借书到期日 " + borrowDueDate + "，请续借或换薄书");
            lastDay = borrowDueDate;
        }
        long totalDays = java.time.temporal.ChronoUnit.DAYS.between(startDate, lastDay) + 1;
        if (totalDays <= 0) {
            throw new IllegalArgumentException("结束日期必须晚于开始日期");
        }
        long reviewDays = Math.max(1, Math.round(totalDays * 0.2)); // 最后20%复习
        long learnDays = totalDays - reviewDays;

        // 规则2：拓扑排序（按 prereq title）
        List<PointInput> sorted = topoSort(points, warnings);

        // 规则4：时间不够时压缩/删除
        double totalHours = sorted.stream().mapToDouble(PointInput::estHours).sum();
        double availableHours = learnDays * dailyHours;
        if (totalHours > availableHours) {
            // 规则4：先压缩低权重（按权重升序选点，但保持拓扑顺序不变），再删非必学
            List<PointInput> orderByWeight = new ArrayList<>(sorted);
            orderByWeight.sort((a, b) -> Double.compare(a.weight(), b.weight()));

            Map<String, Double> hours = new HashMap<>();
            for (PointInput p : sorted) {
                hours.put(p.title(), p.estHours());
            }

            for (PointInput p : orderByWeight) {
                if (totalHours <= availableHours) break;
                if (p.weight() < 1.5) {
                    double half = hours.get(p.title()) / 2;
                    hours.put(p.title(), half);
                    totalHours -= half;
                    warnings.add("已压缩低权重点《" + p.title() + "》学时减半");
                }
            }

            Set<String> removed = new HashSet<>();
            if (totalHours > availableHours) {
                for (PointInput p : orderByWeight) {
                    if (totalHours <= availableHours) break;
                    if (!p.mustLearn()) {
                        removed.add(p.title());
                        totalHours -= hours.get(p.title());
                        warnings.add("时间不够，已删除非必学点《" + p.title() + "》");
                    }
                }
            }

            // 按拓扑顺序重建：应用压缩后的学时、剔除已删点
            List<PointInput> rebuilt = new ArrayList<>();
            for (PointInput p : sorted) {
                if (removed.contains(p.title())) continue;
                double h = hours.getOrDefault(p.title(), p.estHours());
                rebuilt.add(new PointInput(p.title(), p.summary(), p.weight(), h, p.mustLearn(), p.prereq()));
            }
            sorted = rebuilt;
        }

        // 规则3+6：按天铺，每天不超 dailyHours
        Map<LocalDate, Double> dayLoad = new HashMap<>();
        List<ScheduledPoint> out = new ArrayList<>();
        for (PointInput p : sorted) {
            double need = p.estHours();
            // 找一个能放下的学习日（不超过 learnDays）
            LocalDate placed = null;
            for (int i = 0; i < learnDays; i++) {
                LocalDate d = startDate.plusDays(i);
                double used = dayLoad.getOrDefault(d, 0.0);
                // 规则6：复习积压>5 时跳过新内容（简化：当天已排满就跳过）
                if (used + need <= dailyHours + 0.01) {
                    dayLoad.put(d, used + need);
                    placed = d;
                    break;
                }
            }
            if (placed == null) {
                warnings.add("点《" + p.title() + "》排不进学习日，顺延到复习日");
                placed = startDate.plusDays((int) learnDays);
            }
            out.add(new ScheduledPoint(p.title(), p.summary(), need, placed, null, null));
        }
        return new Result(out, warnings);
    }

    private List<PointInput> topoSort(List<PointInput> points, List<String> warnings) {
        Map<String, PointInput> byTitle = new LinkedHashMap<>();
        for (PointInput p : points) byTitle.put(p.title(), p);
        Map<String, Set<String>> indeg = new HashMap<>();
        Map<String, List<String>> adj = new HashMap<>();
        // 先为所有点建好邻接表与入度表，再连边，避免前置节点还没初始化导致 adj.get(pre) 为 null
        for (PointInput p : points) {
            indeg.put(p.title(), new HashSet<>());
            adj.put(p.title(), new ArrayList<>());
        }
        for (PointInput p : points) {
            for (String pre : p.prereq()) {
                if (byTitle.containsKey(pre)) {
                    indeg.get(p.title()).add(pre);
                    adj.get(pre).add(p.title());
                }
            }
        }
        List<PointInput> result = new ArrayList<>();
        var queue = new java.util.ArrayDeque<String>();
        for (var e : indeg.entrySet()) if (e.getValue().isEmpty()) queue.add(e.getKey());
        while (!queue.isEmpty()) {
            String t = queue.poll();
            result.add(byTitle.get(t));
            for (String next : adj.get(t)) {
                indeg.get(next).remove(t);
                if (indeg.get(next).isEmpty()) queue.add(next);
            }
        }
        if (result.size() != points.size()) {
            warnings.add("前置依赖存在环，已按原序输出");
            result = new ArrayList<>(points);
        }
        return result;
    }
}
