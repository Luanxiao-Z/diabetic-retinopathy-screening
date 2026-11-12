# 贡献指南

感谢关注本项目。这是一个**教学与科研用途**的糖尿病视网膜病变（DR）智能筛查演示系统，
**不是医疗器械，未经过临床验证**，任何改动都不得用于真实诊断场景。

## 开始之前

- 完整的功能说明、技术栈与启动步骤见 [README](README.md)。
- 开发规范统一维护在 [`docs/`](docs/) 下（`backend/`、`design/`、`frontend/`、`project/`）。
  **规范与代码冲突时以代码实现为准**，并请在 PR 中同步修订文档。
- 建议先开 Issue 描述问题或想法，确认方向后再动手，避免重复劳动。

## 环境要求

| 组件 | 版本 |
| --- | --- |
| JDK | 21 |
| Maven | 3.9.x |
| Node.js | 18+（推荐 22） |
| Yarn | 1.22 |
| Python | 3.13（与 torch cu130 轮子匹配） |
| MySQL / Redis / Silo | 8.0 / 7 / 最新（见 [deploy/README.md](deploy/README.md)） |

数据库与对象存储需自行准备；连接信息一律通过环境变量注入（`DB_PASSWORD`、`REDIS_PASSWORD`、
`MINIO_ACCESS_KEY`、`MINIO_SECRET_KEY`），**请勿将明文密码写入版本库**。

## 本地开发

```bash
# 后端（在 backend/ 目录内执行，Maven 包装脚本要求）
bash tools/mvn.sh clean install -DskipTests
java -jar backend/drs-app/drs-app-server/target/drs-app-server-1.0.0.jar --server.port=8080

# 模型服务
cd model-service && python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --host 127.0.0.1 --port 8000

# 前端
cd frontend/web && yarn install && yarn dev
```

> 修改后端代码前请先停止 8080 端口，否则 `mvn clean` 无法删除被占用的 `target` 目录。

## 提交前自检

只运行与改动相关的最小集合即可，但**必须全部通过**：

```bash
# 前端（frontend/web）
yarn lint          # ESLint
yarn stylelint     # Stylelint（.vue 内样式同样检查）
npx vue-tsc --noEmit
yarn build

# 后端（backend）
bash tools/mvn.sh compile -DskipTests

# 模型服务（model-service）
ruff check app/ training/
```

CI 会对上述检查做同样的校验（见 [.github/workflows/ci.yml](.github/workflows/ci.yml)），
PR 中 CI 未通过不会被合并。

## 代码约定

- **提交信息**：遵循 [Conventional Commits](https://www.conventionalcommits.org/zh-hans/)，
  格式 `<type>(<scope>): <subject>`，`subject` 用小写祈使句、不超过 50 字符；
  一个提交只做一件事，跨前后端的改动请拆开提交。
- **后端**：统一响应体 `{code,msg,data}`、`@RequirePermission` 鉴权、实体与 DTO 使用 Lombok、
  数据权限在 Service 层拼接过滤条件。
- **前端**：TypeScript 全量、设计令牌集中在 `src/styles/theme.css`（禁止写死色值）、
  图标统一用 `AppIcon`、页面骨架为 `PageHeader` + `drs-card`。
- **样式类名**：kebab-case，BEM 的 `__` / `--` 分隔符允许使用。
- **Python**：`ruff check` 通过；规则集固定于 `model-service/ruff.toml`。

## 分支与 PR

1. 从 `main` 拉出功能分支：`feat/xxx`、`fix/xxx`、`docs/xxx`。
2. 提交前跑完上面的自检项，更新受影响的文档与截图。
3. PR 描述中说明：**动机**（为什么改）、**影响面**（哪些页面/接口/表）、**验证方式**（跑了什么、结果如何）。
4. 涉及权限、数据权限、导出或报告版式的改动，请在描述中附截图或接口返回示例。

## 报告问题

Issue 中请尽量包含：

- 环境（操作系统、Java/Node/Python 版本、是否使用 GPU）
- 复现步骤（越具体越好）
- 期望结果与实际结果
- 相关日志片段（**请先脱敏**：不要粘贴数据库密码、对象存储密钥、患者真实信息）

## 数据与隐私

- 仓库内不含真实患者影像；`datasets/` 已被 `.gitignore` 排除。
- 提交截图或日志前，请确认不含真实患者姓名、影像与凭据。
- APTOS 2019 数据集版权归原始提供方所有，仅限科研与教学用途。

## 许可证

贡献代码即表示同意以项目的 [MIT License](LICENSE) 授权。
