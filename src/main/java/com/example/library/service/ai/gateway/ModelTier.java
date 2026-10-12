package com.example.library.service.ai.gateway;

/**
 * 模型档位。
 * STRONG：诊断/规划/点评，强模型
 * WEAK ：打标/分类，便宜模型
 * MOCK ：无 key 时本地假数据，全流程可跑
 */
public enum ModelTier {
    STRONG, WEAK, MOCK
}
