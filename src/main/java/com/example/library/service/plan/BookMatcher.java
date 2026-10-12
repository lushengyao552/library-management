package com.example.library.service.plan;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.library.entity.Book;
import com.example.library.mapper.BookMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 配书器：根据学习点标题关键词，在 book 表找最匹配的一本书 + 推断章节。
 * 先用 LIKE，后续可升级 MATCH AGAINST。
 */
@Component
@RequiredArgsConstructor
public class BookMatcher {

    private final BookMapper bookMapper;

    /**
     * @param pointTitle 学习点标题，如 "JVM入门"
     * @param categoryId 场景对应的书分类
     * @return [bookId, chapter]，没配到返回 [null, null]
     */
    public Object[] match(String pointTitle, Long categoryId) {
        if (pointTitle == null || pointTitle.isBlank()) return new Object[]{null, null};

        // 关键词映射：学习点标题 → 书标题关键词
        String kw = pointTitle;
        if (pointTitle.contains("JVM") || pointTitle.contains("虚拟机")) kw = "虚拟机";
        else if (pointTitle.contains("Spring")) kw = "Spring";
        else if (pointTitle.contains("集合")) kw = "Java核心";
        else if (pointTitle.contains("异常") || pointTitle.contains("IO")) kw = "Java核心";
        else if (pointTitle.contains("面向对象") || pointTitle.contains("基础语法")) kw = "Java核心";
        else if (pointTitle.contains("算法")) kw = "算法";
        else if (pointTitle.contains("代码整洁")) kw = "整洁";

        try {
            LambdaQueryWrapper<Book> q = new LambdaQueryWrapper<Book>()
                    .like(Book::getTitle, kw)
                    .eq(categoryId != null, Book::getCategoryId, categoryId)
                    .last("LIMIT 1");
            Book b = bookMapper.selectOne(q);
            if (b == null) {
                // 不限分类再试一次
                b = bookMapper.selectOne(new LambdaQueryWrapper<Book>()
                        .like(Book::getTitle, kw).last("LIMIT 1"));
            }
            if (b != null) {
                return new Object[]{b.getId(), "第1章 " + pointTitle};
            }
        } catch (Exception ignored) {}
        return new Object[]{null, null};
    }
}
