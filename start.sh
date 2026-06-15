#!/usr/bin/env bash
# ============================================================
# newbee-mall 启动脚本
# 用法：./start.sh
# ============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

info() { echo -e "${GREEN}[INFO]${NC} $*"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $*"; }

if [ ! -f .env ]; then
    warn ".env 文件不存在，请先执行 deploy.sh 或从 .env.example 复制"
    if [ -f .env.example ]; then
        cp .env.example .env
        warn "已从 .env.example 复制 .env，请编辑后重新执行"
        exit 1
    fi
fi

info "启动 newbee-mall 服务..."
docker-compose up -d

info "当前服务状态："
docker-compose ps

info "查看日志: docker-compose logs -f newbee-mall"
