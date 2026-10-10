package com.example.library.service.ai.tool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.library.entity.Book;
import com.example.library.entity.BorrowRecord;
import com.example.library.repository.BookRepository;
import com.example.library.repository.BorrowRecordRepository;
import com.example.library.util.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 图书相关工具：供 LLM 调用。
 * 注意：getActiveBorrows 的 userId 从后端登录态拿，不信任 LLM 传的参数。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookTools {

    private final BookRepository bookRepository;
    private final BorrowRecordRepository borrowRecordRepository;

    @Tool(description = "根据书名关键词或分类搜索图书，返回匹配的图书列表")
    public List<BookResult> searchBooks(
            @ToolParam(description = "书名关键词，模糊匹配") String keyword,
            @ToolParam(description = "分类ID，可选，不传则不按分类筛选", required = false) Long categoryId) {

        LambdaQueryWrapper<Book> q = new LambdaQueryWrapper<Book>()
                .like(keyword != null && !keyword.isBlank(), Book::getTitle, keyword)
                .or()
                .like(keyword != null && !keyword.isBlank(), Book::getAuthor, keyword)
                .eq(categoryId != null, Book::getCategoryId, categoryId)
                .orderByDesc(Book::getCreateTime)
                .last("LIMIT 10");

        List<Book> books = bookRepository.list(q);
        return books.stream().map(b -> new BookResult(
                b.getId(), b.getTitle(), b.getAuthor(), b.getPublisher(), b.getStock()
        )).collect(Collectors.toList());
    }

    @Tool(description = "查询当前登录用户正在借阅（未归还）的图书记录")
    public List<BorrowResult> getActiveBorrows() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("未登录");
        }
        List<BorrowRecord> records = borrowRecordRepository.list(
                new LambdaQueryWrapper<BorrowRecord>()
                        .eq(BorrowRecord::getUserId, userId)
                        .in(BorrowRecord::getStatus, "BORROWED", "OVERDUE")
        );
        return records.stream().map(r -> new BorrowResult(
                r.getBookId(), r.getBorrowDate(), r.getDueDate(), r.getStatus(), r.getFine()
        )).collect(Collectors.toList());
    }

    // === 返回 DTO（只暴露 LLM 需要的字段）===

    public record BookResult(Long id, String title, String author, String publisher, Integer stock) {}
    public record BorrowResult(Long bookId, String borrowDate, String dueDate, String status, java.math.BigDecimal fine) {}
}
