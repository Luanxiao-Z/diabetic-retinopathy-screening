#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
演示数据生成脚本（带真实影像）。

与 tools/seed-demo-data.sh 的区别：
  - seed-demo-data.sh 直接插库、无影像（依赖最少，适合快速演示）；
  - 本脚本通过「筛查上传」接口逐张上传 datasets 中的真实眼底图，由后端完成
    MinIO 上传 → 模型推理 → Grad-CAM 热力图 → 落库，因此记录**带真实影像与热力图**，
    且 uncertainty（归一化熵）由真实推理结果计算，可直接演示「待人工复核」。

流程：
  1. 清理上一次由本脚本写入的演示记录（remark = 演示数据（脚本生成））；
  2. 登录（图形验证码从 Redis 旁路读取）；
  3. 按「患者 / 分级目录 / 就诊天数」清单逐张上传；
  4. 按清单回填 create_time，构造进展/好转、逾期未复诊、统计环比等场景。

依赖：Python 3.13 + requests；本机 MySQL 客户端；Docker（读取验证码）。
环境变量：DB_PASSWORD / REDIS_PASSWORD（Windows 用户级环境变量已注入）。
用法（仓库根目录）：
  py -3.13 tools/gen-demo-data.py            # 清理并重建
  py -3.13 tools/gen-demo-data.py --clean    # 仅清理
"""
from __future__ import annotations

import argparse
import glob
import json
import os
import subprocess
import sys

import requests

API = os.environ.get("DRS_API_BASE", "http://127.0.0.1:8080/api/v1")
MYSQL_BIN = os.environ.get("MYSQL_BIN", r"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe")
DB_HOST = os.environ.get("DB_HOST", "127.0.0.1")
DB_PORT = os.environ.get("DB_PORT", "3306")
DB_NAME = os.environ.get("DB_NAME", "dr_screening")
DB_USER = os.environ.get("DB_USER", "root")
REDIS_CONTAINER = os.environ.get("REDIS_CONTAINER", "fjzhmz-redis")
REDIS_PORT = os.environ.get("REDIS_PORT", "26379")
REMARK = "演示数据（脚本生成）"
IMG_ROOT = os.path.join("datasets", "aptos2019_224x224", "colored_images")

# (患者, 年龄, 性别, 分级目录, 就诊距今天数)
# 同一患者多条用于「分级变化」；>90 天且中度以上用于「逾期未复诊」；跨 30 天用于「环比」
DEMO = [
    ("演示患者-张伟", 62, "MALE", "No_DR", 120),
    ("演示患者-张伟", 62, "MALE", "Severe", 8),
    ("演示患者-李娜", 55, "FEMALE", "Mild", 95),
    ("演示患者-李娜", 55, "FEMALE", "No_DR", 12),
    ("演示患者-王强", 68, "MALE", "Proliferate_DR", 45),
    ("演示患者-赵敏", 49, "FEMALE", "Moderate", 100),
    ("演示患者-陈静", 71, "FEMALE", "Severe", 5),
    ("演示患者-刘洋", 44, "MALE", "No_DR", 3),
    ("演示患者-孙丽", 58, "FEMALE", "Mild", 1),
    ("演示患者-周涛", 66, "MALE", "Moderate", 50),
    ("演示患者-吴霞", 52, "FEMALE", "No_DR", 40),
    ("演示患者-郑凯", 57, "MALE", "Severe", 20),
    ("演示患者-冯敏", 63, "FEMALE", "Proliferate_DR", 15),
    ("演示患者-韩雪", 39, "FEMALE", "Mild", 10),
    ("演示患者-曹阳", 47, "MALE", "Moderate", 25),
    ("演示患者-沈丽", 60, "FEMALE", "No_DR", 30),
]


def mysql(sql: str, database: str | None = None) -> str:
    env = dict(os.environ, MYSQL_PWD=os.environ["DB_PASSWORD"])
    cmd = [MYSQL_BIN, "--default-character-set=utf8mb4", "-h", DB_HOST, "-P", DB_PORT, "-u", DB_USER]
    if database:
        cmd.append(database)
    return subprocess.run(cmd, input=sql.encode("utf-8"), capture_output=True, env=env, check=True).stdout.decode("utf-8", "replace")


def login() -> str:
    cap = requests.get(f"{API}/auth/captcha", timeout=20).json()
    key = cap["data"]["captchaKey"]
    code = subprocess.run(
        ["docker", "exec", REDIS_CONTAINER, "redis-cli", "-p", REDIS_PORT, "-a",
         os.environ["REDIS_PASSWORD"], "--no-auth-warning", "get", f"drs:auth:captcha:{key}"],
        capture_output=True, check=True,
    ).stdout.decode("utf-8", "replace").strip()
    res = requests.post(
        f"{API}/auth/sessions",
        json={"username": "admin", "password": "admin123", "captchaKey": key, "captchaCode": code},
        timeout=20,
    ).json()
    return res["data"]["token"]


def image_pool() -> dict[str, list[str]]:
    pools: dict[str, list[str]] = {}
    for name in os.listdir(IMG_ROOT):
        d = os.path.join(IMG_ROOT, name)
        if os.path.isdir(d):
            pools[name] = sorted(
                glob.glob(os.path.join(d, "*.png")) + glob.glob(os.path.join(d, "*.jpg"))
            )
    return pools


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--clean", action="store_true", help="仅清理演示记录")
    args = parser.parse_args()

    print("==> 清理既有演示记录…")
    mysql(f"DELETE FROM biz_screening_record WHERE remark='{REMARK}';", DB_NAME)

    if args.clean:
        print("==> 仅清理模式，完成。")
        return 0

    print("==> 登录…")
    token = login()
    headers = {"X-Access-Token": token}
    pools = image_pool()
    cursor: dict[str, int] = {}
    created: list[tuple[str, int]] = []

    for name, age, gender, folder, days in DEMO:
        files = pools.get(folder) or []
        if not files:
            print(f"  [跳过] {name}: 目录 {folder} 无图片")
            continue
        idx = cursor.get(folder, 0) % len(files)
        cursor[folder] = idx + 1
        path = files[idx]
        with open(path, "rb") as fh:
            resp = requests.post(
                f"{API}/biz/screening-records",
                headers=headers,
                files=[("files", (os.path.basename(path), fh, "image/png"))],
                data={
                    "patientName": name,
                    "patientAge": str(age),
                    "patientGender": gender,
                    "remark": REMARK,
                },
                timeout=120,
            )
        body = resp.json()
        if body.get("code") != 200 or not body.get("data"):
            print(f"  [失败] {name}: {body.get('msg')}")
            continue
        rec = body["data"][0]
        created.append((rec["id"], days))
        print(f"  [完成] {name} -> {rec.get('levelName')} / conf={rec.get('confidence')} / unc={rec.get('uncertainty')}")

    print("==> 回填就诊日期…")
    for rec_id, days in created:
        mysql(
            f"UPDATE biz_screening_record SET create_time=DATE_SUB(NOW(), INTERVAL {days} DAY), "
            f"update_time=DATE_SUB(NOW(), INTERVAL {days} DAY) WHERE id='{rec_id}';",
            DB_NAME,
        )

    print("==> 完成。演示记录数：", len(created))
    return 0


if __name__ == "__main__":
    sys.exit(main())
