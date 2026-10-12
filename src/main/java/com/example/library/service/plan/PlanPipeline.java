package com.example.library.service.plan;

import com.example.library.config.ScenarioProperties;
import com.example.library.entity.LearningPoint;
import com.example.library.entity.StudyPlan;
import com.example.library.mapper.LearningPointMapper;
import com.example.library.mapper.StudyPlanMapper;
import com.example.library.service.ai.gateway.ModelGateway;
import com.example.library.service.ai.gateway.ModelResult;
import com.example.library.service.ai.gateway.ModelTier;
import com.example.library.util.ClockHolder;
import com.example.library.util.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 计划生成流水线：诊断 → 拆分学习点 → JSON 校验 → 配书 → 排期 → 预览/落库。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlanPipeline {

    private final ModelGateway gateway;
    private final StudyPlanMapper studyPlanMapper;
    private final LearningPointMapper learningPointMapper;
    private final ScenarioProperties scenarios;
    private final BookMatcher bookMatcher;
    private final Scheduler scheduler = new Scheduler();

    /** 步骤1：出诊断题 */
    public String startDiagnosis(String scenarioCode, String goal) {
        Long uid = UserContext.getCurrentUserId();
        String prompt = """
            你是资深学习规划师，精通各类技术/职业学习路径。
            用户目标：「%s」，场景=%s。
            请根据这个目标出3道学情诊断单选题，用于判断用户起点。
            只输出纯 JSON，不要 markdown 代码块，不要解释文字，格式：
            {"questions":[{"stem":"题干","options":["选项1","选项2","选项3","选项4"],"answer_idx":0}]}
            """.formatted(goal, scenarioCode);
        ModelResult r = gateway.chat(ModelTier.STRONG, "DIAGNOSIS", prompt, uid);
        return stripJsonFence(r.text());
    }

    /** 步骤2-5：拆学习点 + 校验 + 配书 + 排期，返回预览（不落库） */
    public Map<String, Object> preview(String scenarioCode, String goal, String answersJson,
                                        double dailyHours, LocalDate endDate) {
        Long uid = UserContext.getCurrentUserId();
        String prompt = """
            你是资深学习规划师，参考 GitHub 上的 awesome-learning、roadmap.sh 等公开学习路径。
            用户目标：「%s」，诊断答案：%s。
            请把目标拆成6-10个学习点，按学习顺序排列，每个点要有一句话说明学什么、怎么练。
            只输出纯 JSON，不要 markdown 代码块，不要解释文字，格式：
            {"points":[{"title":"点标题","summary":"一句话说明（含参考资料）","weight":1.5,"must_learn":true,"prereq":[]}]}
            weight 取值1.0-2.0，核心点2.0，补充点1.0。prereq 填前置点的 title。
            """.formatted(goal, answersJson);
        ModelResult r = gateway.chat(ModelTier.STRONG, "PLAN", prompt, uid);

        // 解析学习点
        List<Scheduler.PointInput> points = parsePoints(stripJsonFence(r.text()));

        // 排期（用 ClockHolder，支持快进日期）
        Scheduler.Result sched = scheduler.schedule(points, dailyHours,
                ClockHolder.today(), endDate, null);

        Map<String, Object> out = new HashMap<>();
        out.put("points", sched.points());
        out.put("warnings", sched.warnings());
        out.put("scenarioCode", scenarioCode);
        out.put("goal", goal);
        return out;
    }

    /** 步骤6：确认落库 */
    public Long confirm(String scenarioCode, String goal, String answersJson,
                        double dailyHours, LocalDate endDate) {
        Long uid = UserContext.getCurrentUserId();
        Map<String, Object> preview = preview(scenarioCode, goal, answersJson, dailyHours, endDate);

        StudyPlan plan = new StudyPlan();
        plan.setUserId(uid);
        plan.setSubject(goal);
        plan.setTitle(goal + " 学习计划");
        plan.setExamDate(endDate != null ? endDate : LocalDate.now().plusDays(30));
        plan.setDailyHours(BigDecimal.valueOf(dailyHours));
        plan.setStatus("ACTIVE");
        studyPlanMapper.insert(plan);

        @SuppressWarnings("unchecked")
        List<Scheduler.ScheduledPoint> pts = (List<Scheduler.ScheduledPoint>) preview.get("points");

        // 查场景对应的书分类
        Long catId = scenarios.getItems().stream()
                .filter(s -> s.getCode().equals(scenarioCode))
                .findFirst()
                .map(s -> s.getBookCategoryIds() == null || s.getBookCategoryIds().isEmpty() ? null : s.getBookCategoryIds().get(0))
                .orElse(null);

        int seq = 1;
        for (Scheduler.ScheduledPoint sp : pts) {
            LearningPoint lp = new LearningPoint();
            lp.setPlanId(plan.getId());
            lp.setUserId(uid);
            lp.setSeq(seq++);
            lp.setTitle(sp.title());
            lp.setSummary(sp.summary());
            lp.setEstHours(BigDecimal.valueOf(sp.estHours()));
            lp.setScheduledDate(sp.date());
            lp.setStatus("PENDING");
            lp.setMasteryScore(0);
            lp.setWeight(BigDecimal.ONE);
            lp.setMustLearn(1);
            // 配书
            Object[] matched = bookMatcher.match(sp.title(), catId);
            if (matched[0] != null) {
                lp.setMatchedBookId(((Number) matched[0]).longValue());
                lp.setMatchedChapter((String) matched[1]);
            }
            learningPointMapper.insert(lp);
        }
        log.info("用户{}生成计划{}，{}个学习点", uid, plan.getId(), pts.size());
        return plan.getId();
    }

    /** 去掉 LLM 输出里的 markdown fence（```json ... ```）和前后解释文字 */
    private String stripJsonFence(String text) {
        if (text == null) return "{}";
        String s = text.trim();
        if (s.startsWith("```")) {
            int nl = s.indexOf('\n');
            if (nl > 0) s = s.substring(nl + 1);
            int end = s.lastIndexOf("```");
            if (end > 0) s = s.substring(0, end);
        }
        // 找到第一个 { 到最后一个 }
        int a = s.indexOf('{');
        int b = s.lastIndexOf('}');
        if (a >= 0 && b > a) s = s.substring(a, b + 1);
        return s.trim();
    }

    /** 从模型输出的 JSON 粗解析学习点 */
    @SuppressWarnings("unchecked")
    private List<Scheduler.PointInput> parsePoints(String json) {
        List<Scheduler.PointInput> out = new ArrayList<>();
        try {
            var om = new com.fasterxml.jackson.databind.ObjectMapper();
            var node = om.readTree(json);
            for (var p : node.get("points")) {
                out.add(new Scheduler.PointInput(
                        p.get("title").asText(),
                        p.has("summary") ? p.get("summary").asText() : "",
                        p.has("weight") ? p.get("weight").asDouble() : 1.0,
                        1.0,
                        p.has("must_learn") && p.get("must_learn").asBoolean(),
                        List.of()
                ));
            }
        } catch (Exception e) {
            log.warn("学习点 JSON 解析失败，走模板兜底: {}", e.getMessage());
            out.add(new Scheduler.PointInput("基础概念", "入门介绍", 2.0, 1.5, true, List.of()));
            out.add(new Scheduler.PointInput("核心练习", "动手实战", 2.0, 2.0, true, List.of("基础概念")));
            out.add(new Scheduler.PointInput("综合应用", "项目整合", 1.0, 1.0, false, List.of("核心练习")));
        }
        return out;
    }
}
