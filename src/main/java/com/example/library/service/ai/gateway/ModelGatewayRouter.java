package com.example.library.service.ai.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * 路由网关：配了真 key 走 ChatModelGateway，否则走 MockModelGateway。
 * 业务代码只注入这个，不关心底下是真模型还是假数据。
 */
@Slf4j
@Primary
@Component
public class ModelGatewayRouter implements ModelGateway {

    private final ChatModelGateway chatGateway;
    private final MockModelGateway mockGateway;

    @Value("${spring.ai.openai.api-key:dummy-key}")
    private String apiKey;

    public ModelGatewayRouter(ChatModelGateway chatGateway, MockModelGateway mockGateway) {
        this.chatGateway = chatGateway;
        this.mockGateway = mockGateway;
    }

    @Override
    public ModelResult chat(ModelTier tier, String scene, String prompt, Long userId) {
        // MOCK 档强制走 mock
        if (tier == ModelTier.MOCK) {
            return mockGateway.chat(ModelTier.MOCK, scene, prompt, userId);
        }
        // 没配 key 走 mock
        if (apiKey == null || apiKey.isBlank() || "dummy-key".equals(apiKey)) {
            return mockGateway.chat(ModelTier.MOCK, scene, prompt, userId);
        }
        // 真模型调用，失败自动降级 mock（比如 402 欠费、404 模型不存在）
        try {
            return chatGateway.chat(tier, scene, prompt, userId);
        } catch (Exception e) {
            log.warn("真模型调用失败，降级 Mock: {}", e.getMessage());
            return mockGateway.chat(ModelTier.MOCK, scene, prompt, userId);
        }
    }
}
