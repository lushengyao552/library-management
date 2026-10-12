package com.example.library.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.library.common.result.Result;
import com.example.library.entity.AiCallLog;
import com.example.library.mapper.AiCallLogMapper;
import com.example.library.util.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "Token 账本", description = "模型调用与费用统计")
@RestController
@RequestMapping("/token-ledger")
@RequiredArgsConstructor
public class TokenLedgerController {

    private final AiCallLogMapper aiCallLogMapper;

    @Operation(summary = "我的 Token 账本：总调用、总 tokens、总费用、节省比例")
    @GetMapping("/mine")
    public Result<Map<String, Object>> mine() {
        Long userId = UserContext.getCurrentUserId();
        List<AiCallLog> logs = aiCallLogMapper.selectList(
                new LambdaQueryWrapper<AiCallLog>().eq(AiCallLog::getUserId, userId));

        long calls = logs.size();
        long totalTokens = logs.stream().mapToLong(l -> l.getTotalTokens() == null ? 0 : l.getTotalTokens()).sum();
        BigDecimal cost = logs.stream()
                .map(l -> l.getCost() == null ? BigDecimal.ZERO : l.getCost())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 节省比例：MOCK/WEAK 档省下的钱 / 如果全用 STRONG 的理论费用
        double theoreticalStrongCost = logs.stream()
                .mapToDouble(l -> (l.getTotalTokens() == null ? 0 : l.getTotalTokens()) / 1000.0 * 0.0056)
                .sum();
        double savedRatio = theoreticalStrongCost > 0
                ? 1 - cost.doubleValue() / theoreticalStrongCost : 0;

        Map<String, Object> r = new HashMap<>();
        r.put("calls", calls);
        r.put("totalTokens", totalTokens);
        r.put("costYuan", cost);
        r.put("savedRatio", Math.max(0, Math.round(savedRatio * 10000) / 100.0)); // 百分比
        return Result.success(r);
    }
}
