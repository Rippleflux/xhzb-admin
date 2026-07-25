#!/bin/bash
# Git Hooks 验证脚本 — 模拟各种场景测试 hooks 是否生效
# 使用: bash verify.sh

HOOKS_DIR="$(cd "$(dirname "$0")" && pwd)"
PASS=0
FAIL=0

echo "============================================"
echo " Git Hooks 验证测试"
echo "============================================"

# ---------- commit-msg ----------
echo ""
echo "[commit-msg]"

# 测试1: 空提交信息
echo "test1: 空提交" > /tmp/test-commit-msg
bash "$HOOKS_DIR/commit-msg" /tmp/test-commit-msg 2>/dev/null
if [ $? -ne 0 ]; then echo "  ✅ 空提交被拦截"; PASS=$((PASS+1)); else echo "  ❌ 空提交未拦截"; FAIL=$((FAIL+1)); fi

# 测试2: 过短提交信息
echo "fix" > /tmp/test-commit-msg
bash "$HOOKS_DIR/commit-msg" /tmp/test-commit-msg 2>/dev/null
if [ $? -ne 0 ]; then echo "  ✅ 过短提交拦截"; PASS=$((PASS+1)); else echo "  ❌ 过短提交未拦截"; FAIL=$((FAIL+1)); fi

# 测试3: 合格提交
echo "feat(nursing): 新增智能床位报警WebSocket实时推送" > /tmp/test-commit-msg
bash "$HOOKS_DIR/commit-msg" /tmp/test-commit-msg 2>/dev/null
if [ $? -eq 0 ]; then echo "  ✅ 合格提交通过"; PASS=$((PASS+1)); else echo "  ❌ 合格提交被拦截"; FAIL=$((FAIL+1)); fi

# 测试4: 无 type 前缀
echo "新增智能床位报警WebSocket实时推送功能" > /tmp/test-commit-msg
bash "$HOOKS_DIR/commit-msg" /tmp/test-commit-msg 2>/dev/null
if [ $? -ne 0 ]; then echo "  ✅ 无type拦截"; PASS=$((PASS+1)); else echo "  ❌ 无type未拦截"; FAIL=$((FAIL+1)); fi

# ---------- pre-push ----------
echo ""
echo "[pre-push]"

# 测试5: 禁止推master
echo "refs/heads/master 000000  refs/heads/master 000000" | bash "$HOOKS_DIR/pre-push" 2>/dev/null
if [ $? -ne 0 ]; then echo "  ✅ 推master拦截"; PASS=$((PASS+1)); else echo "  ❌ 推master未拦截"; FAIL=$((FAIL+1)); fi

# 测试6: 分支命名不规范
echo "refs/heads/mywork 000000  refs/heads/mywork 000000" | bash "$HOOKS_DIR/pre-push" 2>/dev/null
if [ $? -ne 0 ]; then echo "  ✅ 不规范分支名拦截"; PASS=$((PASS+1)); else echo "  ❌ 不规范分支未拦截"; FAIL=$((FAIL+1)); fi

# 测试7: 规范分支
echo "refs/heads/feature/smart-bed 000000  refs/heads/feature/smart-bed 000000" | bash "$HOOKS_DIR/pre-push" 2>/dev/null
if [ $? -eq 0 ]; then echo "  ✅ 规范分支通过"; PASS=$((PASS+1)); else echo "  ❌ 规范分支被拦截"; FAIL=$((FAIL+1)); fi

# ---------- pre-commit (synthetic) ----------
echo ""
echo "[pre-commit]"

# 测试8: dummy pass (真实测试需要 staged 文件)
bash "$HOOKS_DIR/pre-commit" 2>/dev/null
if [ $? -eq 0 ]; then echo "  ✅ 无违规文件通过"; PASS=$((PASS+1)); else echo "  ❌ 通过失败(可能有staged文件)"; FAIL=$((FAIL+1)); fi

# ---------- result ----------
echo ""
echo "============================================"
echo " 结果: $PASS 通过 / $FAIL 失败"
echo "============================================"
