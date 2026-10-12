package com.example.library.service.ai.tool;

import com.example.library.entity.StudyPlan;
import com.example.library.entity.StudyTask;
import com.example.library.mapper.StudyPlanMapper;
import com.example.library.mapper.StudyTaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * 学习规划工具：根据用户参数创建学习计划和每日任务。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StudyPlanTools {

    private final StudyPlanMapper studyPlanMapper;
    private final StudyTaskMapper studyTaskMapper;

    @Tool(description = "根据科目、考试日期、每日可学习时长和薄弱点，创建学习计划并拆分为每日任务")
    public Long createStudyPlan(
            @ToolParam(description = "考试科目名称，如：Java、数据库、计算机网络") String subject,
            @ToolParam(description = "考试日期，格式 YYYY-MM-DD") String examDate,
            @ToolParam(description = "每天可学习小时数，如：2.0") Double dailyHours,
            @ToolParam(description = "薄弱知识点，如：SQL连接查询、JVM内存模型", required = false) String weakness,
            ToolContext toolContext) {

        Long userId = (Long) toolContext.getContext().get("userId");
        if (userId == null) {
            throw new IllegalStateException("未登录");
        }

        LocalDate exam = LocalDate.parse(examDate);
        long days = ChronoUnit.DAYS.between(LocalDate.now(), exam);
        if (days <= 0) {
            throw new IllegalArgumentException("考试日期已过或无效");
        }

        // 1. 存计划
        StudyPlan plan = new StudyPlan();
        plan.setUserId(userId);
        plan.setSubject(subject);
        plan.setExamDate(exam);
        plan.setDailyHours(BigDecimal.valueOf(dailyHours));
        plan.setWeakness(weakness);
        plan.setTitle(subject + "备考计划（" + days + "天）");
        plan.setStatus("ACTIVE");
        studyPlanMapper.insert(plan);

        // 2. 拆任务（前 70% 学新知识，后 30% 复习刷题）
        List<StudyTask> tasks = new ArrayList<>();
        long learnDays = (long) (days * 0.7);
        for (int i = 1; i <= days; i++) {
            StudyTask t = new StudyTask();
            t.setPlanId(plan.getId());
            t.setUserId(userId);
            t.setDayNumber(i);
            t.setEstimatedHours(BigDecimal.valueOf(dailyHours));
            t.setStatus("PENDING");

            if (i <= learnDays) {
                t.setTitle("第" + i + "天：学习" + subject + "核心知识点");
                t.setContent("完成" + subject + "第" + i + "章学习，" +
                        (weakness != null ? "重点攻克：" + weakness : "配合教材和课程视频"));
            } else {
                t.setTitle("第" + i + "天：复习与刷题");
                t.setContent("复习前" + learnDays + "天笔记，做" + subject + "历年真题和错题回顾");
            }
            tasks.add(t);
        }
        tasks.forEach(studyTaskMapper::insert);

        log.info("用户{}创建{}备考计划，{}天，拆{}个任务", userId, subject, days, tasks.size());
        return plan.getId();
    }

    @Tool(description = "查询当前用户的学习计划列表")
    public List<PlanSummary> listMyPlans(ToolContext toolContext) {
        Long userId = (Long) toolContext.getContext().get("userId");
        List<StudyPlan> plans = studyPlanMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StudyPlan>()
                        .eq(StudyPlan::getUserId, userId)
                        .orderByDesc(StudyPlan::getCreateTime)
        );
        return plans.stream().map(p -> new PlanSummary(
                p.getId(), p.getTitle(), p.getSubject(), p.getExamDate().toString(), p.getStatus()
        )).toList();
    }

    public record PlanSummary(Long id, String title, String subject, String examDate, String status) {}
}
