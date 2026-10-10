package com.example.library.service.ai;

import com.example.library.dto.ai.ChatRequest;
import com.example.library.entity.ChatMessage;
import com.example.library.entity.ChatSession;
import com.example.library.mapper.ChatMessageMapper;
import com.example.library.mapper.ChatSessionMapper;
import com.example.library.service.ai.tool.BookTools;
import com.example.library.service.ai.tool.KnowledgeTools;
import com.example.library.service.ai.tool.StudyPlanTools;
import com.example.library.util.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * AI 聊天服务。
 * - 配了真实 AI_API_KEY：走 MoMA/OpenAI 兼容接口
 * - 没配 Key（默认 dummy）：返回 mock 响应，验证 Controller→Service→DB 链路
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private final ChatClient.Builder chatClientBuilder;
    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final BookTools bookTools;
    private final KnowledgeTools knowledgeTools;
    private final StudyPlanTools studyPlanTools;

    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    public String chat(ChatRequest request) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("未登录");
        }

        ChatSession session = resolveSession(request.getSessionId(), userId);

        String answer;
        if (apiKey == null || apiKey.isBlank() || "dummy-key".equals(apiKey)) {
            answer = "[Mock 模式] 你说的是：「" + request.getMessage() + "」。配置真实 AI_API_KEY 后将调用 MoMA 模型。";
            log.info("Mock 模式，未调用 LLM");
        } else {
            long start = System.currentTimeMillis();
            try {
                answer = chatClientBuilder.build()
                        .prompt()
                        .system("你是智学校园的 AI 学习助手，回答简洁、准确。需要查图书或借阅记录时调用对应工具。")
                        .user(request.getMessage())
                        .tools(bookTools, knowledgeTools, studyPlanTools)
                        .call()
                        .content();
            } catch (Exception e) {
                log.error("LLM 调用失败", e);
                throw new RuntimeException("AI 服务暂时不可用：" + e.getMessage());
            }
            log.info("LLM 调用耗时 {}ms", System.currentTimeMillis() - start);
        }

        saveMessage(session.getId(), userId, "user", request.getMessage(), "text", null);
        saveMessage(session.getId(), userId, "assistant", answer, "text", null);

        return answer;
    }

    private ChatSession resolveSession(Long sessionId, Long userId) {
        if (sessionId != null) {
            ChatSession s = chatSessionMapper.selectById(sessionId);
            if (s != null && s.getUserId().equals(userId)) {
                return s;
            }
        }
        ChatSession s = new ChatSession();
        s.setUserId(userId);
        s.setTitle("新对话");
        s.setStatus(0);
        chatSessionMapper.insert(s);
        return s;
    }

    private void saveMessage(Long sessionId, Long userId, String role, String content, String type, String toolName) {
        ChatMessage m = new ChatMessage();
        m.setSessionId(sessionId);
        m.setUserId(userId);
        m.setRole(role);
        m.setContent(content);
        m.setMessageType(type);
        m.setToolName(toolName);
        chatMessageMapper.insert(m);
    }
}
