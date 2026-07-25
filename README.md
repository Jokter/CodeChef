# Mini Codex Java

这是一个用于学习 Codex / Claude Code 核心工作方式的极简 Java 框架。

第一版只实现最小闭环：

1. 用户输入任务。
2. `AgentLoop` 把任务交给 `FakeModelClient`。
3. 假模型返回一个工具调用。
4. Java 程序执行工具。
5. 工具结果作为观察结果返回给假模型。
6. 假模型输出最终回答。

推荐直接用 JDK 编译运行：

```powershell
mkdir target\classes
javac -encoding UTF-8 -d target\classes @(Get-ChildItem -Recurse -Filter *.java -Path src\main\java | ForEach-Object { $_.FullName })
java -cp target\classes ai.deep.minicodex.cli.Main "看看当前项目里有什么文件"
```

如果你的 Maven 环境可用，也可以运行：

```powershell
mvn compile exec:java -Dexec.mainClass="ai.deep.minicodex.cli.Main" -Dexec.args="看看当前项目里有什么文件"
```

验证真实模型配置：

```powershell
mvn -Dmaven.repo.local=.m2repo exec:java -Dexec.mainClass="ai.deep.minicodex.cli.SimpleModelChat" -Dexec.args="你好，简单介绍一下你自己"
```

`config/model.properties` 不应提交。使用本地代理托管鉴权时，无需配置 `model.apiKey`。

后续可以逐步替换 `FakeModelClient`：

- 接入真实模型 API。
- 使用 Jackson 解析模型返回的工具调用 JSON。
- 增加 `write_file`。
- 增加 `run_command`。
- 增加用户审批。
- 增加 `AGENTS.md` 上下文读取。
- 增加 MCP 工具桥接。
