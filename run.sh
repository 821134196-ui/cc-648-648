#!/usr/bin/env bash
# 一条本地命令启动：构建 Svelte 前端 → 打包 Quarkus（含前端静态资源）→ 启动服务
# 用法：./run.sh        首次会自动安装前端依赖、下载 Maven 依赖
#       ./run.sh fresh  删除数据库与照片后以演示种子重新启动
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

# ---- Java / Maven：优先使用项目内本地安装（本机无系统 JDK 时用 ~/opt）----
LOCAL_JDK="$(ls -d "$HOME"/opt/jdk-21* 2>/dev/null | head -1 || true)"
if [ -n "$LOCAL_JDK" ]; then
  export JAVA_HOME="$LOCAL_JDK"
  export PATH="$JAVA_HOME/bin:$PATH"
fi
MVN="mvn"
LOCAL_MVN="$(ls -d "$HOME"/opt/apache-maven-* 2>/dev/null | head -1 || true)"
if [ -n "$LOCAL_MVN" ] && ! command -v mvn >/dev/null 2>&1; then
  export PATH="$LOCAL_MVN/bin:$PATH"
fi

command -v java >/dev/null 2>&1 || { echo "❌ 未找到 java，请安装 JDK 21+（或解压到 ~/opt/jdk-21*）"; exit 1; }
command -v npm  >/dev/null 2>&1 || { echo "❌ 未找到 npm，请安装 Node.js 18+"; exit 1; }

if [ "${1:-}" = "fresh" ]; then
  echo "🧹 清空数据库与照片，恢复演示种子..."
  rm -f "$ROOT/data/leak.db" "$ROOT/data/leak.db-"* 2>/dev/null || true
  rm -rf "$ROOT/data/photos"
fi
mkdir -p "$ROOT/data/photos"

# ---- 前端 ----
echo "🖥️  构建 Svelte 前端..."
cd "$ROOT/frontend"
[ -d node_modules ] || npm install
npm run build

# ---- 后端（打包时把 frontend/dist 并入 META-INF/resources）----
echo "☕ 打包 Quarkus 后端..."
cd "$ROOT"
mvn -B package -DskipTests

echo "🚀 启动服务：http://localhost:8080"
echo "   （演示三个场景：3栋两户关联待降雨复查 / 7栋复发报修 / 5栋异议处理）"
exec java -jar "$ROOT/target/quarkus-app/quarkus-run.jar"
