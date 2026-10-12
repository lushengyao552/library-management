-- ============================================================
-- 智学伴 v2 迁移：ai_call_log 加档位/场景/费用/兜底列
-- 对已存在的 library_db 执行
-- ============================================================
USE library_db;

ALTER TABLE ai_call_log
  ADD COLUMN tier VARCHAR(20) NOT NULL DEFAULT 'STRONG' COMMENT '档位 STRONG/WEAK/MOCK' AFTER model,
  ADD COLUMN scene VARCHAR(30) NOT NULL DEFAULT 'CHAT' COMMENT '业务场景' AFTER tier,
  ADD COLUMN cost DECIMAL(10,6) NOT NULL DEFAULT 0 COMMENT '本次费用(元)' AFTER total_tokens,
  ADD COLUMN fallback TINYINT NOT NULL DEFAULT 0 COMMENT '是否模板兜底';

-- knowledge_document / book 加全文索引（ngram 支持中文）
ALTER TABLE knowledge_document ADD FULLTEXT INDEX ft_content (title, content) WITH PARSER ngram;
ALTER TABLE book ADD FULLTEXT INDEX ft_book (title, author, description) WITH PARSER ngram;

-- ============================================================
-- 新增：学习点表
-- ============================================================
CREATE TABLE IF NOT EXISTS learning_point (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id         BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    seq             INT NOT NULL,
    title           VARCHAR(200) NOT NULL,
    summary         TEXT,
    weight          DECIMAL(3,1) DEFAULT 1.0,
    must_learn     TINYINT DEFAULT 0,
    prereq_json    JSON,
    matched_book_id BIGINT,
    matched_chapter VARCHAR(100),
    est_hours       DECIMAL(3,1) DEFAULT 1.0,
    scheduled_date  DATE,
    status          VARCHAR(20) DEFAULT 'PENDING',
    mastery_score   INT DEFAULT 0,
    fsrs_state      JSON,
    last_study_date DATE,
    fallback        TINYINT DEFAULT 0,
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT DEFAULT 0,
    KEY idx_plan (plan_id),
    KEY idx_user (user_id),
    KEY idx_scheduled (scheduled_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习点';

-- 新增：调整方案表（两步确认）
CREATE TABLE IF NOT EXISTS adjustment_proposal (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id      BIGINT NOT NULL,
    user_id      BIGINT NOT NULL,
    trigger_type VARCHAR(30) NOT NULL,
    reason       TEXT,
    proposal     JSON NOT NULL,
    status       VARCHAR(20) DEFAULT 'PENDING_CONFIRM',
    create_time  DATETIME DEFAULT CURRENT_TIMESTAMP,
    execute_time DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='调整方案';

-- 新增：验收记录表
CREATE TABLE IF NOT EXISTS review_record (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id     BIGINT NOT NULL,
    user_id     BIGINT NOT NULL,
    point_id    BIGINT,
    review_type VARCHAR(20) NOT NULL,
    user_input  TEXT,
    ai_feedback TEXT,
    score       INT,
    passed      TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='验收记录';
