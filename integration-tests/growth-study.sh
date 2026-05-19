#!/usr/bin/env bash
# Controlled artifact-size growth study.
# Generates N identical @Remote / @Rpc functions, compiles, records JAR size.
#
# Outputs CSV to stdout: framework,N,jar_bytes,total_classes,total_class_bytes

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

KR_FILE=growth-bench/src/main/kotlin/growthbench/GeneratedApi.kt
KRPC_FILE=growth-bench-kotlinrpc/src/main/kotlin/growthbench/GeneratedApi.kt

gen_kotlin_remote() {
  local N=$1
  local with_map=$2  # "with_map" or "no_map"
  {
    echo "package growthbench"
    echo "import kotlinx.remote.Remote"
    echo "import kotlinx.remote.RemoteContext"
    echo "import kotlinx.remote.CallableMap"
    echo "import kotlinx.remote.genCallableMap"
    echo ""
    if [ "$with_map" = "with_map" ]; then
      echo "val GENERATED_MAP: CallableMap = genCallableMap()"
      echo ""
    fi
    if [ "$N" -gt 0 ]; then
      for i in $(seq 0 $((N-1))); do
        echo "@Remote"
        echo "context(_: RemoteContext<GrowthConfig>)"
        echo "suspend fun f${i}(x: Long): Long = x"
      done
    fi
  } > "$KR_FILE"
}

gen_kotlinrpc() {
  local N=$1
  {
    echo "package growthbench"
    echo "import kotlinx.rpc.annotations.Rpc"
    echo ""
    echo "@Rpc"
    echo "interface GrowthService {"
    if [ "$N" -gt 0 ]; then
      for i in $(seq 0 $((N-1))); do
        echo "    suspend fun f${i}(x: Long): Long"
      done
    fi
    echo "}"
  } > "$KRPC_FILE"
}

measure_module() {
  local module=$1
  ./gradlew ":${module}:clean" ":${module}:jar" --no-daemon -q --rerun-tasks > /dev/null
  local jar
  jar=$(find "${module}/build/libs" -name "*.jar" | head -1)
  local jar_bytes
  jar_bytes=$(stat -c%s "$jar")
  local classes_dir="${module}/build/classes/atomicfu-orig/main"
  if [ ! -d "$classes_dir" ]; then
    classes_dir="${module}/build/classes/kotlin/main"
  fi
  local total_classes
  total_classes=$(find "$classes_dir" -name "*.class" 2>/dev/null | wc -l)
  local total_class_bytes
  total_class_bytes=$(find "$classes_dir" -name "*.class" -exec stat -c%s {} \; 2>/dev/null | awk '{s+=$1} END {print s+0}')
  echo "$jar_bytes,$total_classes,$total_class_bytes"
}

echo "framework,N,jar_bytes,total_classes,total_class_bytes"
# Kotlin Remote, no genCallableMap call: measures pure @Remote function body cost.
for N in 0 1 10 100 1000; do
  gen_kotlin_remote "$N" no_map
  out=$(measure_module growth-bench || echo "FAIL,FAIL,FAIL")
  echo "kotlin-remote-bare,$N,$out"
done
# Kotlin Remote, with one top-level genCallableMap() call.
for N in 0 1 10 100 200 500 800 900 1000; do
  gen_kotlin_remote "$N" with_map
  out=$(measure_module growth-bench || echo "FAIL,FAIL,FAIL")
  echo "kotlin-remote-genmap,$N,$out"
done
# kotlinx.rpc with one @Rpc interface holding N methods.
for N in 0 1 10 100 200 500 800 900 1000; do
  gen_kotlinrpc "$N"
  out=$(measure_module growth-bench-kotlinrpc || echo "FAIL,FAIL,FAIL")
  echo "kotlinx.rpc,$N,$out"
done
