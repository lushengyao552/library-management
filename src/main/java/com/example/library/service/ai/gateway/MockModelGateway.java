package com.example.library.service.ai.gateway;

import com.example.library.mapper.AiCallLogMapper;
import com.example.library.entity.AiCallLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Mock 网关：无 MoMA key 时用，根据用户输入的目标关键词动态返回不同学习路径。
 * 不花钱，但 ai_call_log 照样落一条 tier=MOCK 的记录。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MockModelGateway implements ModelGateway {

    private final AiCallLogMapper aiCallLogMapper;

    @Override
    public ModelResult chat(ModelTier tier, String scene, String prompt, Long userId) {
        String text = switch (scene) {
            case "DIAGNOSIS" -> diagnosisFor(prompt);
            case "PLAN" -> planFor(prompt);
            case "REVIEW" -> """
                {"score":82,"feedback":"整体结构清晰，代码规范。建议补充边界处理，错误场景可以再完善。","passed":true}
                """;
            case "QUIZ" -> """
                {"questions":[{"stem":"以下哪个不是基本类型？","options":["int","String","boolean","char"],"answer_idx":1,"analysis":"String 是引用类型。"}]}
                """;
            default -> "[Mock] 收到：" + (prompt.length() > 60 ? prompt.substring(0, 60) + "..." : prompt);
        };
        log.info("[MockGateway] scene={}, tier={}", scene, tier);
        log("MOCK", scene, text, userId, 0, 0, 0.0);
        return new ModelResult(text, "mock", 0, 0, 0, 0.0, false);
    }

    private String diagnosisFor(String prompt) {
        String g = prompt.toLowerCase();
        if (g.contains("redis")) {
            return """
                {"questions":[
                {"stem":"你之前用过Redis吗？","options":["完全没","用过缓存命令","懂数据结构","搭过集群"],"answer_idx":1},
                {"stem":"你的后端语言是？","options":["Java","Python","Go","其他"],"answer_idx":0},
                {"stem":"最想解决什么问题？","options":["面试","项目加缓存","高并发","源码"],"answer_idx":1}]}
                """;
        }
        if (g.contains("前端") || g.contains("web") || g.contains("vue") || g.contains("react")) {
            return """
                {"questions":[
                {"stem":"你目前对 HTML/CSS/JS 基础掌握如何？","options":["完全没接触","写过静态页面","能写交互效果","很熟练"],"answer_idx":1},
                {"stem":"你每天能稳定学习多长时间？","options":["<0.5h","0.5-1h","1-2h",">2h"],"answer_idx":2},
                {"stem":"你更想做哪种项目练手？","options":["企业官网后台","小程序","数据可视化","都可以"],"answer_idx":3}]}
                """;
        }
        if (g.contains("python") || g.contains("爬虫") || g.contains("数据分析")) {
            return """
                {"questions":[
                {"stem":"你编程基础如何？","options":["零基础","会一点","有其他语言经验","很熟练"],"answer_idx":1},
                {"stem":"你的目标方向？","options":["爬虫","数据分析","AI/机器学习","Web开发"],"answer_idx":1},
                {"stem":"每天可学习时长？","options":["<1h","1-2h","2-3h",">3h"],"answer_idx":1}]}
                """;
        }
        if (g.contains("产品") || g.contains("pm") || g.contains("运营")) {
            return """
                {"questions":[
                {"stem":"你做过产品相关工作吗？","options":["完全没","实习过","做过1年","很有经验"],"answer_idx":1},
                {"stem":"最想提升哪块？","options":["需求分析","PRD撰写","项目管理","数据分析"],"answer_idx":2},
                {"stem":"每天学习时长？","options":["<1h","1-2h","2-3h"],"answer_idx":1}]}
                """;
        }
        return """
            {"questions":[
            {"stem":"你目前对Java基础语法掌握程度？","options":["完全没接触","写过小例子","能独立写业务代码","很熟练"],"answer_idx":1},
            {"stem":"你每天能稳定学习多长时间？","options":["<0.5h","0.5-1h","1-2h",">2h"],"answer_idx":2},
            {"stem":"你更希望哪种验收方式？","options":["选择题小测","提交代码让AI点评","读完自己复述","都可以"],"answer_idx":3}]}
            """;
    }

    private String planFor(String prompt) {
        String g = prompt.toLowerCase();
        if (g.contains("redis")) {
            return """
                {"points":[
                {"title":"Redis 安装与基础命令","summary":"Docker启动、redis-cli、KEYS/EXISTS/DEL、字符串命令SET/GET/INCR。参考 redis.io/learn。","weight":1.5,"must_learn":true,"prereq":[]},
                {"title":"五大基础数据结构","summary":"String/Hash/List/Set/ZSet 各自适用场景：ZSet做排行榜、Hash存对象、Set去重。参考《Redis设计与实现》黄健宏。","weight":2.0,"must_learn":true,"prereq":["Redis 安装与基础命令"]},
                {"title":"TTL与过期策略","summary":"EXPIRE/TTL/PERSIST、惰性删除+定期删除。手写一个带过期的缓存。","weight":1.0,"must_learn":true,"prereq":["五大基础数据结构"]},
                {"title":"持久化 RDB vs AOF","summary":"RDB快照 vs AOF追加日志 vs 混合持久化；生产为什么两个都开。","weight":1.5,"must_learn":false,"prereq":["TTL与过期策略"]},
                {"title":"缓存三大问题","summary":"缓存穿透(布隆过滤器)、击穿(互斥锁)、雪崩(随机过期)。面试高频。","weight":2.0,"must_learn":true,"prereq":["五大基础数据结构"]},
                {"title":"分布式锁","summary":"SETNX+EXPIRE、Redisson看门狗、锁续期、RedLock。","weight":1.5,"must_learn":false,"prereq":["缓存三大问题"]},
                {"title":"主从复制与哨兵","summary":"全量/增量复制、哨兵自动故障转移、脑裂问题。","weight":1.5,"must_learn":false,"prereq":["持久化 RDB vs AOF"]},
                {"title":"Redis Cluster 集群","summary":"16384哈希槽、数据分片、一致性哈希、Gossip协议。","weight":1.5,"must_learn":false,"prereq":["主从复制与哨兵"]},
                {"title":"Spring Boot 整合实战","summary":"Spring Data Redis + RedisTemplate + 缓存注解@Cacheable。给一个接口加缓存。","weight":2.0,"must_learn":true,"prereq":["TTL与过期策略"]},
                {"title":"综合实战：短链接+排行榜","summary":"用Redis做短链接(自增ID+Base62)和直播间排行榜(ZSet)。参考《Redis实战》。","weight":2.0,"must_learn":true,"prereq":["Spring Boot 整合实战","Redis Cluster 集群"]}]}
                """;
        }
        if (g.contains("前端") || g.contains("web") || g.contains("vue") || g.contains("react")) {
            return """
                {"points":[
                {"title":"HTML/CSS 基础","summary":"语义化标签、盒模型、Flex布局、响应式。目标：独立还原设计稿。","weight":2.0,"must_learn":true,"prereq":[]},
                {"title":"JavaScript 核心","summary":"变量类型、闭包、原型链、Promise、DOM操作。目标：能手写防抖节流。","weight":2.0,"must_learn":true,"prereq":["HTML/CSS 基础"]},
                {"title":"ES6+ 与模块化","summary":"let/const、箭头函数、解构、import/export、fetch。目标：用ES6重写待办清单。","weight":1.5,"must_learn":true,"prereq":["JavaScript 核心"]},
                {"title":"Vue3 框架","summary":"组合式API、ref/reactive、组件通信、路由、Pinia。目标：写一个SPA后台管理。","weight":2.0,"must_learn":true,"prereq":["ES6+ 与模块化"]},
                {"title":"HTTP 与跨域","summary":"GET/POST、状态码、Token、CORS。目标：能独立调通后端接口。","weight":1.0,"must_learn":true,"prereq":["JavaScript 核心"]},
                {"title":"综合项目：企业官网","summary":"整合响应式官网+后台登录。目标：部署到线上可访问。","weight":2.0,"must_learn":true,"prereq":["Vue3 框架","HTTP 与跨域"]}]}
                """;
        }
        if (g.contains("python") || g.contains("爬虫") || g.contains("数据分析")) {
            return """
                {"points":[
                {"title":"Python 基础语法","summary":"变量、控制流、函数、列表字典。目标：写百行脚本。","weight":2.0,"must_learn":true,"prereq":[]},
                {"title":"文件操作与异常","summary":"open/with、CSV/JSON、try-except。目标：批量处理日志文件。","weight":1.0,"must_learn":true,"prereq":["Python 基础语法"]},
                {"title":"requests 爬虫","summary":"GET/POST、Headers、BeautifulSoup解析。目标：爬取静态网页标题。","weight":1.5,"must_learn":true,"prereq":["文件操作与异常"]},
                {"title":"pandas 数据分析","summary":"DataFrame、groupby、merge、matplotlib。目标：分析一份销售数据。","weight":1.5,"must_learn":true,"prereq":["Python 基础语法"]},
                {"title":"FastAPI Web","summary":"路由、Pydantic模型、SQLite集成。目标：写一个增删改查API。","weight":1.5,"must_learn":false,"prereq":["Python 基础语法"]},
                {"title":"综合项目：数据看板","summary":"爬取+pandas分析+FastAPI展示。目标：跑通完整链路。","weight":2.0,"must_learn":true,"prereq":["requests 爬虫","pandas 数据分析"]}]}
                """;
        }
        if (g.contains("产品") || g.contains("pm") || g.contains("运营")) {
            return """
                {"points":[
                {"title":"产品经理认知","summary":"用户/需求/场景、PM工作流。目标：说清PM一天干什么。","weight":1.0,"must_learn":true,"prereq":[]},
                {"title":"需求分析","summary":"用户访谈、痛点挖掘、KANO模型、PRD骨架。目标：写一份需求文档大纲。","weight":2.0,"must_learn":true,"prereq":["产品经理认知"]},
                {"title":"原型与Axure","summary":"流程图、线框图、交互跳转。目标：画一个登录注册原型。","weight":1.5,"must_learn":true,"prereq":["需求分析"]},
                {"title":"数据指标体系","summary":"北极星指标、漏斗、留存、A/B测试。目标：给App设计指标树。","weight":1.5,"must_learn":false,"prereq":["产品经理认知"]},
                {"title":"综合产出：完整PRD","summary":"选一个常用App挑功能写完整PRD+原型。目标：可拿去面试。","weight":2.0,"must_learn":true,"prereq":["需求分析","原型与Axure"]}]}
                """;
        }
        return """
            {"points":[
            {"title":"Java开发环境与基础语法","summary":"JDK安装、IDEA配置、变量类型、流程控制。目标：写百行控制台程序。","weight":2.0,"must_learn":true,"prereq":[]},
            {"title":"面向对象编程","summary":"类与对象、封装继承多态、接口抽象类。目标：用OOP设计学生/订单类。","weight":2.0,"must_learn":true,"prereq":["Java开发环境与基础语法"]},
            {"title":"集合框架","summary":"ArrayList/HashMap/HashSet、泛型。目标：手写LRU缓存。","weight":1.5,"must_learn":true,"prereq":["面向对象编程"]},
            {"title":"MySQL基础与SQL","summary":"DDL/DML、索引、多表JOIN、事务。目标：三表联查+explain分析。","weight":2.0,"must_learn":true,"prereq":[]},
            {"title":"Spring Boot核心","summary":"IOC/DI、REST控制器、全局异常处理。目标：写CRUD接口。","weight":2.0,"must_learn":true,"prereq":["面向对象编程","MySQL基础与SQL"]},
            {"title":"MyBatis-Plus持久层","summary":"BaseMapper、条件构造器、分页插件。目标：用MP实现用户CRUD。","weight":1.5,"must_learn":true,"prereq":["Spring Boot核心"]},
            {"title":"综合项目：图书管理API","summary":"登录+CRUD+借阅流程。目标：接口通过Knife4j自测。","weight":2.0,"must_learn":true,"prereq":["Spring Boot核心","MyBatis-Plus持久层"]}]}
            """;
    }

    private void log(String model, String scene, String text, Long userId, int pt, int ct, double cost) {
        AiCallLog l = new AiCallLog();
        l.setUserId(userId);
        l.setModel(model);
        l.setTier("MOCK");
        l.setScene(scene);
        l.setPromptTokens(pt);
        l.setCompletionTokens(ct);
        l.setTotalTokens(pt + ct);
        l.setCost(java.math.BigDecimal.valueOf(cost));
        l.setStatus("SUCCESS");
        l.setUserQuery(text.length() > 500 ? text.substring(0, 500) : text);
        l.setToolNames("mock");
        aiCallLogMapper.insert(l);
    }
}
