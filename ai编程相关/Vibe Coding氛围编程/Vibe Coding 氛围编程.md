# Vibe Coding 氛围编程

## Vibe Coding 的起源和定义

最开始起源自一篇推文，源自Andrej Karpathy(OpenAI创始人之一、斯坦福大学博士、曾任特斯拉人工智能总监)，原文如下：

> There's a new kind of coding I call "vibe coding", where you fully give in to the vibes, embrace exponentials, and forget that the code even exists. It's possible because the LLMs (e.g. Cursor Composer w Sonnet) are getting too good. Also I just talk to Composer with SuperWhisper so I barely even touch the keyboard. I ask for the dumbest things like "decrease the padding on the sidebar by half" because I'm too lazy to find it. I "Accept All" always, I don't read the diffs anymore. When I get error messages I just copy paste them in with no comment, usually that fixes it. The code grows beyond my usual comprehension, I'd have to really read through it for a while. Sometimes the LLMs can't fix a bug so I just work around it or ask for random changes until it goes away. It's not too bad for throwaway weekend projects, but still quite amusing. I'm building a project or webapp, but it's not really coding - I just see stuff, say stuff, run stuff, and copy paste stuff, and it mostly works.

他描述了自己使用cursor协助编程的流程：

1. 不进行实际的代码编程
2. 只通过语言描述需求
3. 不需要理解大模型生成的代码，任由大模型生成代码，结束后直接进行测试
4. 代码报错或实际表现有误差，交给大模型随机更改，直到执行成功

在Vibe Coding下，开发者可以完全不参与代码编写或只进行有限的代码编写，通过自然语言和大模型进行交互，由大模型生成代码，并由开发者控制迭代改进。

## 与AI辅助编程的区别

一般的辅助编程大多是用AI补全代码或者是生成原子级别的方法，由于每次生成的代码行数少、功能清晰，开发者会进行review后采用或重新生成，确保这部分代码符合预期，本质上是过往的大模型能力不足导致的。

Vibe Coding强调的是由大模型完全掌握所有的代码生成，开发者不进行review或者只进行少量的代码理解，只负责一遍遍地描述需求，让大模型自由发挥直到最终的产出满足要求。

## 使用案例

以下是我使用Cursor，用Vibe Coding理念生成一个java小型项目的整体流程：

### 环境部署

- Cursor for windows
- java8
- maven

### 生成流程

Cursor可以导出问答过程，详见：[问答过程](./cursor_java_maven_project_for_reading_e.md)。

如该文档所示，没有人为的修改任意一行代码，也没有code review，需要人为处理的部分只有运行和把报错提供给大模型，并要求大模型根据出入生产出新的代码。

## 总结

Vibe Coding确实极大降低了开发门槛，在部分适用场景下大幅提升了开发速度。

根据反馈，Vibe Coding比较泛用的场景如下：

- 原型开发和想法验证
- 一次性脚本或小型工具
- 前端页面搭建和快速迭代
- 学习新技术、新语言

但是由于人工参与有限，不可避免的会存在各种奇奇怪怪的问题，包括各种代码质量问题、未知的bug、难以调试、难以维护或新增功能、复杂程度难以控制等等。

只建议Vibe Coding应用于上述场景。