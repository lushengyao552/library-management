package com.example.library.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("learning_point")
public class LearningPoint implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long planId;
    private Long userId;
    private Integer seq;
    private String title;
    private String summary;
    private BigDecimal weight;
    private Integer mustLearn;
    private String prereqJson;
    private Long matchedBookId;
    private String matchedChapter;
    private BigDecimal estHours;
    private LocalDate scheduledDate;
    private String status;
    private Integer masteryScore;
    private String fsrsState;
    private LocalDate lastStudyDate;
    private Integer fallback;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;
}
