package com.example.library.repository;

import com.baomidou.mybatisplus.spring.service.IService;
import com.example.library.entity.BookCategory;

public interface BookCategoryRepository extends IService<BookCategory> {
    Long countByCategoryId(Long categoryId);
}
