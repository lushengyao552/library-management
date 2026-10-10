package com.example.library.repository.impl;

import com.baomidou.mybatisplus.extension.repository.AbstractRepository;
import com.example.library.entity.BorrowRecord;
import com.example.library.mapper.BorrowRecordMapper;
import com.example.library.repository.BorrowRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class BorrowRecordRepositoryImpl extends AbstractRepository<BorrowRecordMapper, BorrowRecord> implements BorrowRecordRepository {

    private final BorrowRecordMapper borrowRecordMapper;

    @Override
    public int countBorrowingByUserId(Long userId) {
        return borrowRecordMapper.countBorrowingByUserId(userId);
    }

    @Override
    public int countByUserAndBook(Long userId, Long bookId) {
        return borrowRecordMapper.countByUserAndBook(userId, bookId);
    }
}
