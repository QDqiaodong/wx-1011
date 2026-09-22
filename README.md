# wx-1011 皮划艇训练基地里程统计系统

## 项目简介

皮划艇训练基地里程统计系统，包含 Spring Boot 后端、Vue/Vite 前端、MySQL 和 Redis。项目已统一为 UTF-8 编码，并通过 Docker Compose 固定端口交付。

## 功能模块

- 支架 / 队伍 / 绑定管理、里程区间实时统计看板
- **月度里程结算（按月封账）**：每月底为队伍生成当月结算单，逐段绑定按「覆盖天数 × 支架日里程配额」累计并判定档位达标；
  单据头与逐段明细整段快照冻结，`(team_id, period_month)` 唯一索引防并发重复封账（重复请求返回 409 及失败原因），
  封账后的换绑/解绑/配额调整不影响历史单据，只在后续月份按新数据计算。

## 端口

- 前端: http://localhost:3211 / http://127.0.0.1:3211
- 后端 API: http://localhost:3311/api
- MySQL: 127.0.0.1:3411
- Redis: 127.0.0.1:6511

## 构建与启动

```bash
cd /Users/Admin/Desktop/solo-0601/wx-0701/wx-组1/wx-1011
cd backend && mvn compile -q
cd ../frontend && npm ci && npm run build
cd .. && docker compose up -d --build
```

也可以执行：

```bash
./start.sh
```

## Docker 构建缓存

- 后端 Dockerfile 先复制 `pom.xml` 和 `settings.xml` 并下载 Maven 依赖，再复制 `src` 编译。
- 前端 Dockerfile 先复制 `package.json` 和 `package-lock.json` 并安装 npm 依赖，再复制源码执行构建。
- `.dockerignore` 排除了 `node_modules`、`dist`、`target`、日志、临时文件、截图和 IDE 配置。
