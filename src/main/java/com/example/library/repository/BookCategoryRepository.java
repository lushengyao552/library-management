package com.example.library.repository;

import com.baomidou.mybatisplus.extension.repository.IRepository;
import com.example.library.entity.BookCategory;

public interface BookCategoryRepository extends IRepository<BookCategory> {
    Long countByCategoryId(Long categoryId);
}
