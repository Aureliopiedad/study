## Delta Spec：新增 Java 8 Maven Hello World 工程

本次变更新增一个最小 Maven 工程 `java8-maven-hello-world/`，满足 `openspec/specs/java-hello-world/spec.md` 中描述的全部行为与约束。

### 新增内容

- Maven 工程目录 `java8-maven-hello-world/`
- `pom.xml`：Java 8 编译 + 可执行 jar（manifest 指定 `Main-Class=com.example.App`）
- `com.example.App`：启动打印 `Hello World`
