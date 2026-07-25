# Git Hooks 企业级提交规范 — 完整指南

> 项目：xhzb-parent
> 日期：2026-07-25
> 版本：1.1（增加分支工作流 + 自动 Tag）

---

## 一、分支工作流

```
feature/smart-bed ──→ dev ──→ master + tag (v3.10.0)
     ↑                  ↑              ↑
   功能开发           测试校验        发布上线
```

### 完整流程

```bash
# 1. 从 dev 创建功能分支
git checkout dev
git pull origin dev
git checkout -b feature/my-feature

# 2. 开发 + 提交（走 commit-msg 校验）
git add .
git commit -m "feat(nursing): 新增XX功能"

# 3. 合并到 dev 测试
git checkout dev
git merge feature/my-feature --no-ff
git push origin dev

# 4. dev 测试通过后 → 发布
git checkout master
bash git-hooks/merge-dev-to-master.sh

# 或手动指定版本号
bash git-hooks/merge-dev-to-master.sh v3.10.0
```

### 版本号自动规则

`merge-dev-to-master.sh` 检测 dev 新提交的 type，自动递增版本号：

| dev 新提交含 | 版本升级 | 示例 |
|-------------|---------|------|
| `BREAKING CHANGE` / `feat!:` | 主版本 | v1.2.3 → v2.0.0 |
| `feat` | 次版本 | v1.2.3 → v1.3.0 |
| 其他（fix/docs/chore...） | 修订版本 | v1.2.3 → v1.2.4 |

---

## 二、文件清单

| 文件 | 用途 |
|------|------|
| `commit-msg` | 提交信息校验（触发于 `git commit`） |
| `pre-commit` | 暂存文件检查（触发于 `git commit`） |
| `pre-push` | 分支保护 + master 推送引导（触发于 `git push`） |
| `merge-dev-to-master.sh` | 发布脚本：dev→master 合并 + 自动 Tag |
| `install.sh` | 一键安装所有 hooks 到 `.git/hooks/` |
| `verify.sh` | 自检脚本，模拟各种违规场景 |
| `README.md` | 快速参考 |

---

## 三、安装

```bash
cd xhzb-parent-master
bash git-hooks/install.sh
```

输出：

```
安装 Git Hooks → .git/hooks
  ✅ commit-msg
  ✅ pre-commit
  ✅ pre-push
安装完成。

发布流程:
  bash git-hooks/merge-dev-to-master.sh [版本号]
```

---

## 四、commit-msg 详细规则

### 4.1 校验项

| 校验 | 规则 | 示例 |
|------|------|------|
| 非空 | 提交信息不能为空 | `""` → ❌ |
| 最低长度 | ≥ 10 字符 | `"fix"`(3字符) → ❌ |
| 格式 | `type(scope): subject` | `"feat(nursing): 新增功能"` → ✅ |
| 主题行长度 | ≤ 72 字符（警告不阻断） | 73字符 → ⚠️ |
| 禁止注释行 | 不允许 `#` 开头 | `"# 测试"` → ❌ |

### 4.2 支持的 type

| type | 说明 | 示例 |
|------|------|------|
| feat | 新功能 | `feat(nursing): 新增智能床位报警推送` |
| fix | 修 Bug | `fix(alert): 修复沉默周期计算错误` |
| docs | 文档变更 | `docs: 更新部署说明` |
| style | 代码格式 | `style: 统一缩进为4空格` |
| refactor | 重构 | `refactor(device): 抽取设备注册校验逻辑` |
| perf | 性能优化 | `perf(chat): 对话记忆滑动窗口优化` |
| test | 测试 | `test: 补充报警规则单元测试` |
| chore | 构建/工具 | `chore: 升级Spring Boot到3.5.1` |
| ci | CI/CD | `ci: 添加代码扫描流水线` |
| build | 依赖变更 | `build: 添加influxdb-client依赖` |
| revert | 回退 | `revert: 回退feat(nursing)提交` |

---

## 五、pre-commit 详细规则

### 5.1 校验项

| 校验 | 规则 |
|------|------|
| 文件命名 | `.java` PascalCase、`.xml` PascalCase/kebab-case |
| 临时文件 | 禁止提交 `~` `.bak` `.tmp` `.orig` `.iml` `.class` |
| 合并冲突 | 禁止存在 `<<<<<<<` / `=======` / `>>>>>>>` 标记 |
| 调试代码 | 警告 `System.out.print` / `console.log` |
| 敏感信息 | 拦截 `password` / `secret` / `token` / `api_key`（排除 test 文件） |
| 大文件 | 警告 >1MB |

---

## 六、pre-push 详细规则

### 6.1 校验项

| 校验 | 规则 |
|------|------|
| 禁止直推 master | `refs/heads/master` 和 `refs/heads/main` 均拦截，提示使用 `merge-dev-to-master.sh` |
| 分支命名 | `type/description` 格式 |

### 6.2 支持的推送分支类型

| type | 用途 | 示例 |
|------|------|------|
| feature | 功能开发 | `feature/smart-bed-alert` |
| bugfix | Bug 修复 | `bugfix/alert-silent-period` |
| hotfix | 紧急修复 | `hotfix/npe-in-device-controller` |
| release | 发布分支 | `release/v3.9.1` |

---

## 七、临时绕过

```bash
git commit --no-verify -m "临时提交，后续补充"
git push --no-verify
```

---

## 八、自定义

编辑对应 hook 文件，修改变量即可调整阈值：

| 文件 | 变量 | 默认值 |
|------|------|--------|
| commit-msg | `MIN_LENGTH` | 10 |
| commit-msg | `MAX_SUBJECT_LEN` | 72 |
| pre-push | `BRANCH_PATTERN` | `^(feature\|bugfix\|hotfix\|release)/(.+)$` |
