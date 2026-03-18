## 变更概述

创建一个最小可运行的 **Java 8 + Maven** 示例项目，程序启动后在标准输出打印 `Hello World`。

## 背景 / 动机

在本仓库里需要一个“最小可工作的 Java 项目”作为后续规格驱动开发（SDD）流程的演示载体：从 spec → tasks → 实现 → 归档。

## 目标

- 生成一个标准 Maven 工程结构（`src/main/java`）。
- 使用 Java 8 编译（source/target 1.8）。
- 产出可执行的 jar，并能运行输出 `Hello World`。
- 提供最小的构建与运行说明。

## 非目标

- 不引入 Spring / Jakarta EE 等框架
- 不做复杂参数解析、日志系统、配置系统
- 不要求单元测试（可作为后续 change）
- 不发布到 Maven Central

## 交付物

- 一个新目录：`java8-maven-hello-world/`（包含 `pom.xml` 与 `src/main/java/...`）
- `README.md`：构建与运行命令

## 验收标准

- 执行 `mvn -q -f java8-maven-hello-world/pom.xml package` 成功
- 执行 `java -jar java8-maven-hello-world/target/*.jar` 输出一行 `Hello World` 并以 0 退出
