## Tasks

- [x] 在仓库根目录创建 Maven 工程目录 `java8-maven-hello-world/`
- [x] 添加 `java8-maven-hello-world/pom.xml`
  - [x] 设置 `groupId=com.example2`、`artifactId=java8-maven-hello-world`、`version=0.1.0-SNAPSHOT`
  - [x] 配置 Java 8 编译（`maven-compiler-plugin`：source/target=1.8）
  - [x] 配置可执行 jar（`maven-jar-plugin` manifest：`Main-Class=com.example.App`）
- [x] 添加入口类 `java8-maven-hello-world/src/main/java/com/example/App.java`
  - [x] `public static void main(String[] args)` 打印 `Hello World`
- [x] 添加 `java8-maven-hello-world/README.md`
  - [x] 给出构建命令与运行命令
- [x] 本地验证（验收）
  - [x] `mvn -q -f java8-maven-hello-world/pom.xml package`
  - [x] `java -jar java8-maven-hello-world/target/java8-maven-hello-world-0.1.0-SNAPSHOT.jar` 输出 `Hello World`
