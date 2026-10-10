package com.example.library.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.library.common.result.Result;
import com.example.library.entity.Book;
import com.example.library.repository.BookRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "图书", description = "图书查询")
@RestController
@RequestMapping("/books")
@RequiredArgsConstructor
public class BookController {

    private final BookRepository bookRepository;

    @Operation(summary = "分页查询图书（支持按书名/作者关键词）")
    @GetMapping
    public Result<Page<Book>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {

        LambdaQueryWrapper<Book> q = new LambdaQueryWrapper<Book>()
                .like(keyword != null && !keyword.isBlank(), Book::getTitle, keyword)
                .or()
                .like(keyword != null && !keyword.isBlank(), Book::getAuthor, keyword)
                .eq(categoryId != null, Book::getCategoryId, categoryId)
                .orderByDesc(Book::getCreateTime);

        Page<Book> result = bookRepository.page(new Page<>(page, size), q);
        return Result.success(result);
    }
}
