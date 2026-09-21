# 企业级 API 开放平台技术方案

> **技术栈概览**：Spring Boot 3.x · MyBatis-Flex · Spring Security + JWT · Spring Cloud Gateway · Caffeine + Redis · Vue 3 + TypeScript + Vite + Ant Design Vue + Pinia

## 一、架构总览

```text
┌─────────────────────────────────────────────────────────────┐
│                  开发者门户 / 运营管理后台                      │
│         Vue 3 + TypeScript + Vite + Ant Design Vue           │
│         API 文档 · 应用管理 · 密钥管理 · 调用统计 · 审批流      │
└──────────────────────────┬──────────────────────────────────┘
                           │ HTTPS + JWT
┌──────────────────────────▼──────────────────────────────────┐
│                 网关层 (Spring Cloud Gateway)                 │
│    路由转发 · 限流 · 熔断 · 鉴权前置 · 请求日志 · 签名校验      │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│             管理服务 (Spring Boot 3 + MyBatis-Flex)           │
│    用户管理 · API 元数据 · 权限校验 · 密钥管理 · 统计报表       │
│    多数据源（管理库 / 日志库） · Caffeine + Redis 多级缓存     │
└──────────────────────────┬──────────────────────────────────┘
                           │
        ┌──────────────────┼──────────────────┐
        ▼                  ▼                  ▼
   管理库 (MySQL)      日志库 (MySQL)       Redis (缓存/限流)
```

## 二、后端技术栈

### 2.1 核心依赖

**Spring Boot 3.x 必须使用专用 Starter**：

```xml
<properties>
    <spring-boot.version>3.3.x</spring-boot.version>
    <mybatis-flex.version>1.9.5</mybatis-flex.version>
    <jjwt.version>0.12.x</jjwt.version>
</properties>

<!-- MyBatis-Flex Spring Boot 3 专用 Starter -->
<dependency>
    <groupId>com.mybatis-flex</groupId>
    <artifactId>mybatis-flex-spring-boot3-starter</artifactId>
    <version>${mybatis-flex.version}</version>
</dependency>

<!-- 连接池：HikariCP（Spring Boot 默认） -->
<dependency>
    <groupId>com.zaxxer</groupId>
    <artifactId>HikariCP</artifactId>
</dependency>

<!-- Spring Security + JWT -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>${jjwt.version}</version>
</dependency>

<!-- 多级缓存 -->
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

> ⚠️ **关键约束**：不要引入 `mybatis-spring-boot-starter` 或 `mybatis-plus-boot-starter`，否则会导致 MyBatis 被优先初始化，MyBatis-Flex 无法加载。

### 2.2 MyBatis-Flex APT 配置

MyBatis-Flex 通过 APT（Annotation Processing Tool）在编译期生成 `TableDef` 辅助类，实现类型安全的查询条件构建，避免手写字段名出错。

在项目根目录（`pom.xml` 所在目录）创建 `mybatis-flex.config`：

```properties
# 开启 TableDef 生成（默认已开启）
processor.enable=true

# 生成 Tables 聚合类（可选，将所有 TableDef 聚合到一个类中）
processor.allInTables.enable=true

# 自定义 TableDef 包路径：实体包名下的 table 子包
processor.tableDef.package=${entityPackage}.table

# Mapper 生成（默认关闭）
processor.mapper.generateEnable=false
```



编译后，`Account.java` 会生成 `AccountTableDef.java`，其中包含 `ACCOUNT` 静态常量，用于类型安全的查询构建。

### 2.3 多数据源配置（管理库 + 日志库分离）

API 开放平台通常需要将**管理数据**（API 元数据、用户、密钥）与**日志数据**（调用记录、统计）分离存储。MyBatis-Flex **内置多数据源支持**，无需第三方插件。

**application.yml**：

```yaml
mybatis-flex:
  datasource:
    master:
      type: com.zaxxer.hikari.HikariDataSource
      url: jdbc:mysql://master-db:3306/api_platform_mgr
      username: ${DB_USER}
      password: ${DB_PASSWORD}
    log:
      type: com.zaxxer.hikari.HikariDataSource
      url: jdbc:mysql://log-db:3306/api_platform_log
      username: ${DB_USER}
      password: ${DB_PASSWORD}
```



**切换数据源的四种方式**：

1. **编码方式**：`DataSourceKey.use("log", () -> { ... })`
2. **Mapper 类级别**：`@UseDataSource("log")` 标注在 Mapper 接口上
3. **Mapper 方法级别**：`@UseDataSource("log")` 标注在方法上
4. **Entity 级别**：`@Table(dataSource = "log")` 标注在实体类上

### 2.4 Spring Security + JWT 无状态认证

**JWT 认证流程**：

1. 用户登录 → 服务端生成 JWT 并返回
2. 客户端携带 `Authorization: Bearer <token>` 访问接口
3. 网关或过滤器解析并验证 JWT 签名和有效期
4. 验证通过后将用户信息注入请求头，供下游服务使用

**自定义过滤器核心逻辑**：

```java
// 解析 JWT，将用户信息注入请求头
ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
    .header("X-User-Id", claims.getSubject())
    .header("X-User-Roles", claims.get("roles").toString())
    .build();
return chain.filter(exchange.mutate().request(mutatedRequest).build());
```



**JWT 安全优化建议**：

- 访问令牌有效期建议控制在 15 分钟以内
- 生产环境强制 HTTPS 传输
- 使用 HttpOnly Cookie 存储令牌（防 XSS）
- 实施令牌绑定（将 JWT 与客户端指纹绑定）

## 三、网关层：Spring Cloud Gateway

Spring Cloud Gateway 是基于 Spring 5、Spring Boot 2 和 Project Reactor 的 API 网关，提供非阻塞的高性能处理。

### 3.1 路由配置示例

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: api-service
          uri: lb://api-service
          predicates:
            - Path=/openapi/**
          filters:
            - StripPrefix=1
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 100
                redis-rate-limiter.burstCapacity: 200
                key-resolver: "#{@apiKeyResolver}"
            - name: CircuitBreaker
              args:
                name: apiCircuitBreaker
                fallbackUri: forward:/fallback
```



### 3.2 关键过滤器

**限流（RequestRateLimiter）** ：基于令牌桶算法，默认使用 Redis 存储限流计数。需自定义 `KeyResolver` 来决定限流维度：

```java
@Component("apiKeyResolver")
public class ApiKeyResolver implements KeyResolver {
    @Override
    public Mono<String> resolve(ServerWebExchange exchange) {
        return Mono.just(exchange.getRequest().getHeaders().getFirst("X-Api-Key"));
    }
}
```



**熔断降级（CircuitBreaker）** ：基于 Resilience4j 实现，支持超时、失败率阈值配置，熔断后转发到降级接口。

**重试（Retry）** ：当服务返回指定状态码或超时时自动重试，需确保接口幂等性。

## 四、多级缓存：Caffeine + Redis

接口元数据（路径、方法、权限配置）变化频率低但读取频繁，适合使用 **Caffeine（本地缓存，L1） + Redis（分布式缓存，L2）** 多级缓存方案。

### 4.1 缓存架构

text

```
请求 → Caffeine (L1) → 命中？返回
                      ↓ 未命中
                  Redis (L2) → 命中？回填 L1，返回
                      ↓ 未命中
                  MySQL → 回填 L2 + L1，返回
```



### 4.2 核心逻辑

Caffeine 采用 **W-TinyLFU 淘汰算法**，在高并发场景下比传统 LRU 算法命中率提升 10%-20%。

**查询流程**：

1. 先查询 Caffeine 本地缓存，命中则直接返回
2. 未命中则查询 Redis 分布式缓存，命中则将数据同步到 Caffeine 后返回
3. 两者均未命中则查询 MySQL，将结果同步到 Redis 和 Caffeine 后返回

## 五、前端：Vue 3 + TypeScript + Ant Design Vue

### 5.1 项目初始化

bash

```bash
npm create vue@latest
# 选择 TypeScript ✓ / Vue Router ✓ / Pinia ✓
```



### 5.2 Axios 拦截器统一处理 JWT

```typescript
// src/api/client.ts
import axios from 'axios'

const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 15000,
})

client.interceptors.request.use((config) => {
  const token = localStorage.getItem('admin_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

client.interceptors.response.use(
  (resp) => resp.data,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('admin_token')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default client
```



### 5.3 页面结构参考

**开发者门户**（对外）：

- `/login`、`/register`：开发者注册登录
- `/dashboard`：控制台概览
- `/apps`：应用列表
- `/apps/:id`：应用详情（密钥查看/重置、接口权限）
- `/tokens`：访问令牌管理
- `/statistics`：调用统计

**运营管理平台**（对内）：

- `/tenants`：租户管理
- `/apis`：API 元数据管理
- `/approvals`：应用/接口审批
- `/statistics`：全局调用统计

## 六、技术栈速查表

| 层级        | 选型                  | 说明                                         |
| :---------- | :-------------------- | :------------------------------------------- |
| 后端框架    | Spring Boot 3.x       |                                              |
| ORM         | MyBatis-Flex          | 必须使用 `mybatis-flex-spring-boot3-starter` |
| 安全        | Spring Security + JWT | 无状态认证，令牌有效期 ≤15 分钟              |
| 网关        | Spring Cloud Gateway  | 路由、限流、熔断、鉴权前置                   |
| 缓存        | Caffeine + Redis      | 本地 L1 + 分布式 L2 多级缓存                 |
| 前端框架    | Vue 3 + TypeScript    | Composition API + `<script setup>`           |
| 构建工具    | Vite                  | 替代 Vue CLI                                 |
| UI 组件库   | Ant Design Vue 4.x    | 企业级后台标准                               |
| 状态管理    | Pinia                 | Vue 官方推荐                                 |
| HTTP 客户端 | Axios                 | 拦截器统一处理 JWT/401                       |

## 七、参考文档

- **MyBatis-Flex 官方文档**：https://mybatis-flex.com/ （APT 配置、多数据源）
- **Spring Cloud Gateway**：https://spring.io/projects/spring-cloud-gateway （路由、过滤器、限流）
- **Spring Security + JWT 实战**：https://github.com/sammoww/spring-jwt-auth （GitHub 参考实现）
- **Vue 3 官方文档**：https://cn.vuejs.org/
- **Ant Design Vue**：https://antdv.com/
- **Caffeine GitHub**：https://github.com/ben-manes/caffeine