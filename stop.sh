#!/usr/bin/env bash
# ============================================================
# 停止 newbee-mall：先尝试优雅停止，超时后强制结束
# 可选环境变量：STOP_TIMEOUT（优雅停止等待秒数，默认 30）
# ============================================================
set -euo pipefail
cd "$(dirname "$0")"

APP_NAME="newbee-mall"
PID_FILE="./${APP_NAME}.pid"
TIMEOUT="${STOP_TIMEOUT:-30}"

if [ ! -f "${PID_FILE}" ]; then
  echo "[WARN] 未找到 PID 文件，${APP_NAME} 可能未在运行"
  exit 0
fi

PID="$(cat "${PID_FILE}")"
if ! kill -0 "${PID}" 2>/dev/null; then
  echo "[WARN] 进程 ${PID} 不存在，清理 PID 文件"
  rm -f "${PID_FILE}"
  exit 0
fi

echo "[INFO] 正在停止 ${APP_NAME} (PID=${PID}) ..."
kill "${PID}"
for ((i=0; i<TIMEOUT; i++)); do
  if ! kill -0 "${PID}" 2>/dev/null; then
    rm -f "${PID_FILE}"
    echo "[INFO] ${APP_NAME} 已停止"
    exit 0
  fi
  sleep 1
done

echo "[WARN] 优雅停止超时，强制结束 (PID=${PID})"
kill -9 "${PID}" 2>/dev/null || true
rm -f "${PID_FILE}"
echo "[INFO] ${APP_NAME} 已强制停止"
