# MsgCenter 消息中心

## 项目结构

```
sorryqin/
├── common/              # 公共模块（BaseModel、Redis、ResponseEntity）
├── msgcenter/          # 主模块
│   ├── controller/      # HTTP 接口层（MsgCenterController, ConsoleController）
│   ├── service/         # 业务逻辑层
│   ├── manager/         # 消息发送管理器（Kafka/MySQL 消息处理）
│   ├── msgpush/         # 渠道推送（Email、SMS、Lark）
│   ├── consumer/        # 消息消费者（Kafka 消费、定时任务）
│   ├── mapper/          # MyBatis Mapper 接口
│   ├── model/           # 数据模型
│   ├── tools/           # 工具服务（熔断、限流、记录查询）
│   └── resources/
│       ├── mapper/*.xml # MyBatis SQL 映射
│       └── application.yml  # 应用配置（不上传）
└── msgcenter-web/       # 前端控制台（独立前端项目）
    ├── index.html
    ├── css/styles.css
    └── js/
        ├── api.js      # API 调用层
        └── app.js      # 业务逻辑 + UI 交互
```

## 技术栈

- **后端**：Spring Boot + MyBatis + MySQL + Redis + Kafka
- **前端**：原生 HTML/CSS/JS（深色科技风格）
- **端口**：8082

## 常用命令

```bash
# 启动服务（从 msgcenter 目录）
cd msgcenter && mvn package -DskipTests && java -jar target/msgcenter.jar

# 重启服务
lsof -ti:8082 | xargs kill -9 && mvn package -DskipTests && java -jar target/msgcenter.jar

# 查看端口占用
lsof -i:8082
```

## 关键接口

| 接口 | 说明 |
|------|------|
| GET /find_all_template | 获取所有模板 |
| POST /create_template | 创建模板 |
| POST /update_template | 更新模板 |
| POST /del_template | 删除模板 |
| POST /send_msg | 发送消息 |
| GET /get_msg_record | 查询发送记录 |
| GET /console/ | 前端控制台页面 |

## 常见问题

### 1. 前端页面打不开（500 / 重定向循环）
- 检查 ConsoleController 是否存在且路径 `/console/` 映射正确
- 检查 `src/main/resources/console/` 下静态文件是否存在

### 2. 模板列表为空
- 确认数据库 `t_msg_template` 表有数据
- 确认 Mapper `findAll()` 方法存在且 SQL 正确

### 3. 发送消息失败（code 1）
- 检查前端 `api.js` 中 `code !== 0` 判断是否正确（后端返回 int，不是字符串）
- 检查 `SendMsgReq` 是否有 `sourceId`、`channel` 字段
- 检查 Redis 是否可用（`stop-writes-on-bgsave-error no`）

### 4. 字段对不上
- MySQL 字段：`template_id`, `template_name`, `template_content`
- Java 字段：`templateId`, `name`, `content`
- Mapper XML 中用 `<resultMap>` 做映射，不要依赖驼峰自动映射

### 5. 端口被占用
```bash
lsof -ti:8082 | xargs kill -9
```

## 数据库表

- `t_msg_template` — 消息模板
- `t_msg_queue` — 消息队列（待发送）
- `t_msg_queue_timer` — 定时消息队列
- `t_msg_record` — 发送记录
- `t_source_quota` — 来源配额
- `t_global_quota` — 全局限流

## 敏感信息

`application.yml` 包含真实数据库密码、Redis 密码、邮箱 SMTP 凭据，已通过 `.gitignore` 排除。部署时需要配置 `application.yml.example` 为真实值。
