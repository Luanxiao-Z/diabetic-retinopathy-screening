# 糖尿病视网膜病变（DR）智能筛查系统 — 项目综述文档

## 1. 项目概述

本项目旨在构建一个面向基层医疗机构、体检中心及社区卫生服务中心的**糖尿病视网膜病变（Diabetic Retinopathy, DR）智能筛查系统**。系统允许用户上传眼底照片，自动调用深度学习模型进行DR严重程度分级（0-4级），并提供相应的转诊建议。系统采用前后端分离架构，后端以Java SpringBoot为核心，前端使用Vue框架，对象存储使用MinIO，整体通过Docker容器化部署，并结合Nginx反向代理。模型层采用轻量化卷积神经网络，可在有限硬件资源（如6GB显存GPU）上训练和推理。

## 2. 目标用户与使用场景

- **主要用户**：基层全科医生、体检中心技师、社区卫生服务中心慢病管理人员。
- **使用场景**：
  - 糖尿病患者定期眼底筛查，支持单张或批量上传眼底照片，快速获得分级结果。
  - 社区义诊等移动筛查场景，即时上传并查看结果，对疑似重度患者建议转诊。
  - 生成筛查统计报表，便于汇总上报。

## 3. 功能需求

### 3.1 用户管理
- 简单登录认证（可选JWT）。
- 用户角色：医生、管理员（管理员可管理所有记录，医生仅管理自己的记录）。
- 注册功能可开放或由管理员创建。

### 3.2 图像上传与筛查
- 支持单张或批量上传眼底照片（常见格式：JPEG、PNG）。
- 上传后系统自动调用模型服务进行DR分级，返回结果。
- 展示每张图像的分级结果、置信度和转诊建议。

### 3.3 结果展示
- 以卡片或列表形式展示筛查结果，包含：
  - 眼底图像缩略图（通过MinIO预签名URL加载）
  - DR分级（0：正常，1：轻度NPDR，2：中度NPDR，3：重度NPDR，4：PDR）
  - 置信度
  - 转诊建议（0-1级定期复查，2级建议眼科就诊，3-4级建议尽快转诊上级医院）
- 可选：点击查看大图及Grad-CAM热力图（可解释性）。

### 3.4 报告生成
- 支持选中一条或多条筛查记录，生成PDF筛查报告（包含图像、分级、建议）。
- 支持导出Excel统计表。

### 3.5 历史记录管理
- 分页查询历史筛查记录，支持按日期、患者ID、分级等条件筛选。
- 删除记录（仅管理员或记录创建者）。

### 3.6 统计汇总
- 提供统计接口，返回各分级数量、筛查时间趋势、转诊率等数据。
- 前端使用图表（如ECharts）展示。

## 4. 技术架构

### 4.1 总体架构
```
[前端 Vue3 + Element Plus] 
        |
        | HTTP/HTTPS
        v
[Nginx] -- 静态资源托管 / 反向代理
        |
        v
[后端 SpringBoot] -- RESTful API
        |
        |---> [MySQL] 业务数据
        |---> [Redis] 缓存/会话
        |---> [MinIO] 眼底图像存储
        |---> [模型服务 (Python FastAPI)] -- 深度学习推理
```

### 4.2 技术栈（版本不限，由实现者选择合适版本）
- **前端**：Vue 3，TypeScript（可选），Vite，Element Plus，Pinia，Axios，ECharts。
- **后端**：Java，Spring Boot，MyBatis-Plus，Spring Data Redis，MinIO Java SDK，JWT（如jjwt），Lombok，Hutool。
- **数据库**：MySQL 8.x。
- **缓存**：Redis（运行在Docker容器内）。
- **对象存储**：MinIO（运行在Docker容器内）。
- **模型服务**：Python 3.12，PyTorch，FastAPI，timm，pytorch-grad-cam（可选）。
- **部署**：Docker，Docker Compose，Nginx。

当前运行在系统Docker容器内的信息如下，后续部署可以采用类似配置：

```
services:
  # ========== MinIO 对象存储服务 ==========
  fjzhmz-minio:
    image: harbor.ffcs.cn/cne/cne-common/minio:RELEASE.2024-03-10T02-53-48Z
    container_name: fjzhmz-minio
    restart: unless-stopped
    environment:
      MINIO_ACCESS_KEY: minio.admin
      MINIO_SECRET_KEY: minio.123!@#
      MINIO_ADDRESS: ":9000"
      MINIO_CONSOLE_ADDRESS: ":9001"
    ports:
      - "9001:9001"
      - "9000:9000"
    volumes:
      - ./docker/data/minio:/minio_data
    command: minio server /minio_data --console-address ":9001"

  # ========== Redis 缓存服务 ==========
  fjzhmz-redis:
    image: harbor.ffcs.cn/cne/cne-common/redis:7.2.4
    container_name: fjzhmz-redis
    restart: unless-stopped
    environment:
      TZ: Asia/Shanghai
    ports:
      - "26379:26379"
    volumes:
      - ./docker/data/redis/db:/data
    command:
      - '--port 26379'
      - '--requirepass cne.123!@#'
      - '--dir /data'
      - '--appendonly yes'

```



### 4.3 模型层轻量化方案
- **骨干网络**：MobileNetV3-Small 或 EfficientNet-B0（参数量约2.5M~5.3M，输入分辨率224×224）。
- **训练数据**：APTOS 2019 Blindness Detection（公开，CC0），共3662张眼底照片，标注0-4级。
- **训练策略**：ImageNet预训练权重初始化，冻结前几层，微调最后几层和分类头，混合精度训练，batch size 32~64，可在6GB显存设备上完成。
- **模型导出**：支持导出ONNX或TorchScript格式，由模型服务加载。
- **模型服务接口**：提供`POST /predict`，接收图像文件（multipart/form-data），返回JSON（分级、置信度、各分类概率）。

## 5. 数据库设计（MySQL）

### 5.1 用户表 `user`
| 字段       | 类型                     | 说明                       |
| ---------- | ------------------------ | -------------------------- |
| id         | BIGINT PK AUTO_INCREMENT | 用户ID                     |
| username   | VARCHAR(50) UNIQUE       | 登录名                     |
| password   | VARCHAR(255)             | 加密密码                   |
| real_name  | VARCHAR(50)              | 真实姓名                   |
| role       | TINYINT                  | 角色（0：医生，1：管理员） |
| created_at | DATETIME                 | 创建时间                   |

### 5.2 筛查记录表 `screening_record`
| 字段         | 类型                     | 说明                      |
| ------------ | ------------------------ | ------------------------- |
| id           | BIGINT PK AUTO_INCREMENT | 记录ID                    |
| patient_id   | VARCHAR(50)              | 患者编号（可自填）        |
| user_id      | BIGINT                   | 操作医生ID                |
| image_url    | VARCHAR(255)             | MinIO对象路径（相对路径） |
| result_level | TINYINT                  | DR分级（0-4）             |
| confidence   | FLOAT                    | 置信度                    |
| suggestion   | VARCHAR(255)             | 转诊建议                  |
| created_at   | DATETIME                 | 筛查时间                  |

可根据需要增加索引（如`user_id`, `created_at`）。

## 6. 后端接口设计（RESTful）

统一前缀：`/api`，返回JSON格式，使用统一响应结构（如`{code, message, data}`）。

### 6.1 认证模块
- `POST /api/auth/login`：用户登录，请求体`{username, password}`，返回JWT token。
- `POST /api/auth/register`：用户注册（可选）。

### 6.2 筛查模块
- `POST /api/screening/upload`：上传一张或多张眼底照片（multipart/form-data，字段名`files`）。后端接收文件，调用模型服务获取分级，上传原始图像到MinIO，写入数据库，返回筛查记录列表。
- `GET /api/screening/{id}`：获取单条筛查记录详情（包含预签名URL）。
- `GET /api/screening/list`：分页查询筛查记录，参数：`page`, `size`, `patientId`(可选), `startDate`, `endDate`, `level`(可选)。
- `DELETE /api/screening/{id}`：删除记录（管理员或创建者）。
- `GET /api/screening/stats`：获取统计汇总数据（各分级数量、按日期趋势等）。
- `GET /api/screening/export`：导出筛查报告（PDF或Excel），参数：记录ID列表。

### 6.3 文件访问
- 不直接暴露MinIO，由后端生成预签名URL返回给前端，前端直接加载图像。

## 7. MinIO集成方案

- 使用MinIO存储眼底图像，桶名建议`fundus-images`，私有权限。
- 后端通过MinIO Java SDK进行上传、删除、生成预签名URL（有效期可配置，如7天）。
- 上传对象命名规则：`{patientId}/{yyyyMMddHHmmss}_{random}.jpg`。
- 桶策略保持私有，避免公开访问泄露患者隐私。

## 8. Redis使用场景

- 缓存登录用户信息或JWT黑名单（可选）。
- 缓存统计数据（如每日筛查数量），减少数据库压力。
- 可用于批量上传时的临时任务状态（可选，若采用异步处理）。

## 9. 前端页面设计

### 9.1 登录页
- 简洁表单，医疗主题背景。

### 9.2 主布局
- 侧边导航栏：上传筛查、历史记录、统计汇总、个人中心。
- 顶部栏：显示当前用户、退出登录。

### 9.3 上传筛查页（核心）
- 拖拽上传区域，支持多文件。
- 上传后自动显示处理进度（可显示“分析中”动画）。
- 结果卡片列表：每张卡片展示缩略图、分级彩色标签、置信度、建议。
- 点击卡片查看大图及热力图（如果模型服务提供）。
- 支持多选记录，一键生成PDF报告。

### 9.4 历史记录页
- 表格展示，支持筛选条件，可查看详情，可删除。

### 9.5 统计汇总页
- 使用ECharts展示柱状图（各分级数量）、趋势图（按日期）、饼图（转诊率）等。

### 9.6 个人中心
- 查看和修改个人资料。

## 10. 部署方案（Docker Compose）

编写`docker-compose.yml`，包含以下服务：
- `mysql`：MySQL 8，挂载数据卷。
- `redis`：Redis 7。
- `minio`：MinIO服务器，暴露9000（API）和9001（控制台）。
- `model-service`：Python FastAPI模型服务，内部端口8000。
- `backend`：SpringBoot应用，构建自Dockerfile，内部端口8080。
- `frontend`：前端构建产物，使用Nginx镜像托管静态文件，并配置反向代理（将`/api`代理到`backend:8080`）。

Nginx配置要点：
- 根路径托管前端静态文件，支持Vue Router的history模式。
- `/api/` 反向代理到后端。
- 可选：代理MinIO控制台（不推荐直接暴露，可配置独立端口访问）。

## 11. 非功能性需求

- **性能**：单张图像推理时间应小于2秒（含网络和模型推理），批量上传时前端应有进度提示。
- **安全**：密码加密存储（BCrypt），JWT令牌有效期控制，MinIO私有桶，接口权限校验。
- **可靠性**：模型服务应具备错误处理，后端需处理模型服务不可用情况，返回友好错误。
- **可扩展性**：模型服务独立，可水平扩展；MinIO可替换为云对象存储。

## 12. 开发步骤建议

1. **模型训练与模型服务**（Python）
   - 下载APTOS数据集，预处理，训练轻量模型，导出模型。
   - 开发FastAPI推理服务，提供`/predict`接口，测试通过。
2. **后端开发**（Java）
   - 搭建SpringBoot项目，整合MyBatis-Plus、Redis、MinIO、JWT。
   - 实现数据库表结构与实体类。
   - 实现认证、上传、查询、统计、删除、导出等接口。
   - 联调模型服务（HTTP调用）。
3. **前端开发**（Vue）
   - 搭建Vue3项目，引入Element Plus、Pinia、Axios。
   - 实现登录、上传、结果展示、历史记录、统计页面。
   - 对接后端API。
4. **Docker化部署**
   - 编写各服务Dockerfile和docker-compose.yml。
   - 本地测试一键启动。
5. **文档与发布**
   - 编写README（架构图、运行说明、演示截图）。
   - 添加MIT LICENSE，上传GitHub。

## 13. 其他说明

- **开源协议**：项目代码使用MIT License；数据集APTOS 2019为CC0；预训练模型遵守PyTorch BSD协议；MinIO使用AGPL v3（内部使用无碍）。
- **可扩展方向**：可引入LLM生成个性化建议，联邦学习模拟，多模态融合等，作为后续创新点。

---

**本文档为项目综述，旨在指导智能体开展后续开发工作。智能体可根据实际需要进一步设计。**