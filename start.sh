#!/usr/bin/env bash
# ============================================================
# 启动 newbee-mall。用法：./start.sh [profile]
#   profile 默认取环境变量 SPRING_PROFILES_ACTIVE，再回退到 prod
# 可选环境变量：JAR_PATH、JAVA_HOME、JAVA_OPTS
# ============================================================
set -euo pipefail
cd "$(dirname "$0")"

APP_NAME="newbee-mall"
PROFILE="${SPRING_PROFILES_ACTIVE:-${1:-prod}}"
PID_FILE="./${APP_NAME}.pid"
LOG_DIR="./logs"
LOG_FILE="${LOG_DIR}/${APP_NAME}.out"
JAVA_OPTS="${JAVA_OPTS:--Xms256m -Xmx512m}"

if [ -n "${JAVA_HOME:-}" ]; then
  JAVA_BIN="${JAVA_HOME}/bin/java"
else
  JAVA_BIN="java"
fi

# 定位可执行 jar
if [ -n "${JAR_PATH:-}" ]; then
  JAR="${JAR_PATH}"
elif [ -f "target/${APP_NAME}.jar" ]; then
  JAR="target/${APP_NAME}.jar"
elif [ -f "./${APP_NAME}.jar" ]; then
  JAR="./${APP_NAME}.jar"
else
  echo "[ERROR] 未找到可执行 jar，请先执行 mvn -Pprod package 或设置 JAR_PATH" >&2
  exit 1
fi

# 已在运行则不重复启动
if [ -f "${PID_FILE}" ]; then
  OLD_PID="$(cat "${PID_FILE}")"
  if kill -0 "${OLD_PID}" 2>/dev/null; then
    echo "[WARN] ${APP_NAME} 已在运行 (PID=${OLD_PID})"
    exit 0
  fi
  rm -f "${PID_FILE}"
fi

mkdir -p "${LOG_DIR}"
echo "[INFO] 启动 ${APP_NAME}，profile=${PROFILE}，jar=${JAR}"
nohup "${JAVA_BIN}" ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom \
  -jar "${JAR}" --spring.profiles.active="${PROFILE}" > "${LOG_FILE}" 2>&1 &
echo $! > "${PID_FILE}"
echo "[INFO] 已启动 (PID=$(cat "${PID_FILE}"))，日志：${LOG_FILE}"
