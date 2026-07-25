#!/bin/bash
# ============================================================
# merge-dev-to-master.sh — dev → master 合并 + 自动打 Tag
# 
# 工作流:
#   1. feature/* 分支开发完成 → 合并到 dev
#   2. dev 环境测试通过后 → 运行此脚本
#   3. 自动合并 dev→master + 生成版本 Tag + 推送
#
# 使用: bash git-hooks/merge-dev-to-master.sh [版本号]
#       不传版本号则自动递增 patch 版本
# ============================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$REPO_ROOT"

# ============================================================
# 0. 前置检查
# ============================================================
CURRENT_BRANCH=$(git branch --show-current)
if [ "$CURRENT_BRANCH" != "master" ] && [ "$CURRENT_BRANCH" != "main" ]; then
    echo "❌ 请在 master 分支上运行此脚本 (当前: $CURRENT_BRANCH)"
    echo "   git checkout master"
    exit 1
fi

# 确保工作区干净
if ! git diff-index --quiet HEAD --; then
    echo "❌ 工作区不干净，请先提交或暂存变更"
    git status --short
    exit 1
fi

# 确保 dev 分支存在
if ! git show-ref --verify --quiet refs/heads/dev; then
    echo "❌ dev 分支不存在"
    exit 1
fi

# ============================================================
# 1. 拉取最新代码
# ============================================================
echo "🔄 拉取最新代码..."
git fetch origin

# 检查 dev 是否有未合并到 master 的提交
BEHIND=$(git rev-list master..origin/dev --count 2>/dev/null || echo "0")
if [ "$BEHIND" -eq 0 ]; then
    echo "✅ dev 分支没有新提交，无需合并"
    exit 0
fi

echo "   dev 领先 master $BEHIND 个提交"

# ============================================================
# 2. 确定版本号
# ============================================================
if [ -n "$1" ]; then
    NEW_VERSION="$1"
else
    # 获取最新 tag，自动递增 patch
    LATEST_TAG=$(git describe --tags --abbrev=0 2>/dev/null || echo "v0.0.0")
    # 解析版本号: v1.2.3 → 1.2.3
    VERSION_NUM=$(echo "$LATEST_TAG" | sed 's/^v//')
    MAJOR=$(echo "$VERSION_NUM" | cut -d. -f1)
    MINOR=$(echo "$VERSION_NUM" | cut -d. -f2)
    PATCH=$(echo "$VERSION_NUM" | cut -d. -f3)

    # 检查 commit message 中是否有版本升级指令
    MERGE_COMMITS=$(git log master..origin/dev --oneline --no-merges)
    if echo "$MERGE_COMMITS" | grep -qiE 'BREAKING CHANGE|!:' ; then
        MAJOR=$((MAJOR + 1))
        MINOR=0
        PATCH=0
        echo "   🔴 检测到 BREAKING CHANGE，主版本升级"
    elif echo "$MERGE_COMMITS" | grep -qiE '^feat'; then
        MINOR=$((MINOR + 1))
        PATCH=0
        echo "   🟡 检测到新功能(feat)，次版本升级"
    else
        PATCH=$((PATCH + 1))
        echo "   🟢 修复/优化，修订版本升级"
    fi
    NEW_VERSION="v${MAJOR}.${MINOR}.${PATCH}"
fi

echo ""
echo "📦 版本号: $LATEST_TAG → $NEW_VERSION"

# ============================================================
# 3. 确认操作
# ============================================================
echo ""
echo "============================================"
echo " 即将执行:"
echo "   1. git merge origin/dev --no-ff"
echo "   2. git tag -a $NEW_VERSION"
echo "   3. git push origin master --tags"
echo "============================================"
echo ""
read -p "确认继续? (y/N) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "已取消"
    exit 0
fi

# ============================================================
# 4. 合并 dev → master
# ============================================================
echo ""
echo "🔀 合并 dev → master..."
git merge origin/dev --no-ff -m "chore: merge dev → master, release $NEW_VERSION"

# ============================================================
# 5. 创建 Tag
# ============================================================
echo ""
echo "🏷️  创建 Tag: $NEW_VERSION"

# 生成 tag 注释：收集 dev 新提交的摘要
TAG_MSG="Release $NEW_VERSION

Changelog:
$(git log ${LATEST_TAG}..HEAD --oneline --no-merges | sed 's/^/  - /')"

git tag -a "$NEW_VERSION" -m "$TAG_MSG"

# ============================================================
# 6. 推送
# ============================================================
echo ""
echo "🚀 推送 master + tags..."
git push origin master
git push origin --tags

echo ""
echo "============================================"
echo " ✅ 发布完成"
echo "   版本: $NEW_VERSION"
echo "   分支: master (已推送)"
echo "============================================"
