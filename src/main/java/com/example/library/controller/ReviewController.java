package com.example.library.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.library.common.result.Result;
import com.example.library.entity.AdjustmentProposal;
import com.example.library.entity.LearningPoint;
import com.example.library.entity.ReviewRecord;
import com.example.library.mapper.AdjustmentProposalMapper;
import com.example.library.mapper.LearningPointMapper;
import com.example.library.mapper.ReviewRecordMapper;
import com.example.library.service.ai.gateway.ModelGateway;
import com.example.library.service.ai.gateway.ModelResult;
import com.example.library.service.ai.gateway.ModelTier;
import com.example.library.service.plan.MasteryCalculator;
import com.example.library.util.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "智学伴-验收", description = "小测/自由练习/复述点评")
@RestController
@RequestMapping("/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ModelGateway gateway;
    private final LearningPointMapper pointMapper;
    private final ReviewRecordMapper reviewMapper;
    private final AdjustmentProposalMapper adjustmentMapper;

    @Operation(summary = "提交练习/复述，AI 点评")
    @PostMapping("/points/{pointId}")
    public Result<Map<String, Object>> submit(@PathVariable Long pointId, @RequestBody Map<String, Object> body) {
        Long uid = UserContext.getCurrentUserId();
        String type = (String) body.getOrDefault("type", "FREE_PRACTICE");
        String content = (String) body.get("content");

        LearningPoint point = pointMapper.selectById(pointId);
        if (point == null || !point.getUserId().equals(uid)) {
            return Result.error("学习点不存在");
        }

        String prompt = """
            你是严格的学习教练，按0-100打分。
            学习点：《%s》，用户提交了%s：
            %s
            只输出纯 JSON，不要 markdown，格式：
            {"score":0-100整数,"feedback":"2-3句具体点评（指出问题+改进建议）","passed":true/false}
            score>=60 为 passed=true。
            """.formatted(point.getTitle(),
                "RETELL".equals(type) ? "复述" : "练习", content);
        ModelResult r = gateway.chat(ModelTier.STRONG, "REVIEW", prompt, uid);

        // 粗解析评分（容忍 markdown fence）
        int score = 80; boolean passed = true; String feedback = r.text();
        try {
            String s = r.text().trim();
            int a = s.indexOf('{'), b = s.lastIndexOf('}');
            if (a >= 0 && b > a) s = s.substring(a, b + 1);
            var m = new com.fasterxml.jackson.databind.ObjectMapper().readTree(s);
            score = m.get("score").asInt();
            passed = m.get("passed").asBoolean();
            feedback = m.get("feedback").asText();
        } catch (Exception ignored) {}

        // 落验收记录
        ReviewRecord rec = new ReviewRecord();
        rec.setPlanId(point.getPlanId());
        rec.setUserId(uid);
        rec.setPointId(pointId);
        rec.setReviewType(type);
        rec.setUserInput(content);
        rec.setAiFeedback(feedback);
        rec.setScore(score);
        rec.setPassed(passed ? 1 : 0);
        reviewMapper.insert(rec);

        // 更新掌握度
        int newScore = passed ? MasteryCalculator.onPass(point.getMasteryScore())
                               : MasteryCalculator.onFail(point.getMasteryScore());
        point.setMasteryScore(newScore);
        point.setLastStudyDate(com.example.library.util.ClockHolder.today());
        point.setStatus(passed ? "DONE" : "FAILED");
        pointMapper.updateById(point);

        // 不及格 → 生成调整方案
        if (!passed) {
            AdjustmentProposal ap = new AdjustmentProposal();
            ap.setPlanId(point.getPlanId());
            ap.setUserId(uid);
            ap.setTriggerType("QUIZ_FAIL");
            ap.setReason("学习点《" + point.getTitle() + "》验收 " + score + " 分未通过");
            ap.setProposal("{\"action\":\"重学该点并安排 1 天后复习\",\"pointId\":" + pointId + "}");
            ap.setStatus("PENDING_CONFIRM");
            adjustmentMapper.insert(ap);
        }

        Map<String, Object> out = new HashMap<>();
        out.put("score", score);
        out.put("feedback", feedback);
        out.put("passed", passed);
        out.put("masteryScore", newScore);
        return Result.success(out);
    }

    @Operation(summary = "出 5 道小测题（Mock 模式返回固定题）")
    @PostMapping("/quiz/generate")
    public Result<String> quiz(@RequestBody Map<String, Object> body) {
        Long uid = UserContext.getCurrentUserId();
        Long planId = Long.parseLong(String.valueOf(body.get("planId")));
        ModelResult r = gateway.chat(ModelTier.STRONG, "QUIZ",
                "为计划 " + planId + " 出 5 道单选题，输出纯 JSON", uid);
        return Result.success(r.text());
    }
}
