#!/usr/bin/env bash
# 物业楼栋外墙渗水季节复查系统 —— 一条命令启动
# 用法: ./start.sh          构建前端 + 启动后端（首次会下载依赖，耗时几分钟）
#       ./start.sh --rebuild  强制重新构建后端
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"

# ---- 定位 Java / Maven（优先系统环境，其次本仓库使用的便携版） ----
if [ -z "${JAVA_HOME:-}" ] && [ -x "$HOME/tools/jdk/bin/java" ]; then
  export JAVA_HOME="$HOME/tools/jdk"
fi
if [ -n "${JAVA_HOME:-}" ]; then
  export PATH="$JAVA_HOME/bin:$PATH"
fi
if ! command -v java >/dev/null 2>&1; then
  echo "未找到 Java，请安装 JDK 17+ 或设置 JAVA_HOME" >&2
  exit 1
fi

MVN=(mvn)
if ! command -v mvn >/dev/null 2>&1; then
  if [ -x "$HOME/tools/maven/bin/mvn" ]; then
    MVN=("$HOME/tools/maven/bin/mvn")
  else
    echo "未找到 Maven，请安装 Maven 3.9+" >&2
    exit 1
  fi
fi
# 使用项目自带镜像配置（部分网络环境拦截 repo.maven.apache.org 的 Java TLS）
# 并限制单线程下载（并发 TLS 握手易被网络中间设备重置）
MVN+=(-s "$ROOT/maven-settings.xml" -Dmaven.artifact.threads=1)

# ---- 1. 构建前端（Svelte → Quarkus 静态资源目录） ----
echo "==> [1/3] 构建前端"
if [ ! -d "$ROOT/frontend/node_modules" ]; then
  (cd "$ROOT/frontend" && npm install --no-audit --no-fund)
fi
(cd "$ROOT/frontend" && npm run build)

# ---- 2. 构建后端（Quarkus + Hibernate + SQLite） ----
echo "==> [2/3] 构建后端"
mkdir -p "$ROOT/backend/data/photos"
cd "$ROOT/backend"
if [ "${1:-}" = "--rebuild" ] || [ ! -f target/quarkus-app/quarkus-run.jar ]; then
  "${MVN[@]}" -q package -DskipTests
fi

# ---- 3. 启动 ----
echo "==> [3/3] 启动系统"
echo "    访问 http://localhost:8080  （Ctrl+C 停止）"
exec java -jar target/quarkus-app/quarkus-run.jar
