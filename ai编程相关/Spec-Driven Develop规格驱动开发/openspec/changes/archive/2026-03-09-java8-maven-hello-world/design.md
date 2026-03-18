## 总体设计

这是一个最小 Maven 单模块工程，通过 `main` 方法打印 `Hello World`。

## 目录结构

在仓库根目录新增：

```
java8-maven-hello-world/
├── pom.xml
├── README.md
└── src/
    └── main/
        └── java/
            └── com/
                └── example/
                    └── App.java
```

## Maven 坐标与版本

- **groupId**: `com.example2`
- **artifactId**: `java8-maven-hello-world`
- **version**: `0.1.0-SNAPSHOT`
- **Java**: 1.8

## 构建配置

`pom.xml` 关键点：

- `maven-compiler-plugin`：设置 `source/target = 1.8`
- `maven-jar-plugin`：在 manifest 写入 `Main-Class: com.example.App`，以支持 `java -jar`

不引入额外依赖。

## 运行方式

- 构建：
  - `mvn -q -f java8-maven-hello-world/pom.xml package`
- 运行：
  - `java -jar java8-maven-hello-world/target/java8-maven-hello-world-0.1.0-SNAPSHOT.jar`

输出应为：

```
Hello World
```
