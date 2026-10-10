package com.example.library.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.library.common.result.Result;
import com.example.library.entity.BorrowRecord;
import com.example.library.repository.BorrowRecordRepository;
import com.example.library.util.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "借阅", description = "借阅记录查询")
@RestController
@RequestMapping("/borrows")
@RequiredArgsConstructor
public class BorrowController {

    private final BorrowRecordRepository borrowRecordRepository;

    @Operation(summary = "查询我当前未还的借阅")
    @GetMapping("/active")
    public Result<List<BorrowRecord>> myActiveBorrows() {
        Long userId = UserContext.getCurrentUserId();
        List<BorrowRecord> list = borrowRecordRepository.list(
                new LambdaQueryWrapper<BorrowRecord>()
                        .eq(BorrowRecord::getUserId, userId)
                        .in(BorrowRecord::getStatus, "BORROWED", "OVERDUE")
                        .orderByDesc(BorrowRecord::getBorrowDate)
        );
        return Result.success(list);
    }

    @Operation(summary = "查询我的借阅历史")
    @GetMapping("/history")
    public Result<List<BorrowRecord>> myHistory() {
        Long userId = UserContext.getCurrentUserId();
        List<BorrowRecord> list = borrowRecordRepository.list(
                new LambdaQueryWrapper<BorrowRecord>()
                        .eq(BorrowRecord::getUserId, userId)
                        .orderByDesc(BorrowRecord::getBorrowDate)
        );
        return Result.success(list);
    }
}
