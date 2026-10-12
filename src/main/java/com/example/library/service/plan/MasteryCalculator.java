package com.example.library.service.plan;

/**
 * 掌握度算法 + 五类页面状态。
 * 学完+10(封顶60) / 复习+5 / 验收+15 / 不通过-20 / 三天没学-3
 * 状态：未开始 / 学习中 / 待办理 / 待巩固 / 已掌握
 */
public class MasteryCalculator {

    public enum State { NOT_STARTED, LEARNING, DUE, CONSOLIDATING, MASTERED }

    public static int onLearnDone(int cur)    { return Math.min(60, cur + 10); }
    public static int onReview(int cur)      { return cur + 5; }
    public static int onPass(int cur)         { return cur + 15; }
    public static int onFail(int cur)         { return Math.max(0, cur - 20); }
    public static int onIdle3Days(int cur)    { return Math.max(0, cur - 3); }

    /** 判定页面状态 */
    public static State state(int mastery, boolean scheduledBeforeToday,
                              boolean started, boolean reviewedOnce) {
        if (mastery >= 80) return State.MASTERED;
        if (!started) return State.NOT_STARTED;
        if (scheduledBeforeToday) return State.DUE;
        if (mastery >= 40 && reviewedOnce) return State.CONSOLIDATING;
        return State.LEARNING;
    }
}
