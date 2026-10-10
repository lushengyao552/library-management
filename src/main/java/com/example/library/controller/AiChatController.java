package com.example.library.controller;

import com.example.library.common.result.Result;
import com.example.library.dto.ai.ChatRequest;
import com.example.library.service.ai.AiChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "AI 聊天", description = "智学校园 AI 学习助手")
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;

    @Operation(summary = "单轮对话")
    @PostMapping("/chat")
    public Result<String> chat(@RequestBody @Valid ChatRequest request) {
        return Result.success(aiChatService.chat(request));
    }
}
