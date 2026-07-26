# Issue tracker: GitHub

本仓库的 issues 和 PRD 都存放在 GitHub Issues 中。所有操作使用 `gh` CLI。

## 约定

- 创建 issue：`gh issue create --title "..." --body "..."`
- 读取 issue：`gh issue view <number> --comments`
- 列出 issues：`gh issue list --state open --json number,title,body,labels,comments`
- 评论 issue：`gh issue comment <number> --body "..."`
- 添加/移除标签：`gh issue edit <number> --add-label "..."` / `--remove-label "..."`
- 关闭 issue：`gh issue close <number> --comment "..."`

在仓库克隆目录中运行时，由 `gh` 根据 `git remote -v` 自动推断仓库。

## Pull requests as a triage surface

**PRs as a request surface: no.**

如果以后要把外部 PR 也纳入 triage 队列，可手动改为 `yes`。

## 技能说“发布到 issue tracker”时

创建一个 GitHub issue。

## 技能说“读取相关 ticket”时

运行 `gh issue view <number> --comments`。

## Wayfinding 操作

`/wayfinder` 使用一个带 `wayfinder:map` 标签的 GitHub issue 作为 map，并用子 issues 表示具体 tickets。

- Map：创建带 `wayfinder:map` 标签的 issue。
- Child ticket：作为 map 的子 issue；如果 GitHub 子 issue 不可用，则在 map body 中用任务列表链接，并在 child body 顶部写 `Part of #<map>`。
- Blocking：优先使用 GitHub 原生 issue dependencies；不可用时在 child body 顶部写 `Blocked by: #<n>`。
- Frontier query：列出 map 下未关闭、未阻塞、未被认领的 child tickets。
- Claim：`gh issue edit <n> --add-assignee @me`。
- Resolve：评论答案，关闭 issue，并把上下文指针追加到 map。