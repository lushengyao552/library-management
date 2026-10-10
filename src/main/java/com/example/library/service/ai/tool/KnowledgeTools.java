package com.example.library.service.ai.tool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.library.entity.KnowledgeDocument;
import com.example.library.mapper.KnowledgeDocumentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识库检索工具：从 knowledge_document 表检索校园规章/FAQ。
 * Demo 阶段用关键词模糊匹配，后续升级为向量检索。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeTools {

    private final KnowledgeDocumentMapper knowledgeDocumentMapper;

    @Tool(description = "检索校园规章制度、图书馆规则、常见问题等知识库文档，返回相关条款内容")
    public List<KnowledgeResult> searchKnowledge(
            @ToolParam(description = "用户问题关键词，如：逾期、罚款、开馆时间、续借") String query) {

        LambdaQueryWrapper<KnowledgeDocument> q = new LambdaQueryWrapper<KnowledgeDocument>()
                .like(KnowledgeDocument::getTitle, query)
                .or()
                .like(KnowledgeDocument::getContent, query)
                .orderByDesc(KnowledgeDocument::getUpdateTime)
                .last("LIMIT 5");

        List<KnowledgeDocument> docs = knowledgeDocumentMapper.selectList(q);
        log.info("知识库检索「{}」命中 {} 条", query, docs.size());

        return docs.stream().map(d -> new KnowledgeResult(
                d.getId(), d.getTitle(), d.getContent(), d.getDocType(), d.getSource()
        )).collect(Collectors.toList());
    }

    public record KnowledgeResult(Long id, String title, String content, String docType, String source) {}
}
