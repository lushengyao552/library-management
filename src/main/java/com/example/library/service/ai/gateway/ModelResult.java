package com.example.library.service.ai.gateway;

/**
 * 一次模型调用的统一返回。
 */
public record ModelResult(
        String text,
        String model,
        int promptTokens,
        int completionTokens,
        int totalTokens,
        double costYuan,
        boolean fallback
) {
    public static ModelResult mock(String text) {
        return new ModelResult(text, "mock", 0, 0, 0, 0.0, false);
    }
}
