package com.example.library.util;

import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 全局时钟：业务代码取"今天"统一走这里，管理员快进日期只改 offset。
 * 静态字段全局共享，同时保留实例方法供 @Autowired 注入。
 */
@Component
public class ClockHolder {

    private static int offsetDays = 0;

    public static LocalDate today() {
        return LocalDate.now().plusDays(offsetDays);
    }

    public static void fastForward(int days) {
        offsetDays += days;
    }

    public static void reset() {
        offsetDays = 0;
    }

    public static int offset() {
        return offsetDays;
    }

    // 实例方法（供 @Autowired 注入用，委托静态）
    public LocalDate todayInstance() { return today(); }
    public void fastForwardInstance(int days) { fastForward(days); }
    public void resetInstance() { reset(); }
}
