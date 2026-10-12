package com.example.library.controller;

import com.example.library.common.result.Result;
import com.example.library.entity.KnowledgeDocument;
import com.example.library.mapper.KnowledgeDocumentMapper;
import com.example.library.service.ai.gateway.ModelGateway;
import com.example.library.service.ai.gateway.ModelResult;
import com.example.library.service.ai.gateway.ModelTier;
import com.example.library.util.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "资料问答", description = "MySQL 全文检索 + 标出处")
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class QaController {

    private final JdbcTemplate jdbc;
    private final ModelGateway gateway;

    @Operation(summary = "资料问答：基于知识库回答，附出处")
    @PostMapping("/qa")
    public Result<Map<String, Object>> qa(@RequestBody Map<String, String> body) {
        Long uid = UserContext.getCurrentUserId();
        String question = body.get("question");

        // 1. 全文检索（MATCH AGAINST，降级 LIKE）
        List<Map<String, Object>> refs = new ArrayList<>();
        try {
            jdbc.queryForList(
                "SELECT id, title, source, LEFT(content, 200) AS snippet, " +
                "MATCH(title, content) AGAINST (? IN NATURAL LANGUAGE MODE) AS score " +
                "FROM knowledge_document " +
                "WHERE MATCH(title, content) AGAINST (? IN NATURAL LANGUAGE MODE) " +
                "ORDER BY score DESC LIMIT 5", question, question
            ).forEach(row -> refs.add(row));
        } catch (Exception e) {
            // 降级 LIKE
            jdbc.queryForList(
                "SELECT id, title, source, LEFT(content, 200) AS snippet FROM knowledge_document " +
                "WHERE title LIKE ? OR content LIKE ? LIMIT 5",
                "%" + question + "%", "%" + question + "%"
            ).forEach(row -> refs.add(row));
        }

        // 2. 拼上下文调模型（Mock 模式直接返回引用）
        StringBuilder ctx = new StringBuilder();
        for (int i = 0; i < refs.size(); i++) {
            ctx.append("【资料").append(i + 1).append("】").append(refs.get(i).get("title"))
               .append("：").append(refs.get(i).get("snippet")).append("\n");
        }
        String prompt = "用户问题：" + question + "\n参考资料：\n" + ctx +
                "\n请基于资料回答，标注[资料N]。没有资料就说不知道。";
        ModelResult r = gateway.chat(ModelTier.WEAK, "QA", prompt, uid);

        Map<String, Object> out = new HashMap<>();
        out.put("answer", r.text());
        out.put("references", refs);
        out.put("memoryUsed", 10);
        return Result.success(out);
    }
}
