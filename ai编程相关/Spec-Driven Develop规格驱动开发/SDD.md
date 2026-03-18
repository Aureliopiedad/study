# SDD 和 Open Spec

## SDD介绍

规格驱动开发(Spec-Driven Development, SDD)，某种程度上是为了解决Vibe Coding的问题而生的。

SDD诞生的原因很简单：

1. 随着大模型的发展，上下文窗口变得足够大，模型也能很好的理解文档描述的内容
2. 规范文档可以在多个模型上复用

## Open Spec 实际使用

[Open Spec](https://github.com/Fission-AI/OpenSpec)是一个开源的适用于SDD的轻量级框架。

### init

在目标目录下执行`openspec init`命令，会根据选择的tool自动生成一部分skills，在本例中，Open Spec会为Cursor生成数个文件夹，包括:

```
.cursor/
├── commands/
|   ├── opsx-apply.md # 第二个命令，用于按照propose命令中产出的文档实施任务。
|   ├── opsx-archive.md # 最终命令，归档
|   ├── opsx-explore.md # 调用链之外的命令，用于修改propose中产出的文档，该命令不会产出实际代码。
|   └── opsx-propose.md # 第一个命令，用于创建一个新的隔离空间，顺序生成文档，直到准备实现。
├── skills/
|   ├── openspec-apply-change
|   ├── openspec-archive-change
|   ├── openspec-explore
|   └── openspec-propose
openspec/
├── specs/              # Source of truth (your system's behavior) # 指的是当前项目的真实代码内容
│   └── <domain>/
│       └── spec.md
├── changes/            # Proposed updates (one folder per change) # 正在进行的修改
│   └── <change-name>/
│       ├── proposal.md
│       ├── design.md
│       ├── tasks.md
│       └── specs/      # Delta specs (what's changing)
│           └── <domain>/
│               └── spec.md
└── config.yaml         # Project configuration (optional)
```

详细流程可以查看[问答过程](./cursor_cursor_directory_file_summaries.md)。