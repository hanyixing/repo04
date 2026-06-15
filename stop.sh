#!/usr/bin/env bash
# ============================================================
# newbee-mall 停止脚本
# 用法：./stop.sh
# ============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

GREEN='\033[0;32m'
NC='\033[0m'

info() { echo -e "${GREEN}[INFO]${NC} $*"; }

info "停止 newbee-mall 服务..."
docker-compose down

info "服务已停止"
docker-compose ps 2>/dev/null || true
