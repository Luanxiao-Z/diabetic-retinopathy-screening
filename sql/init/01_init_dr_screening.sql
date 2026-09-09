-- ============================================================
-- DR 智能筛查系统 —— 数据库初始化
-- MySQL 8.0；主键 CHAR(32) UUID；编码类字段使用 *_as_cs 排序规则；
-- 不建物理外键（按数据库设计规范）；逻辑删除标记 delete_flag = N/Y。
-- 容器首次启动时由 /docker-entrypoint-initdb.d 自动执行。
-- ============================================================

CREATE DATABASE IF NOT EXISTS dr_screening
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE dr_screening;

-- ---------------- 字典域 ----------------
CREATE TABLE IF NOT EXISTS sys_dict_domain (
  id          CHAR(32)     NOT NULL,
  domain_code VARCHAR(64)  NOT NULL COLLATE utf8mb4_0900_as_cs,
  domain_name VARCHAR(128) NOT NULL,
  remark      VARCHAR(255) DEFAULT NULL,
  create_by   VARCHAR(64)  DEFAULT NULL,
  create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
  update_by   VARCHAR(64)  DEFAULT NULL,
  update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  delete_flag VARCHAR(1)   NOT NULL DEFAULT 'N' COLLATE utf8mb4_0900_as_cs,
  PRIMARY KEY (id),
  UNIQUE KEY uk_domain_code (domain_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------------- 字典项 ----------------
CREATE TABLE IF NOT EXISTS sys_dict_item (
  id          CHAR(32)     NOT NULL,
  domain_code VARCHAR(64)  NOT NULL COLLATE utf8mb4_0900_as_cs,
  item_code   VARCHAR(64)  NOT NULL COLLATE utf8mb4_0900_as_cs,
  item_name   VARCHAR(128) NOT NULL,
  item_value  VARCHAR(255) DEFAULT NULL,
  sort        INT          NOT NULL DEFAULT 0,
  remark      VARCHAR(255) DEFAULT NULL,
  create_by   VARCHAR(64)  DEFAULT NULL,
  create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
  update_by   VARCHAR(64)  DEFAULT NULL,
  update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  delete_flag VARCHAR(1)   NOT NULL DEFAULT 'N' COLLATE utf8mb4_0900_as_cs,
  PRIMARY KEY (id),
  UNIQUE KEY uk_domain_item (domain_code, item_code),
  KEY idx_domain_code (domain_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------------- 系统用户 ----------------
CREATE TABLE IF NOT EXISTS sys_user (
  id          CHAR(32)     NOT NULL,
  username    VARCHAR(64)  NOT NULL COLLATE utf8mb4_0900_as_cs,
  password    VARCHAR(128) NOT NULL,
  real_name   VARCHAR(64)  DEFAULT NULL,
  role        VARCHAR(20)  NOT NULL COLLATE utf8mb4_0900_as_cs COMMENT '用户角色(字典:B_USER_ROLE) DOCTOR/ADMIN',
  phone       VARCHAR(32)  DEFAULT NULL,
  status      VARCHAR(20)  NOT NULL DEFAULT 'ENABLED' COLLATE utf8mb4_0900_as_cs COMMENT '状态(字典:C_STATUS)',
  create_by   VARCHAR(64)  DEFAULT NULL,
  create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
  update_by   VARCHAR(64)  DEFAULT NULL,
  update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  delete_flag VARCHAR(1)   NOT NULL DEFAULT 'N' COLLATE utf8mb4_0900_as_cs,
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------------- 筛查记录 ----------------
CREATE TABLE IF NOT EXISTS biz_screening_record (
  id            CHAR(32)     NOT NULL,
  user_id       VARCHAR(64)  NOT NULL COLLATE utf8mb4_0900_as_cs COMMENT '操作医生 username',
  patient_name  VARCHAR(64)  DEFAULT NULL,
  patient_age   INT          DEFAULT NULL,
  patient_gender VARCHAR(20) DEFAULT NULL COLLATE utf8mb4_0900_as_cs COMMENT '性别(字典:C_GENDER)',
  image_key     VARCHAR(255) NOT NULL COMMENT '原始图片 MinIO object key',
  image_url     VARCHAR(512) DEFAULT NULL,
  result_level  VARCHAR(20)  NOT NULL COLLATE utf8mb4_0900_as_cs COMMENT 'DR分级(字典:B_DR_LEVEL) LEVEL_0..LEVEL_4',
  confidence    DECIMAL(6,4) DEFAULT NULL COMMENT '最高概率置信度',
  probabilities TEXT         DEFAULT NULL COMMENT '各类别概率 JSON',
  suggestion    VARCHAR(20)  DEFAULT NULL COLLATE utf8mb4_0900_as_cs COMMENT '转诊建议(字典:B_DR_SUGGESTION)',
  grad_cam_key  VARCHAR(255) DEFAULT NULL COMMENT '热力图 MinIO object key',
  model_version VARCHAR(64)  DEFAULT NULL,
  remark        VARCHAR(512) DEFAULT NULL,
  create_by     VARCHAR(64)  DEFAULT NULL,
  create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP,
  update_by     VARCHAR(64)  DEFAULT NULL,
  update_time   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  delete_flag   VARCHAR(1)   NOT NULL DEFAULT 'N' COLLATE utf8mb4_0900_as_cs,
  PRIMARY KEY (id),
  KEY idx_user_id (user_id),
  KEY idx_result_level (result_level),
  KEY idx_create_time (create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------------- 字典初始化数据 ----------------
INSERT INTO sys_dict_domain (id, domain_code, domain_name, remark)
VALUES
  ('d0c00000000000000000000000000001', 'B_USER_ROLE',     '用户角色',   'DOCTOR/ADMIN'),
  ('d0c00000000000000000000000000002', 'B_DR_LEVEL',      'DR 分级',   'LEVEL_0..LEVEL_4'),
  ('d0c00000000000000000000000000003', 'B_DR_SUGGESTION', '转诊建议',   'REVIEW/CLINIC/REFERRAL'),
  ('d0c00000000000000000000000000004', 'C_STATUS',        '通用状态',   'ENABLED/DISABLED'),
  ('d0c00000000000000000000000000005', 'C_YES_NO',        '是否',       'Y/N'),
  ('d0c00000000000000000000000000006', 'C_DELETE_FLAG',   '删除标记',   'N/Y'),
  ('d0c00000000000000000000000000007', 'C_GENDER',        '性别',       'MALE/FEMALE')
ON DUPLICATE KEY UPDATE domain_name = VALUES(domain_name);

INSERT INTO sys_dict_item (id, domain_code, item_code, item_name, item_value, sort)
VALUES
  ('d0i00000000000000000000000000011', 'B_USER_ROLE',     'DOCTOR', '医生',       NULL, 1),
  ('d0i00000000000000000000000000012', 'B_USER_ROLE',     'ADMIN',  '管理员',     NULL, 2),
  ('d0i00000000000000000000000000021', 'B_DR_LEVEL',      'LEVEL_0','正常(Normal)','0', 1),
  ('d0i00000000000000000000000000022', 'B_DR_LEVEL',      'LEVEL_1','轻度 NPDR',   '1', 2),
  ('d0i00000000000000000000000000023', 'B_DR_LEVEL',      'LEVEL_2','中度 NPDR',   '2', 3),
  ('d0i00000000000000000000000000024', 'B_DR_LEVEL',      'LEVEL_3','重度 NPDR',   '3', 4),
  ('d0i00000000000000000000000000025', 'B_DR_LEVEL',      'LEVEL_4','PDR',         '4', 5),
  ('d0i00000000000000000000000000031', 'B_DR_SUGGESTION', 'REVIEW',  '定期复查',           NULL, 1),
  ('d0i00000000000000000000000000032', 'B_DR_SUGGESTION', 'CLINIC',  '建议眼科就诊',       NULL, 2),
  ('d0i00000000000000000000000000033', 'B_DR_SUGGESTION', 'REFERRAL','建议尽快转诊上级医院', NULL, 3),
  ('d0i00000000000000000000000000041', 'C_STATUS',        'ENABLED','启用', NULL, 1),
  ('d0i00000000000000000000000000042', 'C_STATUS',        'DISABLED','停用', NULL, 2),
  ('d0i00000000000000000000000000051', 'C_YES_NO',        'Y', '是', NULL, 1),
  ('d0i00000000000000000000000000052', 'C_YES_NO',        'N', '否', NULL, 2),
  ('d0i00000000000000000000000000061', 'C_DELETE_FLAG',   'N', '未删除', NULL, 1),
  ('d0i00000000000000000000000000062', 'C_DELETE_FLAG',   'Y', '已删除', NULL, 2),
  ('d0i00000000000000000000000000071', 'C_GENDER',        'MALE',   '男', NULL, 1),
  ('d0i00000000000000000000000000072', 'C_GENDER',        'FEMALE', '女', NULL, 2)
ON DUPLICATE KEY UPDATE item_name = VALUES(item_name);
