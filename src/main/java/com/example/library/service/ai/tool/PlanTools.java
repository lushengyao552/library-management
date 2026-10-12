package com.example.library.service.ai.tool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.library.entity.LearningPoint;
import com.example.library.entity.StudyPlan;
import com.example.library.mapper.LearningPointMapper;
import com.example.library.mapper.StudyPlanMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 学习规划工具：供 LLM 在对话中自主调用。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlanTools {

    private final StudyPlanMapper studyPlanMapper;
    private final LearningPointMapper learningPointMapper;

    @Tool(description = "查询当前用户的学习计划列表和进度")
    public List<PlanStatusResult> getMyPlans(ToolContext toolContext) {
        Long userId = (Long) toolContext.getContext().get("userId");
        if (userId == null) throw new IllegalStateException("未登录");

        List<StudyPlan> plans = studyPlanMapper.selectList(
                new LambdaQueryWrapper<StudyPlan>()
                        .eq(StudyPlan::getUserId, userId)
                        .orderByDesc(StudyPlan::getCreateTime)
                        .last("LIMIT 5"));

        return plans.stream().map(p -> {
            long total = learningPointMapper.selectCount(
                    new LambdaQueryWrapper<LearningPoint>().eq(LearningPoint::getPlanId, p.getId()));
            long done = learningPointMapper.selectCount(
                    new LambdaQueryWrapper<LearningPoint>()
                            .eq(LearningPoint::getPlanId, p.getId())
                            .eq(LearningPoint::getStatus, "DONE"));
            return new PlanStatusResult(
                    p.getId(), p.getTitle(), p.getStatus(),
                    total, done, p.getExamDate() == null ? null : p.getExamDate().toString());
        }).collect(Collectors.toList());
    }

    @Tool(description = "查询某个学习计划下的所有学习点详情，包括掌握度和状态")
    public List<PointDetailResult> getPlanPoints(
            @ToolParam(description = "计划ID") Long planId,
            ToolContext toolContext) {
        Long userId = (Long) toolContext.getContext().get("userId");
        if (userId == null) throw new IllegalStateException("未登录");

        List<LearningPoint> points = learningPointMapper.selectList(
                new LambdaQueryWrapper<LearningPoint>()
                        .eq(LearningPoint::getPlanId, planId)
                        .eq(LearningPoint::getUserId, userId)
                        .orderByAsc(LearningPoint::getSeq));

        return points.stream().map(p -> new PointDetailResult(
                p.getId(), p.getTitle(), p.getSummary(),
                p.getScheduledDate() == null ? null : p.getScheduledDate().toString(),
                p.getStatus(), p.getMasteryScore()
        )).collect(Collectors.toList());
    }

    public record PlanStatusResult(Long planId, String title, String status,
                                   long totalPoints, long donePoints, String examDate) {}
    public record PointDetailResult(Long pointId, String title, String summary,
                                    String scheduledDate, String status, Integer masteryScore) {}
}
