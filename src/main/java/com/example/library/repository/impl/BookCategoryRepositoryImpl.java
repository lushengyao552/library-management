package com.example.library.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.repository.AbstractRepository;
import com.example.library.entity.Book;
import com.example.library.entity.BookCategory;
import com.example.library.mapper.BookCategoryMapper;
import com.example.library.mapper.BookMapper;
import com.example.library.repository.BookCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class BookCategoryRepositoryImpl extends AbstractRepository<BookCategoryMapper, BookCategory> implements BookCategoryRepository {

    private final BookMapper bookMapper;

    @Override
    public Long countByCategoryId(Long categoryId) {
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Book::getCategoryId, categoryId);
        return bookMapper.selectCount(wrapper);
    }
}
