package com.example.library.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.library.common.result.Result;
import com.example.library.entity.StudyPlan;
import com.example.library.entity.StudyTask;
import com.example.library.mapper.StudyPlanMapper;
import com.example.library.mapper.StudyTaskMapper;
import com.example.library.util.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "学习计划", description = "学习计划与任务管理")
@RestController
@RequestMapping("/study-plans")
@RequiredArgsConstructor
public class StudyPlanController {

    private final StudyPlanMapper studyPlanMapper;
    private final StudyTaskMapper studyTaskMapper;

    @Operation(summary = "我的学习计划列表")
    @GetMapping
    public Result<List<StudyPlan>> myPlans() {
        Long userId = UserContext.getCurrentUserId();
        List<StudyPlan> plans = studyPlanMapper.selectList(
                new LambdaQueryWrapper<StudyPlan>()
                        .eq(StudyPlan::getUserId, userId)
                        .orderByDesc(StudyPlan::getCreateTime)
        );
        return Result.success(plans);
    }

    @Operation(summary = "查看某个计划下的所有任务")
    @GetMapping("/{planId}/tasks")
    public Result<List<StudyTask>> tasks(@PathVariable Long planId) {
        Long userId = UserContext.getCurrentUserId();
        // 校验归属
        StudyPlan plan = studyPlanMapper.selectById(planId);
        if (plan == null || !plan.getUserId().equals(userId)) {
            return Result.error("计划不存在或无权限");
        }
        List<StudyTask> tasks = studyTaskMapper.selectList(
                new LambdaQueryWrapper<StudyTask>()
                        .eq(StudyTask::getPlanId, planId)
                        .orderByAsc(StudyTask::getDayNumber)
        );
        return Result.success(tasks);
    }

    @Operation(summary = "标记任务完成")
    @PostMapping("/tasks/{taskId}/done")
    public Result<String> markDone(@PathVariable Long taskId) {
        Long userId = UserContext.getCurrentUserId();
        StudyTask task = studyTaskMapper.selectById(taskId);
        if (task == null || !task.getUserId().equals(userId)) {
            return Result.error("任务不存在或无权限");
        }
        task.setStatus("DONE");
        task.setDoneTime(LocalDateTime.now());
        studyTaskMapper.updateById(task);
        return Result.success("任务已完成");
    }
}
