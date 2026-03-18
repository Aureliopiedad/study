## 能力：Java 8 Maven Hello World

### 行为规格

- 系统应提供一个可构建的 Maven 工程，使用 Java 8（source/target 1.8）编译。
- 构建命令 `mvn -q -f java8-maven-hello-world/pom.xml package` 应成功并生成可执行 jar。
- 执行 `java -jar java8-maven-hello-world/target/java8-maven-hello-world-0.1.0-SNAPSHOT.jar` 时：
  - 标准输出应打印一行 `Hello World`
  - 进程退出码为 0

### 约束

- 不依赖任何第三方库（除 Maven 插件本身）。
- 入口类为 `com.example.App`，并通过 jar manifest 的 `Main-Class` 指定为主类。
