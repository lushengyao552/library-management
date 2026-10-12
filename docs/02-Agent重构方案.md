# 智学伴 · Agent 化重构方案（从"工作流"到"智能体"）

> 比赛：第五届「移动云杯」智算应用创新大赛 · 模型应用赛
> 状态：待确认
> 编写日期：2026-10-12
> 前置文档：`docs/01-技术方案.md`（现有"固定流水线"方案）

---

## 0. 一句话结论

现有方案把大模型当**「JSON 生成器」**，把排期 / 复习 / 掌握度写成**死代码**——评委一眼就能看出"这不是智能体，是带 AI 的排期工具"。

本方案把模型从"配角"扶正为**「决策大脑」**：

> **模型自主决定每一步做什么；确定性算法降级为它可调用的工具；对话记忆让它能"记住用户、实时调整"。**

---

## 1. 现状诊断（为什么现在不是 Agent）

### 1.1 现有代码的真实定性

| 文件 | 现状 | 问题 |
|---|---|---|
| `PlanPipeline.java` | 写死 5 步：诊断→拆点→排期→配书→落库，每步是固定 prompt + 解析固定 JSON | 模型被当"文本生成器"，流程由代码定死，用户想法无法插进来 |
| `Scheduler.java` / `FsrsCalculator.java` / `MasteryCalculator.java` | 排期、复习、掌握度全是纯 Java 规则 | 真正的"智能"部分反而和模型无关 |
| `BookMatcher.java` | 关键词 `LIKE` 匹配 | 传统算法 |
| `AiChatService.java` | 单轮聊天 + `.tools(bookTools, knowledgeTools, studyPlanTools)` | **唯一的 agent 种子**，但单轮、无记忆、且和规划流程割裂 |
| `StudyPlanTools.java` | `createStudyPlan` 是写死"前 70% 学新、后 30% 复习"的字符串拼接 | 旧逻辑，和新 `Scheduler` 是两套 |
| `AdjustmentController.java` | AI 出方案，`execute` 是空壳 | "两步确认"执行侧没落地 |

### 1.2 一句话定性

> 现有作品 = **LLM 辅助的固定工作流（workflow）+ 规则引擎**，不是**智能体（Agent）**。

关键区别：

| | 工作流（现在） | 智能体（目标） |
|---|---|---|
| 谁决定下一步 | 代码写死顺序 | **模型自己决定** |
| 用户角色 | 填表单参数 | **对话、随时打断/纠正/提新想法** |
| 状态 | 跑完就完 | **有记忆，持续感知学习状态** |
| 遇到变化 | 重跑固定流程 | **实时重新规划（ReAct 循环）** |

---

## 2. 目标架构

```
                         ┌─────────────────────────────────────────────┐
                         │         Planner Agent（模型 = 大脑）         │
                         │   system: 你是学习规划师，有这些工具，       │
                         │   自主完成诊断/规划/排期/配书/验收/重排，     │
                         │   记住用户说过的话，随时响应用户的新想法。    │
                         │                                             │
                         │   ReAct 循环：观察状态 → 思考 → 调工具 →     │
                         │              看结果 → 再调整                │
                         └───────────────┬─────────────────────────────┘
                                          │ Tool Calling（自主选择调哪个）
      ┌───────────────────────────────────┼───────────────────────────────┐
      ▼            ▼            ▼         ▼         ▼            ▼         ▼
  ┌────────┐ ┌──────────┐ ┌──────────┐ ┌──────┐ ┌────────┐ ┌────────┐ ┌─────────┐
  │诊断    │ │拆学习点   │ │排期      │ │配书  │ │验收    │ │重排    │ │检索     │
  │diagnose│ │splitPoint│ │schedule  │ │match │ │grade   │ │propose │ │search   │
  │(模型)  │ │(模型+校验)│ │(纯Java)  │ │(代码)│ │(模型)  │ │(模型+  │ │(代码)   │
  │        │ │          │ │          │ │      │ │        │ │ 代码)  │ │         │
  └────────┘ └──────────┘ └──────────┘ └──────┘ └────────┘ └────────┘ └─────────┘
                                          │
                      记忆层：短期(近N条 chat_message) + 长期(学习画像/计划状态)
                                          │
                      数据层：MySQL(plan/point/review/…) + Redis(画像/状态缓存)
```

**核心变化**：不再是 `PlanPipeline` 一个类写死 5 步；而是模型在对话里**自主决定**"现在该出题 / 该排期 / 该配书 / 该重排 / 该答疑"。

## 3. 核心改造点

### 3.1 模型当大脑：统一的 Planner Agent 入口

- 新增 `PlannerAgentService`，作为**唯一对话入口**，替代 `PlanPipeline` 的固定编排。
- 用 Spring AI 的 **Tool Calling + 多轮对话**，让模型自主选择工具。
- `AiChatService` 从"单轮 + 3 个工具"升级为"多轮 + 记忆 + 全量学习工具"。
- 现有 REST 向导接口（`/plans/diagnosis`、`/plans/preview`…）**保留**，但定位降级为"前端快捷方式 / 老页兼容"；主入口收敛到 `/ai/chat`。

### 3.2 工具当手脚：把确定性算法包装成 `@Tool`

- 把 `Scheduler`、`BookMatcher`、`FsrsCalculator`、`MasteryCalculator` 全部包成 `@Tool`，供模型**在需要时调用**，而不是主流程直接调。
- 好处：模型是决策者，算法是"手脚"，符合"agent 决策 + 工具兜底"的主流范式（既显得智能，又可靠可验收）。

### 3.3 记忆当状态：短期 + 长期

- **短期**：把 `chat_message` 近 N 条（默认 10）喂回给模型（用 Spring AI 的记忆 advisor，具体类名见 §11 查证项）。
- **长期**：用户画像（目标、场景、偏好、历史表现）+ 当前计划状态摘要（各学习点掌握度 / 打卡 / 复习积压 / 借书到期），缓存 Redis、落库 MySQL。

### 3.4 确定性兜底：模型可以"聪明"，但不能"乱来"

- **JSON 校验**：模型拆学习点必须输出纯 JSON，代码校验必填字段 / 类型 / 数量上限；失败重试 1 次，再失败走模板兜底并打 `fallback=true`（沿用 01 方案）。
- **排期用代码**：日期不交给模型排（不可复现、不可控），模型只调 `schedulePlan` 工具，由 `Scheduler` 算。
- **写库必须两步确认**：模型只能"出方案"（`proposeAdjustment`），真正改 `learning_point.scheduled_date` 等业务数据必须经用户确认后由代码执行。

---

## 4. 工具清单（@Tool）

| 工具名 | 作用 | 智能来源 | 是否写库 |
|---|---|---|---|
| `diagnose` | 出诊断题 | 模型 | 否 |
| `splitLearningPoints` | 目标 → 学习点（含 weight / mustLearn / prereq / estHours） | 模型 + 代码校验 | 否 |
| `schedulePlan` | 排期（倒排 / 拓扑 / 压缩 / FSRS / 借书约束） | 纯 Java `Scheduler` | 否 |
| `matchBook` | 学习点配书 + 章节 | 代码（`BookMatcher`） | 否 |
| `previewPlan` | 汇总计划预览给用户 | 代码 | 否 |
| `savePlan` | 确认后落库 plan / point | 代码 | **是** |
| `getPlanStatus` | 查计划进度 / 掌握度 / 复习积压 / 借书到期 | 代码 | 否 |
| `generateQuiz` | 出小测题 | 模型 | 否 |
| `gradeReview` | 练习 / 复述 AI 点评打分 | 模型 | 否 |
| `proposeAdjustment` | 生成重排方案（PENDING_CONFIRM） | 模型 + 代码 | **是**（insert proposal） |
| `executeAdjustment` | 用户确认后真正改业务数据 | 代码 | **是** |
| `searchBooks` / `getActiveBorrows` / `searchKnowledge` | 检索（已有） | 代码 | 否 |

> 说明：`diagnose` / `splitLearningPoints` / `generateQuiz` / `gradeReview` 背后走统一 `ModelGateway`（STRONG 档），继续记 Token 账本。

---

## 5. 记忆设计

| 类型 | 内容 | 存储 | 注入方式 |
|---|---|---|---|
| 短期记忆 | 近 10 条 `chat_message`（一问一答 + 工具结果摘要） | `chat_message` 表（已有） | 记忆 advisor 自动拼入 |
| 长期·画像 | 目标、场景 code、偏好（"讨厌刷题"）、日均时长 | Redis（key: `profile:{userId}`）+ 必要时落 `sys_user` 扩展 | prompt 里拼"用户画像"段 |
| 长期·状态 | 当前计划各学习点 `masteryScore` / `status` / 复习积压 / 借书到期 | 实时查 `learning_point` / `borrow_record` | `getPlanStatus` 工具返回 |

---

## 6. 实时调整机制（本方案的命脉 / 卖点）

改造后，用户**任何一句话**都能触发模型实时重规划，而不是重跑固定表单：

| 用户输入 | Agent 行为 |
|---|---|
| "我最近太忙，每天只能 1 小时" | 调 `getPlanStatus` 看现状 → 调 `schedulePlan` 重排 → 预览 → 确认 |
| "今天这个知识点太难了" | 调 `proposeAdjustment` 生成"降难度 / 拆更细 / 补前置"方案 → 确认 |
| "下周突然有个面试" | 识别目标变化 → `schedulePlan` 切冲刺模式（压缩低权重、前置冲刺） |
| "这本书快到期了" | 调 `getActiveBorrows` + `getPlanStatus` → 提醒续借或 `schedulePlan` 赶在到期前 |
| 验收不及格 | 模型反思原因（哪类题错、是否前置没学）→ `proposeAdjustment` |

**关键**：这是"agent 实时决策 + 两步确认写库"，模型**不直接改业务表**，但**决策权在模型**——这正是评委要看的"智能体"。

## 7. 数据模型改动（尽量复用，少建表）

| 改动 | 说明 |
|---|---|
| `chat_message` | 已有，用作短期记忆；补一条"工具调用结果"落库规则（`messageType=tool_result`，`toolName` 记工具名） |
| `chat_session` | 已有，增加 `agent_phase`（诊断中 / 规划中 / 等待确认 / 答疑中），让多轮对话有状态 |
| `sys_user` | 可选加 `profile_json`（学习画像），MVP 阶段先放 Redis，不急着改表 |
| `adjustment_proposal` | 已有，`proposal` 字段存结构化 JSON（改哪些 point、挪到哪天、原因），`executeAdjustment` 按它真正改库 |
| 其余 | 不动 |

> MVP 原则：**不建新表、不动主业务表结构**，先跑通命脉；`agent_phase` 若不想改表，也可暂存 Redis。

---

## 8. 接口清单（改造后）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/ai/chat` | **Agent 主入口**：多轮 + 工具 + 记忆（改造 `AiChatService`） |
| POST | `/ai/qa` | 资料问答（保留，记忆注入改为真实现） |
| GET | `/plans/scenarios` 等 | 保留：前端向导 / 老页兼容 |
| GET | `/token-ledger/mine` | 保留：Token 账本 |

---

## 9. 与现有代码的映射（保留 / 降级 / 删除）

| 现有代码 | 处置 | 去向 |
|---|---|---|
| `Scheduler` / `FsrsCalculator` / `MasteryCalculator` / `BookMatcher` | **保留**（纯算法不动） | 包成 `@Tool` 供模型调用 |
| `PlanPipeline.java` | **拆解** | 编排逻辑删除；"拆点 / 排期 / 配书 / 落库"拆成独立工具；"JSON 校验重试 + 兜底"下沉到工具内 |
| `AiChatService.java` | **升级** | 单轮 → 多轮；3 工具 → 全量工具；接记忆 advisor |
| `StudyPlanTools.java`（旧写死拆任务） | **删除 / 重写** | 由 `schedulePlan` 工具取代 |
| `AdjustmentController.execute`（空壳） | **补全** | 由 `executeAdjustment` 工具真正按 proposal 改库 |
| `PlanController` / `ReviewController` / `QaController` | **保留** | 前端兼容；内部逐步改走 Agent 工具 |
| `ModelGateway` / `ModelGatewayRouter` / `MockModelGateway` | **保留** | 所有模型调用仍走统一网关 + 账本 |

---

## 10. 任务拆解与验收标准

> 每完成一个任务：`mvn test` 全绿才算完，并写进 `docs/进度.md`。

| 编号 | 任务 | 验收标准 |
|---|---|---|
| AG-0 | 查证 Spring AI 2.0.1 的记忆 advisor / 多轮 Tool Calling 确切类名与用法 | 产出 §11 查证结果，能写出可编译的示例 |
| AG-1 | 新增 `PlannerAgentService` + 把 `schedulePlan` / `matchBook` / `splitLearningPoints` 包成 `@Tool`，跑通"诊断→拆点→排期→配书→预览"（先单轮） | 一条对话能产出计划预览；`mvn test` 通过 |
| AG-2 | 多轮记忆：`chat_message` 近 10 条喂回 + `agent_phase` 状态 | 连续两轮，第二轮 prompt 能看到第一轮；能追问"改目标"后重排 |
| AG-3 | 实时重排：`getPlanStatus` + `proposeAdjustment` + `executeAdjustment` 真正按 proposal 改 `learning_point.scheduled_date` | 用户说"每天只能 1 小时"→ 生成方案→确认→点被重排；`execute` 后 scheduled_date 变化 |
| AG-4 | 前端对话式改造：聊天流 + 结构化卡片（计划预览 / 调整方案 / 学习点） | 手机网页能对话式完成"规划→改想法→重排→确认"全流程 |
| AG-5 | 补测试：工具 + 编排单测；Mock 模式全流程联调 | `mvn test` 全绿；无 key 也能演完整 Agent 流程 |
| AG-6 | 收尾：文档同步、进度更新、`docker compose up` 实测 | 一键起全栈，前端可对话 |

---

## 11. 技术坑 / 动手前查证（Spring AI 2.0.1）

| # | 坑 | 说明 / 待查证 |
|---|---|---|
| B1 | 多轮记忆 advisor | 2.0.1 是否仍有 `MessageChatMemoryAdvisor` + `ChatMemory`（内存版 / 自实现 Redis 版）——**AG-0 查证** |
| B2 | Tool 里取用户 | 已有正确姿势：`.advisors(a -> a.param("userId", userId))` + `ToolContext`，沿用 |
| B3 | 多轮工具结果回填 | 历史消息需按 `User/Assistant/ToolResponse` 正确拼回，防止模型"忘了刚查的结果" |
| B4 | 工具返回 DTO 序列化 | 用 `record` + 稳定字段，避免实体直接把 DB 大字段暴露给模型 |
| B5 | JSON 兜底 | 拆点 / 点评输出必须纯 JSON；重试 1 次 + 模板兜底（注意 Jackson 2/3 包与现有代码对齐） |

---

## 12. 变更记录

| 日期 | 内容 |
|---|---|
| 2026-10-12 | 初版：定性"现有是工作流非智能体"，给出 Agent 目标架构、工具清单、记忆、实时调整机制、任务拆解 AG-0~AG-6 |


