# Repository Guidelines

## 项目结构与模块组织

这是一个 Maven Java 项目，主包为 `ai.deep.minicodex`。入口位于 `src/main/java/ai/deep/minicodex/Main.java`。核心循环在 `agent/`，模型接口与假模型实现在 `model/`，工作区安全策略在 `safety/`，工具接口与文件读取、列目录等实现放在 `tool/`。构建产物写入 `target/`，本地 Maven 缓存位于 `.m2repo/`。当前仓库尚未包含 `src/test`；新增测试时请按 Maven 约定放到 `src/test/java`。

## 构建、测试与本地运行命令

- `mvn -Dmaven.repo.local=.m2repo compile`：使用仓库内 Maven 缓存编译 Java 22 源码。
- `mvn -Dmaven.repo.local=.m2repo exec:java -Dexec.args="列出当前项目文件"`：运行 `ai.deep.minicodex.Main` 并传入一个任务。
- `mvn -Dmaven.repo.local=.m2repo test`：运行测试；当前没有测试类时可作为编译与测试阶段检查。
- `javac -encoding UTF-8 -d target/classes ...`：在 Maven 不可用时可参考 `README.md` 手动编译。

## 编码风格与命名约定

使用 Java 22 与 UTF-8。保持现有风格：4 空格缩进，类名使用 `PascalCase`，方法、变量和包名使用 `camelCase` 或小写包名。接口保持小而直接，例如 `Tool`、`ModelClient`；实现类用职责命名，例如 `ReadFileTool`、`FakeModelClient`。新增代码应优先放入现有模块，不为一次性逻辑创建过度抽象。

## 测试指南

当前 `pom.xml` 未声明专用测试框架。新增测试前请先在 `pom.xml` 中加入 JUnit 5 等明确依赖，并将测试类命名为 `*Test`，路径与被测包结构对应，例如 `src/test/java/ai/deep/minicodex/tool/ReadFileToolTest.java`。工具、路径安全策略和 Agent 循环变更应至少覆盖成功路径与关键失败路径。

## 提交与 Pull Request 指南

当前工作区没有可读取的 Git 提交历史，因此采用简洁、祈使式提交信息，例如 `Add workspace policy tests` 或 `Fix read file validation`。PR 应说明改动目的、主要文件、验证命令和行为影响；涉及 CLI 输出或用户交互时附上示例命令与关键输出。保持 PR 聚焦，不混入格式化、重构或无关清理。

## 安全与配置提示

文件工具必须尊重工作区边界，路径校验逻辑集中在 `WorkspacePolicy`。不要提交真实 API Key、用户私有路径或生成产物；`target/` 和 IDE 临时文件应保持忽略。修改模型接入逻辑时，将真实外部调用与 `FakeModelClient` 的演示逻辑分开。
