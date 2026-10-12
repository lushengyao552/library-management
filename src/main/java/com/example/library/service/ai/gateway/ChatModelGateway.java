package com.example.library.service.ai.gateway;

import com.example.library.entity.AiCallLog;
import com.example.library.mapper.AiCallLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 真实模型网关：走 MoMA（OpenAI 兼容）。
 * STRONG / WEAK 用不同 model 和单价；MOCK 档直接被 MockModelGateway 接管。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatModelGateway implements ModelGateway {

    private final ChatClient.Builder chatClientBuilder;
    private final AiCallLogMapper aiCallLogMapper;

    @Value("${spring.ai.openai.chat.options.model:deepseek-v3}")
    private String strongModel;

    @Value("${spring.ai.openai.chat.options.model-weak:deepseek-v3}")
    private String weakModel;

    @Value("${library.model.price.strong-input-per-1k:0.0028}")
    private double strongInputPrice;
    @Value("${library.model.price.strong-output-per-1k:0.0084}")
    private double strongOutputPrice;
    @Value("${library.model.price.weak-input-per-1k:0.0006}")
    private double weakInputPrice;
    @Value("${library.model.price.weak-output-per-1k:0.0018}")
    private double weakOutputPrice;

    @Override
    public ModelResult chat(ModelTier tier, String scene, String prompt, Long userId) {
        String modelName = tier == ModelTier.STRONG ? strongModel : weakModel;
        long start = System.currentTimeMillis();
        try {
            ChatResponse resp = chatClientBuilder.build()
                    .prompt()
                    .user(prompt)
                    .call()
                    .chatResponse();

            String text = resp.getResult().getOutput().getText();
            int pt = resp.getMetadata().getUsage().getPromptTokens();
            int ct = resp.getMetadata().getUsage().getCompletionTokens();
            int total = pt + ct;

            double priceIn = tier == ModelTier.STRONG ? strongInputPrice : weakInputPrice;
            double priceOut = tier == ModelTier.STRONG ? strongOutputPrice : weakOutputPrice;
            double cost = pt / 1000.0 * priceIn + ct / 1000.0 * priceOut;

            saveLog(userId, modelName, tier, scene, pt, ct, total, cost,
                    System.currentTimeMillis() - start, "SUCCESS", null, prompt);
            return new ModelResult(text, modelName, pt, ct, total, cost, false);
        } catch (Exception e) {
            log.error("模型调用失败 scene={}", scene, e);
            saveLog(userId, modelName, tier, scene, 0, 0, 0, 0,
                    System.currentTimeMillis() - start, "ERROR", e.getMessage(), prompt);
            throw new RuntimeException("模型调用失败: " + e.getMessage());
        }
    }

    private void saveLog(Long userId, String model, ModelTier tier, String scene,
                         int pt, int ct, int total, double cost, long dur,
                         String status, String err, String prompt) {
        AiCallLog l = new AiCallLog();
        l.setUserId(userId);
        l.setModel(model);
        l.setTier(tier.name());
        l.setScene(scene);
        l.setPromptTokens(pt);
        l.setCompletionTokens(ct);
        l.setTotalTokens(total);
        l.setCost(BigDecimal.valueOf(cost));
        l.setLlmDuration(dur);
        l.setTotalDuration(dur);
        l.setStatus(status);
        l.setErrorMsg(err);
        l.setUserQuery(prompt != null && prompt.length() > 500 ? prompt.substring(0, 500) : prompt);
        aiCallLogMapper.insert(l);
    }
}
