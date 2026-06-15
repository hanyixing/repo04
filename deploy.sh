#!/usr/bin/env bash
# ============================================================
# newbee-mall 全量部署脚本
# 用法：./deploy.sh [--build]
#   --build  强制重新构建镜像（默认仅在有变更时构建）
# ============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

info()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error() { echo -e "${RED}[ERROR]${NC} $*"; exit 1; }

# 检查 .env 文件
if [ ! -f .env ]; then
    warn ".env 文件不存在，从 .env.example 复制..."
    if [ -f .env.example ]; then
        cp .env.example .env
        warn "请编辑 .env 文件并填入实际值后重新执行部署"
        exit 1
    else
        error "缺少 .env.example，无法生成 .env"
    fi
fi

FORCE_BUILD=false
if [ "${1:-}" = "--build" ]; then
    FORCE_BUILD=true
fi

# 停止旧服务
info "停止现有服务..."
docker-compose down --remove-orphans 2>/dev/null || true

# 构建镜像
if [ "$FORCE_BUILD" = true ]; then
    info "强制重新构建 Docker 镜像..."
    docker-compose build --no-cache
else
    info "构建 Docker 镜像..."
    docker-compose build
fi

# 启动服务
info "启动服务..."
docker-compose up -d

# 健康检查
info "等待服务启动（最多 120 秒）..."
MAX_WAIT=120
WAITED=0
while [ $WAITED -lt $MAX_WAIT ]; do
    if curl -sf http://localhost:${APP_PORT:-28089}/admin/login > /dev/null 2>&1; then
        info "服务启动成功！"
        info "访问地址: http://localhost:${APP_PORT:-28089}"
        docker-compose ps
        exit 0
    fi
    sleep 5
    WAITED=$((WAITED + 5))
    echo -n "."
done

echo ""
error "服务在 ${MAX_WAIT} 秒内未就绪，请检查日志: docker-compose logs newbee-mall"
