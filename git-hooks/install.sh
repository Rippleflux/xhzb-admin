#!/bin/bash
# Git Hooks 安装脚本
# 使用: bash install.sh

HOOKS_DIR="$(cd "$(dirname "$0")" && pwd)"
GIT_HOOKS_DIR="$(cd "$HOOKS_DIR/.." && pwd)/.git/hooks"

if [ ! -d "$GIT_HOOKS_DIR" ]; then
    echo "❌ 未找到 .git/hooks 目录，请确认当前在 Git 仓库根目录"
    exit 1
fi

echo "安装 Git Hooks → $GIT_HOOKS_DIR"

for hook in commit-msg pre-commit pre-push; do
    if [ -f "$HOOKS_DIR/$hook" ]; then
        cp "$HOOKS_DIR/$hook" "$GIT_HOOKS_DIR/$hook"
        chmod +x "$GIT_HOOKS_DIR/$hook"
        echo "  ✅ $hook"
    fi
done

# 合并发布脚本（安装到 .git/hooks/ 方便发现，也保留在 git-hooks/）
cp "$HOOKS_DIR/merge-dev-to-master.sh" "$SCRIPT_DIR/../.git/hooks/merge-dev-to-master.sh"
chmod +x "$SCRIPT_DIR/../.git/hooks/merge-dev-to-master.sh"

echo ""
echo "安装完成。以下操作将触发校验:"
echo "  commit-msg → git commit 时校验提交信息格式"
echo "  pre-commit → git commit 时校验文件命名/敏感信息/冲突标记"
echo "  pre-push   → git push 时校验分支命名/禁止推master"
echo ""
echo "发布流程:"
echo "  bash git-hooks/merge-dev-to-master.sh [版本号]"
echo ""
echo "如需临时绕过: git <command> --no-verify"
