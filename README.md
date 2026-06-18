# 包装机固定卡扣配件库存台账系统

面向包装车间的小件物料管理系统，管控设备卡扣、固定支架配件的入库、领用、盘点、报废全流程。

## 技术栈

- **前端**：Vue 3 + Vite + TypeScript + Tailwind CSS + Pinia + Axios
- **后端**：Spring Boot 3.3 + JDK 17 + MyBatis-Plus 3.5 + Spring Data Redis
- **数据库**：MySQL 8.0（关闭二进制日志）
- **缓存**：Redis 7（LIST 结构缓存配件清单，30 分钟过期）
- **部署**：Docker Compose 全容器部署

## 快速启动

### 一键启动

```bash
./start.sh
```

### 手动启动

```bash
docker compose up -d --build
```

启动成功后访问：**http://localhost:3119**

## 端口配置

所有端口均在 `.env` 文件中统一管理，默认分配：

| 服务 | 宿主机端口 | 容器内端口 | 说明 |
|------|-----------|-----------|------|
| 前端 | 3119 | 80 | Nginx 静态资源 |
| 后端 | 8119 | 8103 | Spring Boot API |
| MySQL | 3369 | 3306 | 业务数据库 |
| Redis | 6319 | 6379 | 缓存中间件 |

所有服务均绑定 `127.0.0.1`，仅本机可访问。

## 环境变量

编辑 `.env` 文件可修改配置：

```env
DOCKER_REGISTRY=docker.io        # 镜像仓库地址
PROJECT_NAME=qd-119             # 项目名称（容器名前缀）

FRONTEND_PORT=3119              # 前端端口
BACKEND_PORT=8119               # 后端端口
MYSQL_PORT=3369                 # MySQL 端口
REDIS_PORT=6319                 # Redis 端口

MYSQL_ROOT_PASSWORD=root123     # MySQL root 密码
MYSQL_DATABASE=buckle_inventory # 数据库名
```

## 功能模块

### 1. 配件入库建档
- 支持新建配件入库（录入名称、型号、数量、货架位置）
- 支持选择已有配件入库
- 自动增加库存总量和当前库存

### 2. 车间领用出库登记
- 选择配件和领用产线
- 库存校验，自动扣减当前库存
- 产线徽章标识（产线A/B/C/D）

### 3. 季度实物盘点
- 自动加载所有配件账面库存
- 可编辑实际盘点数量
- 实时计算账面与实物差额
- 盘点历史记录可展开查看明细

### 4. 变形配件报废登记
- 多选报废原因（变形、锈蚀、断裂、其他）
- 库存校验，自动扣减库存
- 报废记录支持备注信息

### 5. 配件清单管理
- 分页展示所有配件
- 支持按名称、型号、货架位置搜索
- 低库存闪烁告警
- 批量勾选操作
- 模态框编辑配件信息

### 6. 首页仪表盘
- 配件总数、库存总量统计
- 月度入库、出库统计
- 近期动态时间轴

## 项目结构

```
qd-119/
├── .env                      # 全局环境变量配置
├── .gitignore
├── docker-compose.yml        # Docker Compose 编排
├── start.sh                  # 一键启动脚本
├── README.md                 # 项目说明文档
├── init-db/
│   └── init.sql              # 数据库初始化 DDL
├── backend/                  # 后端项目
│   ├── pom.xml
│   ├── Dockerfile
│   ├── settings.xml          # Maven 镜像配置
│   ├── .dockerignore
│   └── src/main/java/com/buckle/inventory/
│       ├── BuckleInventoryApplication.java
│       ├── config/           # 配置类（MyBatis-Plus、Redis、CORS）
│       ├── controller/       # 6 个 Controller
│       ├── service/          # 7 个 Service 接口
│       ├── service/impl/     # 7 个 Service 实现
│       ├── mapper/           # 6 个 Mapper
│       ├── entity/           # 6 个实体类
│       ├── dto/              # 10 个 DTO
│       └── resources/
│           └── application.yml
└── frontend/                 # 前端项目
    ├── package.json
    ├── package-lock.json
    ├── Dockerfile
    ├── .dockerignore
    ├── .npmrc                # npm 镜像配置
    ├── nginx.conf            # Nginx 配置
    ├── vite.config.ts        # Vite 配置
    ├── tailwind.config.js
    ├── index.html
    └── src/
        ├── main.ts
        ├── App.vue
        ├── router/
        ├── stores/
        ├── api/index.ts      # API 封装
        ├── pages/            # 6 个页面
        └── components/       # 公共组件
```

## Docker 构建优化

### 分层缓存机制

- **前端**：先复制 `package*.json` 执行 `npm ci`，再复制源码执行 `npm run build`
- **后端**：先复制 `settings.xml` 和 `pom.xml` 执行 `mvn dependency:go-offline`，再复制源码执行 `mvn package`
- 依赖文件不变时，后续构建复用缓存，仅编译业务代码

### .dockerignore

排除 node_modules、dist、target、日志、临时文件、IDE 配置等无关文件，减小构建上下文。

### 国内镜像源

- **npm**：华为云镜像（https://repo.huaweicloud.com/repository/npm/）
- **Maven**：阿里云镜像（https://maven.aliyun.com/repository/public）
- **基础镜像**：通过 `DOCKER_REGISTRY` 环境变量统一配置

## API 接口

| 模块 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 仪表盘 | GET | `/api/dashboard/overview` | 概览统计 |
| 仪表盘 | GET | `/api/dashboard/recent` | 近期动态 |
| 配件 | GET | `/api/parts` | 配件列表（分页+搜索） |
| 配件 | POST | `/api/parts` | 新增配件 |
| 配件 | PUT | `/api/parts/{id}` | 更新配件 |
| 配件 | DELETE | `/api/parts/{id}` | 删除配件 |
| 配件 | POST | `/api/parts/batch` | 批量新增配件 |
| 入库 | GET | `/api/inbound` | 入库记录列表 |
| 入库 | POST | `/api/inbound` | 新增入库 |
| 出库 | GET | `/api/outbound` | 出库记录列表 |
| 出库 | POST | `/api/outbound` | 新增出库 |
| 报废 | GET | `/api/scrap` | 报废记录列表 |
| 报废 | POST | `/api/scrap` | 新增报废 |
| 盘点 | GET | `/api/inventory` | 盘点记录列表 |
| 盘点 | POST | `/api/inventory` | 新增盘点 |
| 盘点 | GET | `/api/inventory/{id}` | 盘点详情 |

## 数据库设计

共 6 张业务表：

1. `part` - 配件信息表
2. `inbound_record` - 入库流水表
3. `outbound_record` - 出库流水表
4. `scrap_record` - 报废记录表
5. `inventory_check` - 盘点主表
6. `inventory_check_item` - 盘点明细表

## 开发模式

### 前端开发模式

```bash
cd frontend
npm install
npm run dev
```

开发服务器监听 `127.0.0.1:3119`，API 请求代理到后端。

### 后端开发模式

```bash
cd backend
mvn spring-boot:run
```

后端服务监听 `127.0.0.1:8119`。

## 常用命令

```bash
# 启动所有服务
docker compose up -d

# 停止所有服务
docker compose down

# 查看日志
docker compose logs -f

# 查看特定服务日志
docker compose logs -f backend

# 重新构建并启动
docker compose up -d --build

# 重启某个服务
docker compose restart frontend
```
