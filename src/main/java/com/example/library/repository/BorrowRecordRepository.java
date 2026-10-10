package com.example.library.repository;

import com.baomidou.mybatisplus.extension.repository.IRepository;
import com.example.library.entity.BorrowRecord;

public interface BorrowRecordRepository extends IRepository<BorrowRecord> {
    int countBorrowingByUserId(Long userId);
    int countByUserAndBook(Long userId, Long bookId);
}
