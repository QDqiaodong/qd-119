#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

if [ ! -f .env ]; then
  echo "[ERROR] 未找到 .env 文件，请先创建 .env 配置文件"
  exit 1
fi

source .env

echo "========================================"
echo "  开始构建项目: ${PROJECT_NAME}"
echo "========================================"
echo ""

docker compose up -d --build

echo ""
echo "========================================"
echo "  构建启动完成！"
echo "========================================"
echo ""
echo "  前端访问地址:  http://localhost:${FRONTEND_PORT}"
echo "  后端API地址:   http://localhost:${BACKEND_PORT}"
echo "  MySQL地址:     127.0.0.1:${MYSQL_PORT}"
echo "  Redis地址:     127.0.0.1:${REDIS_PORT}"
echo ""
echo "  所有服务均绑定 127.0.0.1"
echo ""
