#!/usr/bin/env bash
# ============================================================
# 部署 newbee-mall。用法：
#   ./deploy.sh            # jar 模式：可选拉取代码 -> prod 打包 -> 重启
#   ./deploy.sh docker     # docker 模式：docker compose 构建并启动
# 可选环境变量：SKIP_GIT=true 跳过 git pull；JAVA_HOME 指定构建用 JDK
# ============================================================
set -euo pipefail
cd "$(dirname "$0")"

MODE="${1:-jar}"

# ---------- Docker 模式 ----------
if [ "${MODE}" = "docker" ]; then
  echo "[INFO] 使用 docker compose 部署 ..."
  if docker compose version >/dev/null 2>&1; then
    docker compose up -d --build
  else
    docker-compose up -d --build
  fi
  echo "[INFO] docker 部署完成"
  exit 0
fi

# ---------- Jar 模式 ----------
if [ "${SKIP_GIT:-false}" != "true" ] && [ -d .git ]; then
  echo "[INFO] 拉取最新代码 ..."
  git pull --ff-only || echo "[WARN] git pull 跳过/失败，使用当前代码继续"
fi

# 构建需要 JDK；若设置了 JAVA_HOME 则透传给 mvn
if [ -n "${JAVA_HOME:-}" ]; then
  echo "[INFO] 使用 JAVA_HOME=${JAVA_HOME} 进行构建"
fi

echo "[INFO] 以 prod profile 打包（跳过测试）..."
JAVA_HOME="${JAVA_HOME:-}" mvn -B -Pprod clean package -DskipTests

echo "[INFO] 重启应用 ..."
bash ./stop.sh || true
SPRING_PROFILES_ACTIVE=prod bash ./start.sh prod
echo "[INFO] 部署完成"
