package com.example.library.service.ai.gateway;

/**
 * 统一模型网关：业务代码只调这个，不直接碰 ChatClient。
 */
public interface ModelGateway {

    /**
     * @param tier   强/弱/Mock
     * @param scene  业务场景 DIAGNOSIS/PLAN/MATCH/REVIEW/CHAT/QA
     * @param prompt 完整 prompt
     * @param userId 触发用户（落日志用）
     */
    ModelResult chat(ModelTier tier, String scene, String prompt, Long userId);
}
