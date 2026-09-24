#!/usr/bin/env bash
# ============================================================================
# 演示数据生成脚本（DR 智能筛查系统）
# ----------------------------------------------------------------------------
# 用途：向业务库批量插入演示用账号与筛查记录，便于演示、验收与截图。
#
# 特点：
#   - **幂等**：先按标记清理上一次由本脚本写入的数据，再重新插入；
#   - **只动自己的数据**：账号以 `demo_` 前缀、记录以固定 remark 标记，
#     不会影响真实/手工数据；
#   - 覆盖多种分级、置信度与时间分布，可同时演示「待复核」「需转诊」
#     「逾期未复诊」与「统计环比」等场景。
#
# 用法（在仓库根目录执行）：
#   bash tools/seed-demo-data.sh
#   bash tools/seed-demo-data.sh --clean      # 仅清理，不插入
#
# 连接信息从环境变量读取（与后端一致，切勿写死明文）：
#   DB_HOST(默认 127.0.0.1) DB_PORT(默认 3306) DB_NAME(默认 dr_screening)
#   DB_USER(默认 root) DB_PASSWORD(必填)
#
# 说明：为不依赖对象存储，演示记录的影像字段留空，页面会优雅显示「无影像」。
#       若需要带影像的完整演示，请在「筛查上传」页点击「演示样例」按钮。
# ============================================================================
set -euo pipefail

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-dr_screening}"
DB_USER="${DB_USER:-root}"
MYSQL_BIN="${MYSQL_BIN:-mysql}"
DEMO_REMARK="演示数据（脚本生成）"

if [[ -z "${DB_PASSWORD:-}" ]]; then
  echo "[错误] 未设置 DB_PASSWORD 环境变量。" >&2
  echo "       请先注入数据库密码，例如：export DB_PASSWORD=******" >&2
  exit 1
fi

CLEAN_ONLY=0
[[ "${1:-}" == "--clean" ]] && CLEAN_ONLY=1

# 通过 MYSQL_PWD 传参，避免密码出现在命令行与进程列表中
# 注意：不吞掉 mysql 的 stderr，否则 SQL 出错时难以定位
run_sql() {
  MYSQL_PWD="$DB_PASSWORD" "$MYSQL_BIN" \
    --default-character-set=utf8mb4 \
    -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" "$DB_NAME"
}

echo "==> 目标库：$DB_USER@$DB_HOST:$DB_PORT/$DB_NAME"

# ---------------------------------------------------------------------------
# 1) 清理上一次生成的演示数据（幂等基础）
# ---------------------------------------------------------------------------
echo "==> 清理既有演示数据…"
run_sql <<SQL
DELETE FROM biz_screening_record WHERE remark = '$DEMO_REMARK';
DELETE FROM sys_user WHERE username LIKE 'demo\_%';
SQL

if [[ "$CLEAN_ONLY" == "1" ]]; then
  echo "==> 仅清理模式，完成。"
  exit 0
fi

# ---------------------------------------------------------------------------
# 2) 演示账号（密码统一为 demo123456）
#    摘要格式与后端 PasswordUtil 一致：base64(salt):base64(SHA256(salt||password))
#    此处使用固定盐 drs-demo-salt-01 预先算好，脚本因此无需依赖后端实现。
# ---------------------------------------------------------------------------
echo "==> 插入演示账号（密码 demo123456）…"
run_sql <<SQL
INSERT INTO sys_user (id, username, real_name, role, phone, status, password,
                      create_by, create_time, update_by, update_time, delete_flag)
VALUES
  (REPLACE(UUID(),'-',''), 'demo_doctor_lin', '林医生（演示）', 'DOCTOR', '13800000001', 'ENABLED',
   'ZHJzLWRlbW8tc2FsdC0wMQ==:AT6h+ie3qoZ2TiHmSsA2LRI3xpMSdT6GGf5NgLtUCcw=', 'seed', NOW(), 'seed', NOW(), 'N'),
  (REPLACE(UUID(),'-',''), 'demo_doctor_wu',  '吴医生（演示）', 'DOCTOR', '13800000002', 'ENABLED',
   'ZHJzLWRlbW8tc2FsdC0wMQ==:AT6h+ie3qoZ2TiHmSsA2LRI3xpMSdT6GGf5NgLtUCcw=', 'seed', NOW(), 'seed', NOW(), 'N');
SQL

# ---------------------------------------------------------------------------
# 3) 演示筛查记录
#    - 覆盖 LEVEL_0..LEVEL_4 与三种转诊建议；
#    - 含 2 条低置信度记录（触发「待复核」）；
#    - 含 3 位患者的多次筛查（触发「患者随访」的分级变化）；
#    - 含 100 天前的记录（触发「逾期未复诊」）；
#    - 时间跨近 30 天与前 30 天（触发「统计环比」）。
# ---------------------------------------------------------------------------
echo "==> 插入演示筛查记录…"
run_sql <<SQL
INSERT INTO biz_screening_record
  (id, user_id, patient_name, patient_age, patient_gender, image_key,
   result_level, confidence, probabilities, suggestion,
   model_version, remark, review_status, reviewer, review_time, review_remark,
   create_by, create_time, update_by, update_time, delete_flag)
VALUES
  -- 张伟：中度 → 重度（进展），含一条已复核
  (REPLACE(UUID(),'-',''), 'admin', '演示患者-张伟', 62, 'MALE', 'demo/placeholder.png',
   'LEVEL_2', 0.9120, '{"LEVEL_0":0.02,"LEVEL_1":0.03,"LEVEL_2":0.912,"LEVEL_3":0.03,"LEVEL_4":0.008}',
   'CLINIC', 'aptos2019-mobilenetv3s-1.0.0', '$DEMO_REMARK', NULL, NULL, NULL, NULL,
   'seed', DATE_SUB(NOW(), INTERVAL 120 DAY), 'seed', NOW(), 'N'),
  (REPLACE(UUID(),'-',''), 'admin', '演示患者-张伟', 62, 'MALE', 'demo/placeholder.png',
   'LEVEL_3', 0.8830, '{"LEVEL_0":0.005,"LEVEL_1":0.01,"LEVEL_2":0.09,"LEVEL_3":0.883,"LEVEL_4":0.012}',
   'REFERRAL', 'aptos2019-mobilenetv3s-1.0.0', '$DEMO_REMARK', 'CONFIRMED', 'admin',
   DATE_SUB(NOW(), INTERVAL 8 DAY), '已核对影像，分级无误，建议转诊',
   'seed', DATE_SUB(NOW(), INTERVAL 8 DAY), 'seed', NOW(), 'N'),
  -- 李娜：正常 → 轻度（好转）
  (REPLACE(UUID(),'-',''), 'admin', '演示患者-李娜', 55, 'FEMALE', 'demo/placeholder.png',
   'LEVEL_1', 0.7640, '{"LEVEL_0":0.2,"LEVEL_1":0.764,"LEVEL_2":0.03,"LEVEL_3":0.005,"LEVEL_4":0.001}',
   'REVIEW', 'aptos2019-mobilenetv3s-1.0.0', '$DEMO_REMARK', NULL, NULL, NULL, NULL,
   'seed', DATE_SUB(NOW(), INTERVAL 95 DAY), 'seed', NOW(), 'N'),
  (REPLACE(UUID(),'-',''), 'admin', '演示患者-李娜', 55, 'FEMALE', 'demo/placeholder.png',
   'LEVEL_0', 0.9510, '{"LEVEL_0":0.951,"LEVEL_1":0.04,"LEVEL_2":0.008,"LEVEL_3":0.001,"LEVEL_4":0.0}',
   'REVIEW', 'aptos2019-mobilenetv3s-1.0.0', '$DEMO_REMARK', NULL, NULL, NULL, NULL,
   'seed', DATE_SUB(NOW(), INTERVAL 12 DAY), 'seed', NOW(), 'N'),
  -- 王强：增殖期，低置信度 → 待复核
  (REPLACE(UUID(),'-',''), 'admin', '演示患者-王强', 68, 'MALE', 'demo/placeholder.png',
   'LEVEL_4', 0.5230, '{"LEVEL_0":0.01,"LEVEL_1":0.02,"LEVEL_2":0.15,"LEVEL_3":0.297,"LEVEL_4":0.523}',
   'REFERRAL', 'aptos2019-mobilenetv3s-1.0.0', '$DEMO_REMARK', NULL, NULL, NULL, NULL,
   'seed', DATE_SUB(NOW(), INTERVAL 45 DAY), 'seed', NOW(), 'N'),
  -- 赵敏：中度但置信度低 → 待复核
  (REPLACE(UUID(),'-',''), 'admin', '演示患者-赵敏', 49, 'FEMALE', 'demo/placeholder.png',
   'LEVEL_2', 0.6120, '{"LEVEL_0":0.05,"LEVEL_1":0.14,"LEVEL_2":0.612,"LEVEL_3":0.15,"LEVEL_4":0.048}',
   'CLINIC', 'aptos2019-mobilenetv3s-1.0.0', '$DEMO_REMARK', NULL, NULL, NULL, NULL,
   'seed', DATE_SUB(NOW(), INTERVAL 20 DAY), 'seed', NOW(), 'N'),
  -- 近 30 天补充记录（用于环比与趋势）
  (REPLACE(UUID(),'-',''), 'admin', '演示患者-陈静', 71, 'FEMALE', 'demo/placeholder.png',
   'LEVEL_3', 0.9020, '{"LEVEL_0":0.004,"LEVEL_1":0.008,"LEVEL_2":0.06,"LEVEL_3":0.902,"LEVEL_4":0.026}',
   'REFERRAL', 'aptos2019-mobilenetv3s-1.0.0', '$DEMO_REMARK', NULL, NULL, NULL, NULL,
   'seed', DATE_SUB(NOW(), INTERVAL 5 DAY), 'seed', NOW(), 'N'),
  (REPLACE(UUID(),'-',''), 'admin', '演示患者-刘洋', 44, 'MALE', 'demo/placeholder.png',
   'LEVEL_0', 0.9780, '{"LEVEL_0":0.978,"LEVEL_1":0.018,"LEVEL_2":0.003,"LEVEL_3":0.001,"LEVEL_4":0.0}',
   'REVIEW', 'aptos2019-mobilenetv3s-1.0.0', '$DEMO_REMARK', NULL, NULL, NULL, NULL,
   'seed', DATE_SUB(NOW(), INTERVAL 3 DAY), 'seed', NOW(), 'N'),
  (REPLACE(UUID(),'-',''), 'admin', '演示患者-孙丽', 58, 'FEMALE', 'demo/placeholder.png',
   'LEVEL_1', 0.8010, '{"LEVEL_0":0.15,"LEVEL_1":0.801,"LEVEL_2":0.04,"LEVEL_3":0.008,"LEVEL_4":0.001}',
   'REVIEW', 'aptos2019-mobilenetv3s-1.0.0', '$DEMO_REMARK', NULL, NULL, NULL, NULL,
   'seed', DATE_SUB(NOW(), INTERVAL 1 DAY), 'seed', NOW(), 'N'),
  -- 前 30 天记录（用于环比对照）
  (REPLACE(UUID(),'-',''), 'admin', '演示患者-周涛', 66, 'MALE', 'demo/placeholder.png',
   'LEVEL_2', 0.8450, '{"LEVEL_0":0.02,"LEVEL_1":0.05,"LEVEL_2":0.845,"LEVEL_3":0.07,"LEVEL_4":0.015}',
   'CLINIC', 'aptos2019-mobilenetv3s-1.0.0', '$DEMO_REMARK', NULL, NULL, NULL, NULL,
   'seed', DATE_SUB(NOW(), INTERVAL 50 DAY), 'seed', NOW(), 'N'),
  (REPLACE(UUID(),'-',''), 'admin', '演示患者-吴霞', 52, 'FEMALE', 'demo/placeholder.png',
   'LEVEL_0', 0.9320, '{"LEVEL_0":0.932,"LEVEL_1":0.055,"LEVEL_2":0.011,"LEVEL_3":0.002,"LEVEL_4":0.0}',
   'REVIEW', 'aptos2019-mobilenetv3s-1.0.0', '$DEMO_REMARK', NULL, NULL, NULL, NULL,
   'seed', DATE_SUB(NOW(), INTERVAL 40 DAY), 'seed', NOW(), 'N');
SQL

# ---------------------------------------------------------------------------
# 4) 汇总
# ---------------------------------------------------------------------------
echo "==> 完成。当前演示数据统计："
run_sql <<SQL
SELECT '演示账号' AS 项目, COUNT(*) AS 数量 FROM sys_user WHERE username LIKE 'demo\_%'
UNION ALL
SELECT '演示筛查记录', COUNT(*) FROM biz_screening_record WHERE remark = '$DEMO_REMARK'
UNION ALL
SELECT '其中待复核', COUNT(*) FROM biz_screening_record
  WHERE remark = '$DEMO_REMARK' AND confidence < 0.70
    AND (review_status IS NULL OR review_status = 'PENDING');
SQL
echo "提示：清理演示数据执行 bash tools/seed-demo-data.sh --clean"
