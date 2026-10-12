package com.example.library.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.library.common.result.Result;
import com.example.library.entity.AdjustmentProposal;
import com.example.library.mapper.AdjustmentProposalMapper;
import com.example.library.util.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "智学伴-调整方案", description = "AI 出方案，用户确认才执行")
@RestController
@RequestMapping("/adjustments")
@RequiredArgsConstructor
public class AdjustmentController {

    private final AdjustmentProposalMapper mapper;

    @Operation(summary = "待确认的调整方案列表")
    @GetMapping
    public Result<List<AdjustmentProposal>> list(@RequestParam(defaultValue = "PENDING_CONFIRM") String status) {
        Long uid = UserContext.getCurrentUserId();
        return Result.success(mapper.selectList(
                new LambdaQueryWrapper<AdjustmentProposal>()
                        .eq(AdjustmentProposal::getUserId, uid)
                        .eq(AdjustmentProposal::getStatus, status)
                        .orderByDesc(AdjustmentProposal::getCreateTime)));
    }

    @Operation(summary = "确认执行方案（真正改业务数据）")
    @PostMapping("/{id}/execute")
    public Result<String> execute(@PathVariable Long id) {
        AdjustmentProposal ap = mapper.selectById(id);
        if (ap == null || !ap.getUserId().equals(UserContext.getCurrentUserId())) {
            return Result.error("方案不存在");
        }
        ap.setStatus("EXECUTED");
        ap.setExecuteTime(LocalDateTime.now());
        mapper.updateById(ap);
        // 真实改业务表的逻辑这里留口：根据 proposal JSON 重排学习点
        return Result.success("方案已执行：" + ap.getReason());
    }

    @Operation(summary = "忽略方案")
    @PostMapping("/{id}/ignore")
    public Result<String> ignore(@PathVariable Long id) {
        AdjustmentProposal ap = mapper.selectById(id);
        if (ap == null || !ap.getUserId().equals(UserContext.getCurrentUserId())) {
            return Result.error("方案不存在");
        }
        ap.setStatus("IGNORED");
        mapper.updateById(ap);
        return Result.success("已忽略");
    }
}
