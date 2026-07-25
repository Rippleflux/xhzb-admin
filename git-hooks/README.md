# Git Hooks — 企业级提交规范

## 安装

```bash
cd xhzb-parent-master
bash git-hooks/install.sh
```

## Hook 清单

| Hook | 触发时机 | 校验内容 |
|------|---------|---------|
| `commit-msg` | `git commit` 提交时 | 提交信息格式：`type(scope): subject`、长度≥10字符、主题≤72字符 |
| `pre-commit` | `git commit` 暂存时 | 文件命名规范、合并冲突标记、调试代码、敏感信息、大文件 |
| `pre-push` | `git push` 推送时 | 禁止直推 master/main、分支命名规范 |

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
