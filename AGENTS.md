# Repository Guidelines

## 项目结构与模块组织

本仓库是一个最小化 Java Agent 示例项目，使用 Maven 管理构建。主要代码位于 `src/main/java/ai/deep/minicodex`：

- `agent/`：Agent 主循环与执行流程。
- `cli/`：命令行入口，例如 `Main` 与 `SimpleModelChat`。
- `model/api/`：模型接口、响应与工具调用数据结构。
- `model/client/`：模型客户端实现，例如 `FakeModelClient` 与 `RealModelClient`。
- `model/config/`：真实模型配置读取。
- `tool/api/`：工具接口与工具执行结果。
- `tool/registry/`：工具注册表与分发逻辑。
- `tool/file/`：文件读取、目录列表等文件工具实现。
- `safety/`：工作区路径安全策略。
- `config/model.properties.example`：真实模型配置模板。

构建产物在 `target/`，本地 Maven 仓库可放在 `.m2repo/`。不要提交 `target/`、`.m2repo/`、IDE 配置或 `config/model.properties`。

## 构建、测试与本地运行命令

- `mvn compile`：编译 Java 22 源码。
- `mvn exec:java -Dexec.mainClass="ai.deep.minicodex.cli.Main" -Dexec.args="看看当前项目里有什么文件"`：运行默认 Agent 演示。
- `mvn -Dmaven.repo.local=.m2repo exec:java -Dexec.mainClass="ai.deep.minicodex.cli.SimpleModelChat" -Dexec.args="你好"`：使用本地 Maven 仓库运行模型连通性验证。
- `mvn test`：运行测试；当前仓库尚未提供 `src/test`，新增测试后应使用此命令验证。

## 编码风格与命名约定

使用 Java 22，源码编码为 UTF-8。保持现有风格：4 空格缩进，类名使用 `PascalCase`，方法、变量和包名使用 `camelCase` 或小写包路径。公共类型应职责单一，新增工具优先实现 `Tool` 并注册到 `ToolRegistry`。除非修改区域确有需要，不要顺手重排无关导入、注释或格式。

## 测试指南

新测试建议放在 `src/test/java`，包路径与被测类保持一致。测试命名使用 `ClassNameTest`，测试方法描述具体行为，例如 `rejectsPathOutsideWorkspace`。涉及安全策略、工具执行、模型响应解析的改动应优先补测试；CLI 交互可用小范围集成测试或手动命令验证。

## 提交与 Pull Request 指南

历史提交使用简短英文祈使句，例如 `Improve model chat verification`。继续沿用这种风格：一句话说明行为变化，避免笼统的 `update`。PR 应包含变更摘要、验证命令与结果、关联 issue；涉及 CLI 输出或配置流程时，附上示例命令或截图。不要在 PR 中包含本地密钥、真实 `model.properties` 或生成产物。

## 安全与配置提示

真实模型密钥只放在本地配置或代理托管环境中。提交前检查 `.gitignore` 覆盖敏感文件，并确认路径访问仍受 `WorkspacePolicy` 限制。
