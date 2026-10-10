package com.example.library.repository;

import com.baomidou.mybatisplus.extension.repository.IRepository;
import com.example.library.entity.Book;

public interface BookRepository extends IRepository<Book> {
    int decreaseStock(Long bookId, Integer count);
    int increaseStock(Long bookId, Integer count);
}
