# Repository Guidelines

## 项目结构与模块组织

本仓库是一个 Maven Java 项目，主包为 `ai.deep.minicodex`。命令行入口放在 `cli/`，核心 Agent 循环放在 `agent/`，模型接口、配置与实现放在 `model/`，工作区路径安全策略放在 `safety/`，工具接口与文件工具实现放在 `tool/`。默认入口是 `src/main/java/ai/deep/minicodex/cli/Main.java`。构建输出位于 `target/`，本地 Maven 缓存位于 `.m2repo/`；新增测试按 Maven 约定放入 `src/test/java`。

## 构建、测试与本地运行命令

- `mvn -Dmaven.repo.local=.m2repo compile`：使用仓库内 Maven 缓存编译 Java 22 源码。
- `mvn -Dmaven.repo.local=.m2repo exec:java -Dexec.args="列出当前项目文件"`：运行默认入口 `ai.deep.minicodex.cli.Main`。
- `mvn -Dmaven.repo.local=.m2repo test`：运行测试阶段；目前没有测试类时可作为基础编译检查。
- `javac -encoding UTF-8 -d target/classes ...`：Maven 不可用时可参考 `README.md` 手动编译。

## 编码风格与命名约定

项目使用 Java 22 和 UTF-8。保持现有 Java 风格：4 空格缩进，类名使用 `PascalCase`，方法和变量使用 `camelCase`，包名保持小写。接口应小而明确，例如 `Tool`、`ModelClient`；实现类按职责命名，例如 `ReadFileTool`、`FakeModelClient`。新增逻辑优先放入现有模块，避免为一次性需求创建额外抽象。

## 测试指南

当前 `pom.xml` 未声明专用测试框架。添加测试前请先引入明确依赖，例如 JUnit 5。测试类命名使用 `*Test`，并镜像源码包结构，例如 `src/test/java/ai/deep/minicodex/tool/ReadFileToolTest.java`。修改工具、路径校验或 Agent 循环时，应覆盖成功路径和关键失败路径。

## 提交与 Pull Request 指南

当前 Git 历史只有 `Initial commit`，尚未形成细化约定。后续提交建议使用简洁祈使句，例如 `Add read file tests` 或 `Fix workspace path validation`。PR 应说明改动目的、主要影响文件、验证命令和用户可见行为变化；涉及 CLI 交互时附示例命令或关键输出。保持 PR 聚焦，不混入无关重构或格式化。

## 安全与配置提示

文件访问必须遵守工作区边界，相关逻辑集中在 `WorkspacePolicy`。不要提交真实 API Key、个人路径或生成产物。修改真实模型接入时，将外部 API 调用与 `FakeModelClient` 的演示逻辑分开，便于本地测试和审查。
