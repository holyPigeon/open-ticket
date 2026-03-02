#!/usr/bin/env bash
set -euo pipefail
TARGET_FD=65536

soft=$(ulimit -n 2>/dev/null || echo 0)

# unlimited이거나 이미 충분하면 그대로
if [[ "$soft" != "unlimited" ]] && (( soft < TARGET_FD )); then
  ulimit -n $TARGET_FD || {
    echo "ERROR: ulimit -n $TARGET_FD 실패 (hard limit=$(ulimit -Hn))"
    echo "해결: 새 터미널에서 sudo launchctl limit maxfiles $TARGET_FD 200000 실행 후 재시도"
    exit 1
  }
  echo "[fd] ulimit -n $TARGET_FD 적용"
fi

echo "[fd] 현재 제한: $(ulimit -n)"
exec k6 run queue-load-test.js "$@"
