package com.example.library.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("adjustment_proposal")
public class AdjustmentProposal {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long planId;
    private Long userId;
    private String triggerType;
    private String reason;
    private String proposal;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime executeTime;
}
