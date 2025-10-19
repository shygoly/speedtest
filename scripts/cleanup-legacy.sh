#!/usr/bin/env bash
set -euo pipefail

# 安全的清理脚本（仅打印建议删除项，不直接删除）
# 使用方法：
#   ./scripts/cleanup-legacy.sh --apply    # 实际删除（危险）

TARGETS=(
  "../SwiftestPlus"
  "../SwiftestSDK"
  "../Swiftest-Web-main"
)

APPLY=0
if [[ "${1-}" == "--apply" ]]; then
  APPLY=1
fi

echo "将要清理的历史目录（建议先备份/迁移重要资源后再删除）："
for t in "${TARGETS[@]}"; do
  if [[ -e "$t" ]]; then
    echo "  - $t"
  fi
done

if [[ $APPLY -eq 1 ]]; then
  read -p "确认删除以上目录? (yes/NO): " ans
  if [[ "$ans" == "yes" ]]; then
    for t in "${TARGETS[@]}"; do
      if [[ -e "$t" ]]; then
        rm -rf "$t"
        echo "已删除: $t"
      fi
    done
  else
    echo "已取消"
  fi
else
  echo "这是预览模式。若确认无误，可执行: ./scripts/cleanup-legacy.sh --apply"
fi
