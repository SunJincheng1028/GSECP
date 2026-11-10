# 生成式软件工程课程设计平台

迭代 1（MVP）已完成：邀请码进班、步骤与交付项配置、按类型动态提交、批改评分与总评汇总；迭代 0 提供认证、权限、统一响应、文件存储等工程基座。需求见 `docs/需求分析列表.md`，迭代计划见 `docs/迭代计划.md`，ER 设计见 `backend/src/main/resources/db/migration/V1__init.sql`。

## 目录结构

```
GSECP/
├── docs/                    需求列表、迭代计划
├── backend/                 Spring Boot 4.1.2 (JDK 21, Maven)
│   └── src/main/resources/
│       ├── application.yml
│       ├── application-dev.yml
│       └── db/migration/V1__init.sql   Flyway 初始化脚本（30 张表）
└── frontend/                Vue 3 + Vite + TypeScript + Pinia + Element Plus
```

## 环境要求

| 组件 | 版本 |
|------|------|
| JDK | 21+ |
| Maven | 3.9+ |
| Node.js | 20+（本项目验证于 Node 24） |
| MySQL | 8.0.16+ |
| Spring Boot | 4.1.2 |

## 启动步骤

### 1. 创建数据库

```sql
CREATE DATABASE IF NOT EXISTS gsecp
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
```

建表由 Flyway 在后端启动时自动执行，无需手工导入 SQL。

### 2. 配置数据库连接

仓库不内置数据库密码，启动后端前必须设置环境变量（PowerShell 示例）：

```powershell
$env:GSECP_DB_PASSWORD = '你的 MySQL 密码'
```

| 变量 | 是否必填 | 说明 |
|------|------|------|
| `GSECP_DB_PASSWORD` | **必填** | 数据库密码，未设置时使用占位值 `changeme` 将无法连接 |
| `GSECP_DB_USERNAME` | 否，默认 `root` | 数据库账号 |
| `GSECP_JWT_SECRET` | 否，有开发默认值 | JWT 密钥，长度 ≥ 32 字符，**生产必须覆盖** |
| `GSECP_ADMIN_PASSWORD` | 否，默认 `Admin@123456` | 首次建库时的初始管理员密码，**生产必须覆盖** |

### 3. 启动后端

```bash
cd backend
mvn spring-boot:run
```

- 地址：`http://localhost:8080`
- 健康检查：`GET http://localhost:8080/api/health`
- 接口文档：`http://localhost:8080/swagger-ui.html`
- 首次启动自动创建管理员：`admin` / 配置中的 `GSECP_ADMIN_PASSWORD`

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

- 地址：`http://localhost:5173`
- Vite 已把 `/api` 代理到 `http://localhost:8080`

## 验收清单

### 迭代 0 工程基座

- [x] `mvn clean package` 构建与单元测试通过、`npm run build` 构建通过
- [x] 后端启动后 Flyway 成功创建 30 张业务表与字典种子数据
- [x] `GET /api/health` 返回 `{"code":200,...}`，版本 `v0.2` / 迭代 `1`
- [x] `POST /api/auth/login`（`admin` + 初始密码）返回 JWT 与角色列表
- [x] 无 Token 访问 `/api/admin/users` 返回 401，带管理员 Token 返回 200
- [x] 前端登录后可进入工作台，菜单按角色显示，按钮级 `v-permission` 生效

### 迭代 1 MVP（已在真实 MySQL 8.4 上端到端验证）

- [x] 管理员创建教师账号；教师创建班级并生成 8 位邀请码，可启停
- [x] 学生凭邀请码注册后自动登录并成为班级成员；停用/过期/超限邀请码被拒绝
- [x] 教师创建课程、发布课程、配置步骤权重与 4 类交付项（文档 / 代码包 / 仓库地址 / 文本）
- [x] `constraint_json` 约束生效：格式、大小、字数、URL 域名白名单，缺必填项被拒绝
- [x] 学生按类型动态提交，保留版本历史，教师进度列表只展示最新版本
- [x] 只能批改最新提交；分数按步骤权重折算，`90 × 60% = 54` 计入总评，已批改权重 60%
- [x] 教师查看总评报表、学生查看个人成绩，结果一致；学生访问教师进度接口返回 403

## 迭代 1 演示数据（本机 MySQL）

| 角色 | 账号 | 密码 |
|------|------|------|
| 管理员 | `admin` | `Admin@123456` |
| 教师 | `mvp_teacher_233714` | `Teacher@123456` |
| 学生 | `mvp_student_233714` | `Student@123456` |

演示数据包含一个班级、一门已发布课程、两个步骤（权重 60% / 40%）、一次带 4 类交付项的提交（v2 已批改 90 分）与对应总评 54 分。

## 关键接口

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/health` | 健康检查 | 公开 |
| POST | `/api/auth/login` | 登录 | 公开 |
| POST | `/api/auth/logout` | 登出（前端丢弃 Token） | 登录 |
| GET | `/api/auth/me` | 当前用户 | 登录 |
| GET | `/api/admin/ping` | 管理员权限示例 | ADMIN |
| GET | `/api/teacher/ping` | 教师权限示例 | ADMIN / TEACHER |
| GET | `/api/student/ping` | 学生权限示例 | 全部角色 |
| POST | `/api/files/upload` | 上传文件 | 登录 |
| GET | `/api/files/{id}/download` | 下载文件 | 登录 |
| POST | `/api/auth/register` | 学生凭邀请码注册并自动登录 | 公开 |
| GET/POST | `/api/admin/users` | 用户列表 / 创建教师或管理员 | ADMIN |
| GET/PUT | `/api/admin/config` | 系统参数与上传上限 | ADMIN |
| GET/PATCH | `/api/dict/deliverable-types` | 交付物类型字典与启停 | 查询登录 / 修改 ADMIN |
| GET/POST | `/api/classes` | 班级列表 / 创建班级 | ADMIN / TEACHER |
| GET/POST | `/api/classes/{id}/invite-codes` | 邀请码列表 / 生成 | 班级管理者 |
| GET/POST | `/api/classes/{classId}/courses` | 课程列表 / 创建 | 班级成员 |
| GET/POST | `/api/courses/{courseId}/steps` | 步骤与交付项列表 / 创建 | 课程可见者 / 管理者 |
| POST | `/api/steps/{id}/submissions` | 按类型动态提交成果 | STUDENT |
| GET | `/api/steps/{id}/my-submission` | 我的最新提交与版本历史 | STUDENT |
| GET | `/api/steps/{id}/submissions` | 班级提交进度 | ADMIN / TEACHER |
| POST | `/api/submissions/{id}/score` | 批改最新提交 | ADMIN / TEACHER |
| GET | `/api/courses/{id}/grades` | 教师总评报表 | ADMIN / TEACHER |
| GET | `/api/courses/{id}/my-grades` | 学生个人总评 | STUDENT |

## 设计约定

- 统一响应体 `{ code, message, data, timestamp }`，`code` 与 HTTP 状态一致
- 枚举使用 `VARCHAR` + 应用层校验；`constraint_json` / `params_json` 使用 MySQL 8 `JSON`
- `dict_deliverable_type.code` 被多表外键引用，**只允许新增或停用，禁止修改**
- 逻辑删除字段 `is_deleted`；唯一索引不区分删除状态，账号名与邀请码不可复用
- 大模型服务抽象为 `AiClient`，迭代 0 为 `NoopAiClient`；AI 功能在迭代 3 接入 DeepSeek
- 文件存储抽象为 `FileStorage`，迭代 0 为本地磁盘实现，可替换为 MinIO / OSS
- 迭代 1 业务模块采用 `JdbcTemplate` + `record` DTO，数据级权限由 `Authz` 统一校验（教师只能管理自己的班级）
- 交付物类型由字典 `frontend_component` / `backend_handler` 驱动：扩展新类型只需增加字典行、注册前端组件与后端校验器，不改提交主流程
- 提交采用版本快照（`version` + `is_latest`）；总评按 `score × weight / 100` 累加，未批改步骤不计入已批改权重

## 安全提示

当前配置面向本地开发：默认 JWT 密钥与管理员密码写在配置文件中，仅用于开发。上线前必须通过环境变量覆盖，并关闭 `gsecp.bootstrap.enabled`。
