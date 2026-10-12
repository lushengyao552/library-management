package com.example.library.repository.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.example.library.entity.Book;
import com.example.library.mapper.BookMapper;
import com.example.library.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class BookRepositoryImpl extends ServiceImpl<BookMapper, Book> implements BookRepository {

    private final BookMapper bookMapper;

    @Override
    public int decreaseStock(Long bookId, Integer count) {
        return bookMapper.decreaseStock(bookId, count);
    }

    @Override
    public int increaseStock(Long bookId, Integer count) {
        return bookMapper.increaseStock(bookId, count);
    }
}
