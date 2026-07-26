# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Agent skills

### Issue tracker

本仓库使用 GitHub Issues 跟踪任务。详见 `docs/agents/issue-tracker.md`。

### Triage labels

本仓库使用默认的五个 triage 标签。详见 `docs/agents/triage-labels.md`。

### Domain docs

本仓库使用 single-context 领域文档布局。详见 `docs/agents/domain.md`。

## 项目概览

这是一个用于学习 Codex / Claude Code 核心工作方式的极简 Java Agent 框架。当前主流程是：CLI 接收用户任务，`AgentLoop` 调用模型客户端，模型返回工具调用或最终回答，Java 侧执行工具并把观察结果交回模型。

项目使用 Maven，但 README 也保留了直接用 JDK 编译运行的方式。源码要求 Java 22（见 `pom.xml` 的 `maven.compiler.source/target`）。

## 常用命令

```bash
mvn compile
```

```bash
mvn exec:java -Dexec.mainClass="ai.deep.minicodex.cli.Main" -Dexec.args="看看当前项目里有什么文件"
```

使用仓库内本地 Maven 仓库运行，适合复用已缓存依赖：

```bash
mvn -Dmaven.repo.local=.m2repo compile
```

验证真实模型配置和一次 OpenAI Chat Completions 兼容请求：

```bash
mvn -Dmaven.repo.local=.m2repo exec:java -Dexec.mainClass="ai.deep.minicodex.cli.SimpleModelChat" -Dexec.args="你好，简单介绍一下你自己"
```

不经过 Maven，直接用 JDK 编译运行（Windows PowerShell）：

```powershell
mkdir target\classes
javac -encoding UTF-8 -d target\classes @(Get-ChildItem -Recurse -Filter *.java -Path src\main\java | ForEach-Object { $_.FullName })
java -cp target\classes ai.deep.minicodex.cli.Main "看看当前项目里有什么文件"
```

当前仓库没有 `src/test/java` 测试目录，也没有配置 Surefire/JUnit；如果新增测试，先补充测试依赖和对应命令。Maven 默认单测命令会是：

```bash
mvn test
```

单测命令通常会是：

```bash
mvn -Dtest=ClassNameTest test
```

## 代码结构

- `ai.deep.minicodex.cli`：命令行入口。
  - `Main` 装配最小 Agent 运行环境：工作区路径策略、工具注册表、假模型客户端和 `AgentLoop`。
  - `SimpleModelChat` 用 `config/model.properties` 发起一次真实模型聊天请求；请求格式按 OpenAI Chat Completions 兼容接口构造，同时兼容解析 Anthropic 风格的 `/content/0/text` 响应。
- `ai.deep.minicodex.agent`：Agent 主循环。
  - `AgentLoop` 最多执行 5 步；每步让 `ModelClient` 基于用户任务和历史 observations 决定下一步；工具结果被格式化为文本 observation 后加入历史。
- `ai.deep.minicodex.model.api`：模型抽象与数据结构。
  - `ModelClient` 是模型接口，便于把假模型客户端替换成真实模型客户端。
  - `ModelResponse` 用 `finalAnswer != null` 区分最终回答和工具调用。
  - `ToolCall` 当前只支持 `Map<String, String>` 参数。
- `ai.deep.minicodex.model.client`：模型客户端实现。
  - `FakeModelClient` 用规则模拟模型行为。
  - `RealModelClient` 调用 OpenAI Chat Completions 兼容接口，并解析模型返回的工具调用 JSON。
- `ai.deep.minicodex.model.config`：模型配置。
  - `ModelConfig` 从 `config/model.properties` 读取 `model.name`、`model.url` 和可选 `model.apiKey`。
- `ai.deep.minicodex.tool.api`：工具接口与工具执行结果。
  - `Tool` 是所有工具实现的接口。
  - `ToolResult` 表示工具是否成功以及返回内容。
- `ai.deep.minicodex.tool.registry`：工具注册表。
  - `ToolRegistry` 用工具名分发调用；未知工具和执行异常都会转换为失败的 `ToolResult`，避免中断主循环。
- `ai.deep.minicodex.tool.file`：文件工具实现。
  - `ListFilesTool` 只列出目录直接子项，不递归。
  - `ReadFileTool` 按 UTF-8 读取工作区内普通文件。
- `ai.deep.minicodex.safety`：文件系统边界控制。
  - `WorkspacePolicy` 是文件工具访问路径前必须经过的边界检查，阻止 `..` 等路径逃逸工作区。

## 配置与本地文件

`config/model.properties.example` 是真实模型配置模板。复制为 `config/model.properties` 后填写本地配置；该文件已在 `.gitignore` 中，不应提交。使用本地代理托管鉴权时，`model.apiKey` 可以留空或省略。

`.m2repo/` 是仓库内本地 Maven 依赖缓存，也已忽略；需要离线或固定本地缓存时使用 `-Dmaven.repo.local=.m2repo`。

## 扩展时的项目约定

- 新增工具时实现 `Tool`，通过 `ToolRegistry.register(...)` 注册，并让所有文件系统访问先经过 `WorkspacePolicy`。
- 新接真实模型时优先新增 `ModelClient` 实现，不要把网络请求逻辑塞进 `AgentLoop`；`AgentLoop` 应保持只负责循环、分发工具和记录 observation。
- 当前工具调用参数模型是 `Map<String, String>`；如果要支持结构化 JSON 参数，需要同步调整 `ToolCall`、模型响应解析和工具参数读取方式。
- 代码和注释目前以中文为主，JavaDoc 较完整；新增代码应沿用这种风格，但不要为显而易见的实现添加冗余说明。
