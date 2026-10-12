INSERT INTO knowledge_document (title, content, doc_type, source) VALUES
('Java面向对象', 'Java面向对象三大特性：封装、继承、多态。封装用private修饰属性，public方法访问；继承用extends关键字；多态通过父类引用指向子类实现。', 'NOTE', 'Java核心技术'),
('JVM内存结构', 'JVM运行时数据区分为堆、栈、方法区、程序计数器、本地方法栈。堆是GC主要区域，栈存放局部变量表。', 'NOTE', '深入理解Java虚拟机'),
('Spring Boot自动配置', 'Spring Boot通过@SpringBootApplication注解组合了@Configuration、@EnableAutoConfiguration、@ComponentScan。自动配置基于spring.factories和条件注解。', 'NOTE', 'Spring实战');
