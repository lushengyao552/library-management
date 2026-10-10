# 智学校园 —— 基于大模型与知识检索的校园智能学习 Agent

移动云模型应用赛参赛项目。基于 Spring Boot + MoMA（OpenAI 兼容接口）+ Tool Calling + RAG，把图书管理系统升级为校园 AI 学习助手。

## 技术栈

- Java 17 + Spring Boot 4.1.1
- MyBatis-Plus 3.5.17（boot4-starter）
- MySQL 8.x
- Redis（Docker）
- Spring AI 2.0.1（ChatClient + Tool Calling）
- Knife4j Next 5.7.3（API 文档）
- JWT 鉴权

## 快速启动

### 1. 准备环境
- MySQL 8.x（端口 3306，库名 `library_db`）
- Redis（Docker，端口 6379）

### 2. 初始化数据库
```bash
mysql -u root -p < src/main/resources/db/schema.sql
```

### 3. 配置环境变量（可选）
```bash
# 不配则走 Mock 模式，应用也能启动
setx AI_API_KEY "你的MoMA/OpenAI Key"
setx AI_BASE_URL "https://your-moma-endpoint"
setx AI_MODEL "deepseek-v3"
```

### 4. 启动
```bash
mvn spring-boot:run
```

访问：`http://localhost:8080/api/doc.html`

## 核心接口

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/auth/login` | 登录拿 JWT |
| GET | `/books` | 分页查图书（支持 keyword） |
| GET | `/borrows/active` | 我的未还借阅 |
| GET | `/borrows/history` | 我的借阅历史 |
| POST | `/ai/chat` | AI 聊天（自动调用 Tool） |
| GET | `/study-plans` | 我的学习计划 |
| GET | `/study-plans/{id}/tasks` | 计划下的任务 |
| POST | `/study-plans/tasks/{id}/done` | 标记任务完成 |

## AI Tool 列表

LLM 可调用的工具：

| 工具 | 作用 |
|---|---|
| `searchBooks(keyword, categoryId)` | 查 MySQL 图书库 |
| `getActiveBorrows()` | 查当前用户未还借阅 |
| `searchKnowledge(query)` | 检索校园规章/FAQ（RAG） |
| `createStudyPlan(subject, examDate, dailyHours, weakness)` | 创建学习计划并拆任务 |
| `listMyPlans()` | 查我的学习计划 |

## 测试账号
- admin / admin123（管理员）
- user / user123（普通用户）
- zhangsan / user123（普通用户）
