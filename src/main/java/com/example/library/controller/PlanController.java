package com.example.library.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.library.common.result.Result;
import com.example.library.config.ScenarioProperties;
import com.example.library.entity.LearningPoint;
import com.example.library.entity.StudyPlan;
import com.example.library.mapper.LearningPointMapper;
import com.example.library.mapper.StudyPlanMapper;
import com.example.library.service.plan.PlanPipeline;
import com.example.library.util.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Tag(name = "智学伴-学习计划", description = "诊断→规划→配书→排期闭环")
@RestController
@RequestMapping("/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanPipeline pipeline;
    private final ScenarioProperties scenarios;
    private final StudyPlanMapper studyPlanMapper;
    private final LearningPointMapper learningPointMapper;

    @Operation(summary = "列出可用学习场景")
    @GetMapping("/scenarios")
    public Result<List<ScenarioProperties.Scenario>> scenarios() {
        return Result.success(scenarios.getItems());
    }

    @Operation(summary = "步骤1：出诊断题")
    @PostMapping("/diagnosis")
    public Result<String> diagnosis(@RequestBody Map<String, Object> body) {
        String scenario = (String) body.get("scenarioCode");
        String goal = (String) body.get("goal");
        return Result.success(pipeline.startDiagnosis(scenario, goal));
    }

    @Operation(summary = "步骤2：预览计划（不落库）")
    @PostMapping("/preview")
    public Result<Map<String, Object>> preview(@RequestBody Map<String, Object> body) {
        String scenario = (String) body.get("scenarioCode");
        String goal = (String) body.get("goal");
        String answers = (String) body.get("answers");
        double hours = Double.parseDouble(String.valueOf(body.getOrDefault("dailyHours", "2")));
        String end = (String) body.get("endDate");
        LocalDate endDate = end != null ? LocalDate.parse(end) : null;
        return Result.success(pipeline.preview(scenario, goal, answers, hours, endDate));
    }

    @Operation(summary = "步骤3：确认生成正式计划")
    @PostMapping
    public Result<Long> confirm(@RequestBody Map<String, Object> body) {
        String scenario = (String) body.get("scenarioCode");
        String goal = (String) body.get("goal");
        String answers = (String) body.get("answers");
        double hours = Double.parseDouble(String.valueOf(body.getOrDefault("dailyHours", "2")));
        String end = (String) body.get("endDate");
        LocalDate endDate = end != null ? LocalDate.parse(end) : null;
        return Result.success(pipeline.confirm(scenario, goal, answers, hours, endDate));
    }

    @Operation(summary = "我的计划列表")
    @GetMapping
    public Result<List<StudyPlan>> myPlans() {
        Long uid = UserContext.getCurrentUserId();
        return Result.success(studyPlanMapper.selectList(
                new LambdaQueryWrapper<StudyPlan>().eq(StudyPlan::getUserId, uid)
                        .orderByDesc(StudyPlan::getCreateTime)));
    }

    @Operation(summary = "计划下的学习点")
    @GetMapping("/{planId}/points")
    public Result<List<LearningPoint>> points(@PathVariable Long planId) {
        Long uid = UserContext.getCurrentUserId();
        return Result.success(learningPointMapper.selectList(
                new LambdaQueryWrapper<LearningPoint>().eq(LearningPoint::getPlanId, planId)
                        .eq(LearningPoint::getUserId, uid)
                        .orderByAsc(LearningPoint::getSeq)));
    }
}
