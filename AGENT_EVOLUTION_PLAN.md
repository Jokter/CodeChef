# Mini Codex 演进计划

## 目标定位

这个项目不是为了复刻完整的 Codex 或 Claude Code，而是作为一个学习型 mini agent：

- 通过真实、可运行的小步演进，理解 coding agent 的核心机制。
- 每一步只引入一个关键概念，避免过早工程化。
- 完成核心骨架后，把主要精力转向阅读 Codex / Claude Code 源码。
- 后续遇到源码里的关键机制，再回到本项目做小实验验证理解。

最终希望形成的能力闭环：

```text
用户任务
 -> 上下文构建
 -> 模型请求
 -> 模型输出解析
 -> 工具调用
 -> 安全与审批
 -> 工具结果回灌
 -> 会话记录
 -> 最终回答
```

## 当前项目已有基础

当前项目已经具备以下核心雏形：

- `GptModelClient`：真实模型请求客户端，支持 OpenAI / Anthropic 两类请求格式。
- `ModelClient`：模型客户端抽象。
- `ModelResponse`：表达最终回答或工具调用。
- `ToolCall`：表达模型请求的工具名与参数。
- `AgentLoop`：执行“模型决策 -> 工具调用 -> observation 回灌”的主循环。
- `ToolRegistry`：注册并分发工具调用。
- `ReadFileTool` / `ListFilesTool`：基础文件工具。
- `WorkspacePolicy`：工作区路径安全边界。

因此后续演进不需要从“能不能调工具”开始，而应该从“让这些能力逐步模块化、结构化、可观察”开始。

## 演进原则

1. 每个阶段只解决一个主要问题。
2. 优先保留当前代码风格，避免大重构。
3. 每一步都要能运行、能验证、能解释。
4. 不追求功能完整，追求概念清楚。
5. 当实现开始明显变复杂时，停下来转向源码阅读。

## 阶段 0：工程阅读体验整理（短前置门槛）

### 目标

先保证这个项目适合作为长期阅读和实验材料。

### 实现范围

- 引入 JUnit 5，并建立最小测试目录 `src/test/java`。
- 只新增 `WorkspacePolicyTest` 和 `ToolRegistryTest` 两个最小单元测试类。
- 在 `README.md` 中补一段当前真实代码状态下的 agent 链路说明。

### 验收标准

- `mvn compile` 通过。
- `mvn test` 通过，且只覆盖 `WorkspacePolicy` 与 `ToolRegistry` 的核心行为。
- 新读者能通过 README 快速理解当前已实现的主链路。

### 对应源码阅读点

- Codex / Claude Code 中的项目级说明文件加载。
- AGENTS.md / CLAUDE.md 这类“给 agent 看的仓库说明”。

## 阶段 1：ToolSchema

### 目标

让工具自己描述能力，而不是把工具说明硬编码在 `GptModelClient` 的 system prompt 里。

### 实现范围

- 新增 `ToolSchema` 数据结构。
- `Tool` 直接增加 `schema()` 方法。
- `ReadFileTool` 提供 `path` 参数说明。
- `ListFilesTool` 提供 `path` 参数说明。
- `ToolRegistry` 提供 `schemas()`，只汇总结构化工具说明。
- 阶段 1 不实现 OpenAI / Anthropic 的真实 API tool schema，只生成面向 prompt 的工具说明文本。
- 阶段 1 暂不修改 `ModelClient.next` 接口，接口重塑留到 `ContextBuilder` 阶段。
- `GptModelClient` 只依赖 `List<ToolSchema>`，不依赖 `ToolRegistry`。
- `GptModelClient` 构造器改为接收 `GptModelConfig` 和 `List<ToolSchema>`，不保留无工具说明的旧构造器。

### 建议最小数据结构

文件位置：`src/main/java/ai/deep/minicodex/tool/api/ToolSchema.java`

```java
public record ToolSchema(
        String name,
        String description,
        Map<String, String> parameters
) {
}
```

参数约束写在自然语言说明中，不新增 `required`、`type`、`defaultValue` 等结构化字段。

### 验收标准

- 工具说明由工具实现类提供。
- 新增工具时不需要修改 `GptModelClient` 的 prompt。
- `FakeModelClient` 和 `GptModelClient` 仍能正常运行。
- 阶段 1 不单独测试临时 prompt 渲染逻辑，等 `ContextBuilder` 阶段再补上下文构建单测。

### 对应源码阅读点

- Codex / Claude Code 的 tool definition。
- OpenAI / Anthropic API 中传给模型的工具 schema。

## 阶段 2：ContextBuilder

### 目标

把“给模型看的上下文”从模型客户端中拆出来。

### 实现范围

- 新增 `ContextBuilder`。
- 它负责生成：
  - system prompt；
  - 工具列表；
  - 用户任务；
  - 历史 observation；
  - 输出协议说明。
- `GptModelClient` 不再自己拼接完整 prompt，只接收构建好的上下文或调用 `ContextBuilder`。

### 建议边界

```text
ContextBuilder 负责“写给模型看什么”
GptModelClient 负责“怎么发给模型”
AgentLoop 负责“什么时候问模型”
```

### 验收标准

- `SYSTEM_PROMPT` 不再承担工具列表硬编码。
- 工具列表来自 `ToolRegistry`。
- 上下文构建逻辑可以单独测试。

### 对应源码阅读点

- Codex / Claude Code 中的 prompt assembly。
- repo instructions、tool descriptions、history 的合并逻辑。

## 阶段 3：ModelOutputParser

### 目标

把模型输出协议解析从 `GptModelClient` 中拆出，形成独立的协议层。

### 实现范围

- 新增 `ModelOutputParser`。
- 移动以下逻辑：
  - 提取 JSON 对象；
  - 判断 `type`；
  - 构造 `ModelResponse`；
  - 解析 `arguments`。
- 对非法 JSON、未知 type、缺失字段补测试。

### 验收标准

- `GptModelClient` 不直接解析业务协议。
- parser 单测覆盖：
  - final；
  - tool_call；
  - markdown 包裹 JSON；
  - 非法输出。

### 对应源码阅读点

- Codex / Claude Code 中模型响应 item 的转换层。
- API 原始响应与 agent 内部事件之间的边界。

## 阶段 4：ToolObservation

### 目标

把当前 `List<String> observations` 升级为结构化观察结果。

### 实现范围

- 新增 `ToolObservation`。
- 字段可以包含：
  - 工具名；
  - 参数；
  - 是否成功；
  - 结果内容；
  - 是否截断；
  - 错误信息。
- `AgentLoop` 中不再手写 observation 大字符串。
- `ContextBuilder` 负责把 `ToolObservation` 渲染成模型可读文本。

### 建议最小结构

```java
public record ToolObservation(
        String toolName,
        Map<String, String> arguments,
        boolean success,
        String content
) {
}
```

### 验收标准

- 工具结果在代码里是结构化对象。
- prompt 渲染仍然保持简单中文文本。
- 后续做日志、压缩、错误处理时不需要解析字符串。

### 对应源码阅读点

- Codex 的 turn item / tool result。
- Claude Code 中工具执行结果如何进入下一轮上下文。

## 阶段 5：WriteFileTool

### 目标

让 agent 从“只读代码”进入“能修改代码”的边界。

### 实现范围

- 新增 `write_file` 工具。
- 参数：
  - `path`：目标文件路径；
  - `content`：完整文件内容。
- 写入前必须通过 `WorkspacePolicy`。
- 暂时不做复杂 diff，只做整文件写入。
- 初期可以只允许写入不存在的新文件，降低风险。

### 验收标准

- 可以在工作区内创建一个小文件。
- 不能写到工作区外。
- 路径越界有测试。
- 写入失败返回 `ToolResult.error`，不打断 agent loop。

### 对应源码阅读点

- Codex / Claude Code 的文件编辑工具。
- 写入工具为什么必须配合 sandbox 和 approval。

## 阶段 6：ApprovalService

### 目标

理解“权限不是 prompt，而是运行时机制”。

### 实现范围

- 新增 `ApprovalService`。
- 新增审批结果：
  - allow；
  - deny；
  - ask。
- 先实现简单模式：
  - 读工具默认 allow；
  - 写工具默认 ask 或 deny；
  - 命令工具默认 ask。
- CLI 中先用控制台输入 `y/n` 完成 ask。

### 建议结构

```text
AgentLoop
 -> 收到 ToolCall
 -> ApprovalService 判断
 -> allow: 执行工具
 -> deny: 返回拒绝 observation
 -> ask: 询问用户后再执行或拒绝
```

### 验收标准

- `write_file` 不会绕过审批直接执行。
- 用户拒绝后，模型能在下一轮看到“工具调用被拒绝”。
- 审批逻辑不写在具体工具类里。

### 对应源码阅读点

- Claude Code permissions。
- Codex approval flow。
- 工具风险分级与用户确认。

## 阶段 7：RunCommandTool

### 目标

让 agent 能运行最基础的开发命令，理解 coding agent 的执行环境边界。

### 实现范围

- 新增 `run_command` 工具。
- 参数：
  - `command`；
  - 可选 `timeoutSeconds`。
- 先只允许白名单命令：
  - `mvn test`；
  - `mvn compile`；
  - `git diff`；
  - `git status`；
  - `rg`。
- 捕获：
  - exit code；
  - stdout；
  - stderr；
  - timeout。
- 输出长度做截断。

### 验收标准

- 能通过 agent 运行 `mvn test`。
- 危险命令被拒绝。
- 超时命令被终止。
- 输出过长时标记已截断。

### 对应源码阅读点

- Codex shell tool。
- Claude Code Bash tool。
- 命令执行、超时、输出截断、安全策略。

## 阶段 8：SessionLog

### 目标

把 agent 的执行过程保存下来，理解 thread / turn / item。

### 实现范围

- 新增 `Session` 概念。
- 新增 `Turn` 概念。
- 每一步写入 JSONL：
  - 用户输入；
  - 模型响应；
  - 工具调用；
  - 工具结果；
  - 最终回答。
- 日志可以先放到 `.minicodex/sessions`。

### 验收标准

- 每次运行都有 session id。
- 能打开 JSONL 看到完整执行链路。
- 程序异常时也尽量保留已完成事件。

### 对应源码阅读点

- Codex thread / turn / item。
- Claude Code conversation transcript。
- 为什么真实 agent 强依赖事件日志。

## 阶段 9：SlashCommand

### 目标

让 CLI 具备少量内部控制命令，理解交互式 agent 的控制面。

### 实现范围

先只做以下命令：

- `/status`：显示当前模型、工作区、工具数量、最大步数。
- `/tools`：显示工具 schema。
- `/permissions`：显示当前审批策略。
- `/exit`：退出。

### 验收标准

- 普通输入仍作为用户任务交给 agent。
- 以 `/` 开头的命令由 CLI 自己处理。
- slash command 不进入模型上下文。

### 对应源码阅读点

- Claude Code slash commands。
- Codex CLI / app 中用户控制命令和模型任务的分离。

## 阶段 10：SimpleCompact

### 目标

理解上下文窗口管理，不追求复杂摘要质量。

### 实现范围

- 当 observations 数量超过阈值时，触发简单压缩。
- 初版可以用规则压缩：
  - 保留用户原始任务；
  - 保留最近 2 条 observation；
  - 旧 observation 合并成简短摘要。
- 后续可选：让模型生成摘要。

### 验收标准

- 长任务不会无限增长 observation 文本。
- 压缩后模型仍能知道：
  - 用户目标；
  - 已调用过哪些工具；
  - 关键结果；
  - 最近上下文。

### 对应源码阅读点

- Codex compact。
- Claude Code compact。
- 长上下文 agent 如何避免“历史包袱”。

## 完成后的停止点

完成阶段 0 到阶段 10 后，本项目就不建议继续大规模演进。

此时它已经覆盖 coding agent 的核心骨架：

```text
工具描述
上下文构建
模型协议解析
工具执行
工作区安全
用户审批
命令执行
会话日志
控制命令
上下文压缩
```

之后更适合直接阅读 Codex / Claude Code 源码。

## 不建议在本项目完整实现的内容

以下内容可以通过源码阅读理解，不建议在本项目里完整复刻：

- 完整 MCP 协议。
- 插件市场。
- 复杂 hook DSL。
- 多 agent 调度系统。
- IDE 集成。
- 云端任务、fork、resume 全套实现。
- 复杂 diff / patch 引擎。
- 多模型 provider 全量适配。
- 大规模缓存和 telemetry。

如果阅读源码时遇到这些机制，只在本项目中做最小 spike：

```text
看懂一个机制
 -> 回 mini 项目写 20 到 80 行验证版本
 -> 记录真实实现为什么更复杂
 -> 继续读源码
```

## 推荐执行顺序

1. 阶段 0：工程阅读体验整理（短前置门槛）。
2. 阶段 1：ToolSchema。
3. 阶段 2：ContextBuilder。
4. 阶段 3：ModelOutputParser。
5. 阶段 4：ToolObservation。
6. 阶段 5：WriteFileTool。
7. 阶段 6：ApprovalService。
8. 阶段 7：RunCommandTool。
9. 阶段 8：SessionLog。
10. 阶段 9：SlashCommand。
11. 阶段 10：SimpleCompact。

## 下一步建议

下一次真实编码建议先完成阶段 0 的短前置门槛，再从阶段 1 开始：

```text
目标：让 GptModelClient 不再硬编码工具说明。

具体改动：
1. 新增 ToolSchema。
2. Tool 暴露 schema。
3. ReadFileTool / ListFilesTool 实现 schema。
4. ToolRegistry 汇总工具 schema。
5. GptModelClient 的 prompt 从 ToolRegistry 获取工具说明。

验证：
1. mvn compile
2. 用 FakeModelClient 跑现有演示
3. 用真实 GptModelClient 跑一次 list/read 任务
```

这一步完成后，再读 Codex / Claude Code 的 tool schema 相关源码，会更容易建立映射。
