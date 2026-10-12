package com.example.library.controller;

import com.example.library.common.result.Result;
import com.example.library.util.ClockHolder;
import com.example.library.util.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "管理员", description = "演示用：快进日期、全平台账本")
@RestController
@RequestMapping("/admin")
public class AdminController {

    private void requireAdmin() {
        if (!UserContext.isAdmin()) throw new RuntimeException("需要管理员权限");
    }

    @Operation(summary = "快进 N 天（演示用）")
    @PostMapping("/time/fast-forward")
    public Result<Map<String, Object>> forward(@RequestParam int days) {
        requireAdmin();
        ClockHolder.fastForward(days);
        return Result.success(Map.of("today", ClockHolder.today().toString(), "offsetDays", ClockHolder.offset()));
    }

    @Operation(summary = "日期归零")
    @PostMapping("/time/reset")
    public Result<Map<String, Object>> reset() {
        requireAdmin();
        ClockHolder.reset();
        return Result.success(Map.of("today", ClockHolder.today().toString()));
    }
}
