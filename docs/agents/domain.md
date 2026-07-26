# Domain Docs

本文件说明工程技能在探索代码库时如何读取本仓库的领域文档。

## 探索前先读取

- 根目录的 `CONTEXT.md`
- 如果根目录存在 `CONTEXT-MAP.md`，则按其中指引读取相关上下文的 `CONTEXT.md`
- `docs/adr/` 中与当前工作区域相关的 ADR

如果这些文件不存在，静默继续。不要因为缺失就主动建议创建；`/domain-modeling` 会在术语或决策真正明确时按需创建。

## 文件结构

本仓库采用 single-context 布局：

```text
/
├── CONTEXT.md
├── docs/adr/
└── src/
```

## 使用 glossary 中的词汇

当输出中命名领域概念时，使用 `CONTEXT.md` 中定义的术语。不要替换成 glossary 明确避免的近义词。

如果需要的概念还不在 glossary 中，说明可能是在发明项目未使用的语言，或领域文档确有缺口；必要时记录给 `/domain-modeling`。

## 标出 ADR 冲突

如果输出内容与现有 ADR 冲突，应明确指出，而不是静默覆盖。