# Mini Codex 学习上下文

本上下文记录这个学习型 mini agent 项目中反复使用的领域语言，帮助后续计划、实现和源码阅读保持同一套说法。

## Language

**短前置门槛**:
在进入某个主要演进阶段前必须完成的少量验证和整理工作。它只移除会妨碍下一步学习的直接障碍，不承担完整清理、重构或扩展职责。
_Avoid_: 完整阶段 0、工程大清理、准备期

**面向 prompt 的最小说明结构**:
用于把工具名称、能力描述和参数说明渲染进模型提示词的轻量结构。它不是 OpenAI 或 Anthropic 的真实 API tool schema，也不承担 provider 适配职责。
_Avoid_: 真实 API schema、JSON Schema、provider tool definition

**工具说明汇总**:
由工具注册表收集所有已注册工具的 ToolSchema，供模型上下文构建使用。它只返回结构化说明，不决定最终 prompt 文本格式。
_Avoid_: prompt 渲染、工具文本拼接、provider schema 生成

**模型客户端工具说明依赖**:
模型客户端接收已汇总的工具说明列表，用来构建发给模型的提示词。它不依赖工具注册表，也不参与工具执行分发。
_Avoid_: 模型客户端依赖 ToolRegistry、模型客户端执行工具

**模型输出协议**:
模型必须返回给 agent 的最小 JSON 对象格式。当前只允许 `final` 和 `tool_call` 两类响应；非法 JSON、未知类型或缺失字段都属于协议错误。
_Avoid_: 普通自然语言回答、Markdown 响应格式、provider 原始响应

**模型输出解析器**:
把模型输出协议文本转换为 `ModelResponse` 的独立协议层。它不负责发送模型请求，也不解析 OpenAI 或 Anthropic 的原始响应外壳。
_Avoid_: 模型客户端解析业务协议、HTTP 响应解析器、provider 适配器
