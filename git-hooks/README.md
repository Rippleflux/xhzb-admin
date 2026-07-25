# Git Hooks — 企业级提交规范 + 分支工作流

## 安装

```bash
cd xhzb-parent-master
bash git-hooks/install.sh
```

## 分支工作流

```
feature/smart-bed  ──→  dev  ──→  master + tag
     ↑                   ↑              ↑
   功能开发            测试校验        发布上线
```

1. **功能开发** — 从 `dev` 创建 `feature/*` 分支，开发完成后合并回 `dev`
2. **测试校验** — `dev` 环境部署测试，通过后进入发布
3. **发布上线** — 运行脚本合并 `dev` → `master`，自动打 Tag

### 发布命令

```bash
# 自动递增版本号（根据 commit type 智能判断）
bash git-hooks/merge-dev-to-master.sh

# 手动指定版本号
bash git-hooks/merge-dev-to-master.sh v3.10.0
```

版本号规则：
- 含 `BREAKING CHANGE` / `feat!:` → 主版本 +1（v1.2.3 → v2.0.0）
- 含 `feat` → 次版本 +1（v1.2.3 → v1.3.0）
- 其余 → 修订版本 +1（v1.2.3 → v1.2.4）

## Hook 清单

| Hook | 触发时机 | 校验内容 |
|------|---------|---------|
| `commit-msg` | `git commit` 提交时 | 提交信息格式：`type(scope): subject`、长度≥10字符、主题≤72字符 |
| `pre-commit` | `git commit` 暂存时 | 文件命名规范、合并冲突标记、调试代码、敏感信息、大文件 |
| `pre-push` | `git push` 推送时 | 禁止直推 master/main、分支命名规范 |
| `merge-dev-to-master.sh` | 手动执行 | dev→master 合并 + 自动打 Tag + 推送 |

## 提交格式

```
type(scope): subject

type:  feat | fix | docs | style | refactor | perf | test | chore | ci | build | revert
scope: 可选，影响模块（如 nursing, alert, device）

✅ 正确:
  feat(nursing): 新增智能床位报警WebSocket实时推送
  fix(alert): 修复报警沉默周期计算错误
  docs: 更新README部署说明

❌ 错误:
  修改了一些Bug                           ← 无type前缀
  fix                                      ← 长度不足10字符
  新增智能床位报警WebSocket实时推送功能     ← 无type前缀，非规范格式
```

## 分支命名

```
type/description

✅ 正确:
  feature/smart-bed-alert
  bugfix/alert-silent-period
  hotfix/npe-in-device-controller
  release/v3.9.1

❌ 错误:
  mywork                         ← 无type前缀
  feature                        ← 只有type无描述
  fix-bug-123                    ← 用 - 而非 /
```

## 临时绕过

```bash
git commit --no-verify -m "..."
git push --no-verify
```

## 验证

```bash
bash git-hooks/verify.sh
```

## 自定义

编辑对应 hook 文件，修改 `MIN_LENGTH`、`BRANCH_PATTERN` 等变量即可调整阈值。
