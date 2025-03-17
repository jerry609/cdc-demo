# Spring Boot RabbitMQ CDC 项目结构

**## 项目结构**

```
cdcdemo/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── blackhorse/
│   │   │           └── cdcdemo/
│   │   │               ├── CdcDemoApplication.java                # 应用程序入口（已更新支持异步）
│   │   │               ├── config/
│   │   │               │   ├── JacksonConfig.java                 # Jackson JSON 配置
│   │   │               │   ├── MybatisPlusConfig.java             # MyBatis Plus 配置
│   │   │               │   ├── RabbitMQConfig.java                # RabbitMQ 配置
│   │   │               │   └── RedisConfig.java                   # Redis 配置
│   │   │               ├── controller/
│   │   │               │   ├── CustomerController.java            # 客户 REST API 控制器
│   │   │               │   └── DataIntegrationController.java     # 数据集成 REST API 控制器
│   │   │               ├── listener/
│   │   │               │   └── DatabaseChangeListener.java        # 消息监听器
│   │   │               ├── model/
│   │   │               │   ├── Customer.java                      # 客户实体类
│   │   │               │   ├── DataChangeEvent.java               # 数据变更事件
│   │   │               │   └── integration/                       # 数据集成模型目录
│   │   │               │       ├── DataIntegrationRequest.java    # 数据集成请求模型
│   │   │               │       ├── IntegrationStatus.java         # 集成状态模型
│   │   │               │       └── IntegrationJob.java            # 集成作业模型
│   │   │               ├── publisher/
│   │   │               │   └── ChangeEventPublisher.java          # 事件发布服务
│   │   │               ├── mapper/
│   │   │               │   ├── CustomerMapper.java                # 客户 MyBatis Plus Mapper
│   │   │               │   └── IntegrationJobMapper.java          # 集成作业 Mapper
│   │   │               └── service/
│   │   │                   ├── CustomerService.java               # 客户业务逻辑服务
│   │   │                   └── DataIntegrationService.java        # 数据集成服务
│   │   └── resources/
│   │       ├── application.yml                                    # 应用配置文件
│   │       ├── application-dev.yml                                # 开发环境配置
│   │       ├── application-prod.yml                               # 生产环境配置
│   │       └── sql/                                               # SQL 脚本目录
│   │           └── integration_jobs.sql                           # 集成作业表创建脚本
│   └── test/
│       └── java/
│           └── com/
│               └── blackhorse/
│                   └── cdcdemo/
│                       ├── CdcDemoApplicationTests.java           # 应用测试类
│                       └── service/
│                           ├── CustomerServiceTests.java          # 客户服务测试类
│                           └── DataIntegrationServiceTests.java   # 数据集成服务测试类
└── pom.xml                                                        # Maven 配置文件
```

## 关键文件说明

### 1. 配置文件

#### `application.properties`
```properties
# 服务器配置
server.port=8080

# MySQL数据库配置
spring.datasource.url=jdbc:mysql://localhost:3306/cdcdemo?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=
spring.datasource.password=
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect

# Redis配置
spring.redis.host=localhost
spring.redis.port=6379
spring.redis.password=
spring.redis.database=0
spring.cache.type=redis
spring.cache.redis.time-to-live=60000
spring.cache.redis.cache-null-values=false

# RabbitMQ配置
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest

# JPA配置
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

### 2. 核心类

#### `CdcDemoApplication.java`
应用程序的主入口点，包含 `main` 方法。

#### `RabbitMQConfig.java`
配置 RabbitMQ 连接、交换机、队列和绑定。

#### `RedisConfig.java`
配置 Redis 连接和缓存管理。

#### `Customer.java`
定义客户实体类，使用 JPA 注解映射到数据库表。

#### `DataChangeEvent.java`
定义数据变更事件的数据结构。

#### `CustomerRepository.java`
定义数据访问接口，使用 Spring Data JPA 操作数据库。

#### `CustomerService.java`
实现业务逻辑，管理客户数据并发布变更事件。

#### `ChangeEventPublisher.java`
负责将变更事件发布到 RabbitMQ。

#### `DatabaseChangeListener.java`
监听 RabbitMQ 中的变更事件并执行相应的处理逻辑。

#### `CustomerController.java`
提供 REST API 接口，处理客户相关的 HTTP 请求。