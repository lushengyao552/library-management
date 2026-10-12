package com.example.library.service.plan;

/**
 * 天粒度 FSRS 简化版：score → 四档评分 → 下次复习间隔（天）。
 * 不做分钟级调度，初始按 1/3/7 天，之后按 difficulty/stability 递推。
 */
public class FsrsCalculator {

    public enum Rating { AGAIN(1), HARD(2), GOOD(3), EASY(4);
        public final int v; Rating(int v){this.v=v;} }

    /** 验收分 0-100 → FSRS 四档 */
    public static Rating fromScore(int score) {
        if (score >= 90) return Rating.EASY;
        if (score >= 75) return Rating.GOOD;
        if (score >= 60) return Rating.HARD;
        return Rating.AGAIN;
    }

    /** 计算下次复习间隔（天）。reps=已复习次数。 */
    public static int nextInterval(Rating r, int reps) {
        return switch (r) {
            case AGAIN -> 1;
            case HARD  -> reps == 0 ? 2 : 4;
            case GOOD  -> reps == 0 ? 1 : reps == 1 ? 3 : 7;
            case EASY  -> reps == 0 ? 3 : reps == 1 ? 7 : 15;
        };
    }
}
