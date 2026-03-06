# Excel 列头读取器

使用 Java 8 + Maven 开发的 Excel 读取工具，可读取 Excel 文件并输出每列的列头。

## 功能

- 支持 `.xls`（Excel 97-2003）和 `.xlsx`（Excel 2007+）格式
- 读取第一行作为列头
- 输出每列的列头名称

## 环境要求

- JDK 8+
- Maven 3.x

## 构建项目

```bash
mvn clean compile
```

## 运行方式

### 方式一：使用 Maven 运行

```bash
mvn exec:java -Dexec.args="你的Excel文件路径.xlsx"
```

### 方式二：打包后运行

```bash
mvn package
java -cp target/excel-reader-1.0-SNAPSHOT.jar;target/lib/* com.excel.reader.ExcelColumnReader 你的Excel文件路径.xlsx
```

### 示例

```bash
mvn exec:java -Dexec.args="D:/data/员工表.xlsx"
```

## 输出示例

```
列头列表（共 5 列）:
  列 1: 姓名
  列 2: 年龄
  列 3: 部门
  列 4: 入职日期
  列 5: 薪资
```

## 项目结构

```
excel-reader/
├── pom.xml
├── README.md
└── src/
    └── main/
        └── java/
            └── com/
                └── excel/
                    └── reader/
                        └── ExcelColumnReader.java
```
