# gRPC

## gRPC 概述

gRPC是一个RPC框架，基于HTTP 2设计。

## gRPC使用

### pom配置

```xml
<project>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.1.0.RELEASE</version>
        <relativePath />
    </parent>

    <properties>
        <maven.compiler.source>8</maven.compiler.source>
        <maven.compiler.target>8</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
        <java.version>1.8</java.version>
        <grpc-spring-boot-starter.version>3.0.0</grpc-spring-boot-starter.version>
        <os-maven-plugin.version>1.6.1</os-maven-plugin.version>
        <protobuf-maven-plugin.version>0.6.1</protobuf-maven-plugin.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>io.github.lognet</groupId>
            <artifactId>grpc-spring-boot-starter</artifactId>
            <version>${grpc-spring-boot-starter.version}</version>
            <exclusions>
                <exclusion>
                    <artifactId>protobuf-java-util</artifactId>
                    <groupId>com.google.protobuf</groupId>
                </exclusion>
                <exclusion>
                    <artifactId>re2j</artifactId>
                    <groupId>com.google.re2j</groupId>
                </exclusion>
                <exclusion>
                    <artifactId>grpc-stub</artifactId>
                    <groupId>io.grpc</groupId>
                </exclusion>
                <exclusion>
                    <artifactId>grpc-netty</artifactId>
                    <groupId>io.grpc</groupId>
                </exclusion>
                <exclusion>
                    <artifactId>grpc-protobuf</artifactId>
                    <groupId>io.grpc</groupId>
                </exclusion>
                <exclusion>
                    <artifactId>grpc-services</artifactId>
                    <groupId>io.grpc</groupId>
                </exclusion>
            </exclusions>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>io.grpc</groupId>
            <artifactId>grpc-protobuf</artifactId>
            <version>1.42.1</version>
            <exclusions>
                <exclusion>
                    <artifactId>protobuf-java</artifactId>
                    <groupId>com.google.protobuf</groupId>
                </exclusion>
                <exclusion>
                    <artifactId>error_prone_annotations</artifactId>
                    <groupId>com.google.errorprone</groupId>
                </exclusion>
            </exclusions>
        </dependency>
        <dependency>
            <groupId>io.grpc</groupId>
            <artifactId>grpc-stub</artifactId>
            <version>1.42.1</version>
        </dependency>
        <dependency>
            <groupId>io.grpc</groupId>
            <artifactId>grpc-alts</artifactId>
            <version>1.42.1</version>
            <exclusions>
                <exclusion>
                    <artifactId>error_prone_annotations</artifactId>
                    <groupId>com.google.errorprone</groupId>
                </exclusion>
                <exclusion>
                    <artifactId>guava</artifactId>
                    <groupId>com.google.guava</groupId>
                </exclusion>
                <exclusion>
                    <artifactId>opencensus-api</artifactId>
                    <groupId>io.opencensus</groupId>
                </exclusion>
            </exclusions>
        </dependency>
        <dependency>
            <groupId>io.grpc</groupId>
            <artifactId>grpc-all</artifactId>
            <version>1.42.1</version>
            <exclusions>
                <exclusion>
                    <artifactId>protobuf-java</artifactId>
                    <groupId>com.google.protobuf</groupId>
                </exclusion>
                <exclusion>
                    <artifactId>okio</artifactId>
                    <groupId>com.squareup.okio</groupId>
                </exclusion>
                <exclusion>
                    <artifactId>grpc-stub</artifactId>
                    <groupId>io.grpc</groupId>
                </exclusion>
                <exclusion>
                    <artifactId>grpc-protobuf</artifactId>
                    <groupId>io.grpc</groupId>
                </exclusion>
                <exclusion>
                    <artifactId>grpc-context</artifactId>
                    <groupId>io.grpc</groupId>
                </exclusion>
            </exclusions>
        </dependency>
    </dependencies>

    <build>
        <extensions>
            <extension>
                <groupId>kr.motd.maven</groupId>
                <artifactId>os-maven-plugin</artifactId>
                <version>${os-maven-plugin.version}</version>
            </extension>
        </extensions>

        <!-- 编译gRPC文件、打包server、打包client使用的plugin不同，注意注释 -->
        <plugins>
            <!-- maven-assembly-plugin 用于打包client -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-assembly-plugin</artifactId>
                <version>3.3.0</version>
                <configuration>
                    <archive>
                        <manifest>
                            <mainClass>com.cloud.grpc.test.client.Client</mainClass>
                        </manifest>
                    </archive>
                    <descriptorRefs>
                        <descriptorRef>jar-with-dependencies</descriptorRef>
                    </descriptorRefs>
                </configuration>
                <executions>
                    <execution>
                        <id>make-assembly</id>
                        <phase>package</phase>
                        <goals>
                            <goal>single</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
            <!-- 用于打包server -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-jar-plugin</artifactId>
            </plugin>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
            <!-- 用于生成gRPC文件 -->
            <plugin>
                <groupId>org.xolstice.maven.plugins</groupId>
                <artifactId>protobuf-maven-plugin</artifactId>
                <version>${protobuf-maven-plugin.version}</version>
                <configuration>
                    <protocArtifact>com.google.protobuf:protoc:3.5.1-1:exe:${os.detected.classifier}</protocArtifact>
                    <pluginId>grpc-java</pluginId>
                    <pluginArtifact>io.grpc:protoc-gen-grpc-java:1.16.1:exe:${os.detected.classifier}</pluginArtifact>
                </configuration>
                <executions>
                    <execution>
                        <goals>
                            <goal>compile</goal>
                            <goal>compile-custom</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

### proto文件

#### 样例和详解

proto格式类似于json，根据proto编译器生成数据接口、基于特定编程语言的runtime库以及发送、写入文件的格式。文件默认存放位置在/src/main/proto/。

##### proto文件样例

```java
syntax = "proto3";

// java_multiple_files配置将全部的消息、服务、枚举创建单独的java文件
option java_multiple_files = true;
package com.cloud.grpc.test;

// 定义数据类型
message Request {
  // 此处的数字指的是在某个message中每个字段都需要标识的唯一值。
  // 用于以二进制方式标识字段，一旦使用就不能更改。
  // 1-15需要1byte，16-2047需要2byte。
  string requestParam = 1;
}

message Response {
  string responseParam = 1;
}

// 定义命名服务
service HelloWorldService {
  rpc sayHello (Request) returns (Response);
}

service ByeService {
  rpc sayBye (Request) returns (Response);
}

service TestService {
  rpc test (Request) returns (Response);
}
```

##### proto 字段默认值(常见)

proto 类型 | java 类型 | 默认值
---------|----------|---------
 string | string | 空字符串
 bool | boolean | false
 int32 | int | 0
 int64 | long | 0
 double | double | 0
 float | float | 0

##### proto格式详解

待补充

- compiler invocation
- package
- message
- field

### 编译生成gRPC文件

配置好pom和proto文件后，执行：

```shell
mvn compile
```

如果配置了同样的proto，则会在target/generated-sources/protobuf/下生成gRPC文件。

### 创建服务端

由于proto中配置了三种服务，上述操作会产生HelloWorldServiceGrpc、ByeServiceGrpc、TestServiceGrpc三个命名服务；
同理，会产生Request、Response两种传递类型以及对应的建造者方法。

注意，本文以Spring的方式启动服务端。
非spring的方式可以参考：

> <https://github.com/grpc/grpc-java/blob/master/examples/src/main/java/io/grpc/examples/helloworld/HelloWorldServer.java>

本文仅以HelloWordService的实现举例：

```java
@Component
public class HelloWordServiceIml extends HelloWorldServiceGrpc.HelloWorldServiceImplBase implements InitializingBean {
    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("hello server port 5");

        ServerBuilder
                .forPort(5)
                .addService(new HelloWordServiceIml())
                .build()
                .start();
    }

    @Override
    public void sayHello(Request request, StreamObserver<Response> responseObserver) {
        System.out.println("server accept say hello");

        // proto生成的传递Model只有getter方法没有setter方法，所以只能以builder创建
        Response response = Response
                .newBuilder()
                .setResponseParam("responseHello")
                .build();

        // 这一步是填充返回值
        responseObserver.onNext(response);
        // 这一步是确认返回
        responseObserver.onCompleted();
    }
}
```

通过类似的方法将其他两种服务实现。因为作为bean注入spring，只需启动spring服务即可。
使用端口如下，考虑负载均衡，起了两次：
hello：2，5
bye：3，6
test：4，7

附Dockerfile文件和docker启动命令：
10.182.63.222 存在镜像和dockerfile文件

```shell
FROM openjdk:8-jdk-alpine
COPY cloud-grpc-1.0-SNAPSHOT.jar /app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
```

```shell
docker run -p 2:2 -p 3:3 -p 4:5 grpc-dafeihou:v2
docker run -p 5:5 -p 6:6 -p 7:7 grpc-dafeihou-567:v1
```

### 创建客户端

本文仅以HelloClient举例：

```java
public class HelloClient {
    private final ManagedChannel channel;
    private final HelloWorldServiceGrpc.HelloWorldServiceBlockingStub helloWorldServiceBlockingStub;

    public HelloClient(String host, int port) {
        channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();

        helloWorldServiceBlockingStub = HelloWorldServiceGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws InterruptedException {
        //关闭连接
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public String sayHello(String param) {
        //构造服务调用参数对象
        Request request = Request.newBuilder().setRequestParam(param).build();
        //调用远程服务方法
        Response response = helloWorldServiceBlockingStub.sayHello(request);
        //返回值
        return response.getResponseParam();
    }


    public static void main(String[] args) throws InterruptedException {
        String host = args[0];
        int port = Integer.parseInt(args[1]);

        System.out.println("=================== 尝试进行rpc通信 =======================");
        System.out.println("host = " + host);
        System.out.println("port = " + port);

        HelloClient client = new HelloClient(host, port);
        //服务调用
        String content = client.sayHello("Java client");
        //打印调用结果
        System.out.println(content + "\n\n\n");
        //关闭连接
        client.shutdown();
        System.out.println("=================== rpc通信完成 =======================");
    }
}
```

### 本地执行结果

服务端：

```shell
bye server port 6
hello server port 5
test server port 7

server accept say hello
```

客户端：

```shell
=================== 尝试进行rpc通信 =======================
host = 127.0.0.1
port = 5

responseHello



=================== rpc通信完成 =======================
```

## 使用Nginx进行服务分发和负载均衡

根据官方说法，需要依赖1.13以上的nginx。本文使用10.182.63.222环境的nginx，版本为1.20.1。

新增Nginx配置如下：

```shell
    client_max_body_size 4000M;
    grpc_read_timeout 1d;
    grpc_send_timeout 1d;
    grpc_buffer_size 100M;

    underscores_in_headers on;
```

新增Nginx转发配置如下：

```shell
server {
   listen 80 http2;

   # default port 2 is hello
   # location / {
   #    grpc_pass grpc://127.0.0.1:2;
   # }

   # POST /com.cloud.grpc.test.HelloWorldService
   location /com.cloud.grpc.test.HelloWorldService {
      grpc_pass grpc://grpc_hello;
   }

   # POST /com.cloud.grpc.test.ByeService
   location /com.cloud.grpc.test.ByeService {
      grpc_pass grpc://grpc_bye;
   }

   # POST /com.cloud.grpc.test.TestService
   location /com.cloud.grpc.test.TestService {
      grpc_pass grpc://grpc_test;
   }
}
```

新增Nginx负载均衡配置日志如下：

```shell
upstream grpc_hello{
    server 10.182.63.222:2;
    server 10.182.63.222:5;
}
upstream grpc_bye{
    server 10.182.63.222:3;
    server 10.182.63.222:6;
}
upstream grpc_test{
    server 10.182.63.222:4;
    server 10.182.63.222:7;
}
```

需要修改客户端如下：

```java
public class Client {
    public static void main(String[] args) throws InterruptedException {
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        int service = Integer.parseInt(args[2]);

        System.out.println("=================== 尝试进行rpc通信 =======================");
        System.out.println("host = " + host);
        System.out.println("port = " + port);

        switch (port) {
            case 2:
                HelloClient helloClient = new HelloClient(host, port);
                //服务调用
                String helloContent = helloClient.sayHello("Java client");
                //打印调用结果
                System.out.println(helloContent + "\n\n\n");
                //关闭连接
                helloClient.shutdown();
                break;
            case 3:
                ByeClient byeClient = new ByeClient(host, port);
                //服务调用
                String byeContent = byeClient.sayBye("Java client");
                //打印调用结果
                System.out.println(byeContent + "\n\n\n");
                //关闭连接
                byeClient.shutdown();
                break;
            case 4:
                TestClient testClient = new TestClient(host, port);
                //服务调用
                String testContent = testClient.test("Java client");
                //打印调用结果
                System.out.println(testContent + "\n\n\n");
                //关闭连接
                testClient.shutdown();
                break;
            default:
                switch (service) {
                    case 2:
                        HelloClient helloClient1 = new HelloClient(host, port);
                        //服务调用
                        String helloContent1 = helloClient1.sayHello("Java client");
                        //打印调用结果
                        System.out.println(helloContent1 + "\n\n\n");
                        //关闭连接
                        helloClient1.shutdown();
                        break;
                    case 3:
                        ByeClient byeClient1 = new ByeClient(host, port);
                        //服务调用
                        String byeContent1 = byeClient1.sayBye("Java client");
                        //打印调用结果
                        System.out.println(byeContent1 + "\n\n\n");
                        //关闭连接
                        byeClient1.shutdown();
                        break;
                    case 4:
                        TestClient testClient1 = new TestClient(host, port);
                        //服务调用
                        String testContent1 = testClient1.test("Java client");
                        //打印调用结果
                        System.out.println(testContent1 + "\n\n\n");
                        //关闭连接
                        testClient1.shutdown();
                        break;
                    default:
                        break;
                }
                break;
        }

        System.out.println("=================== rpc通信完成 =======================");
    }
}
```

当不使用Nginx做转发时，直接指定2 - 7端口即可。
使用Nginx转发时，命令行如下，使用端口为80：

```shell
java -jar cloud-grpc-1.0-SNAPSHOT-jar-with-dependencies.jar 10.182.63.222 80 2 | grep response
```

Nginx access 日志如下(部分)：

```shell
10.182.139.117 - - [18/Aug/2022:15:03:59 +0800] "POST /com.cloud.grpc.test.HelloWorldService/sayHello HTTP/2.0" 200 20 "-" "grpc-java-netty/1.15.1" "-"

10.182.139.117 - - [18/Aug/2022:15:04:06 +0800] "POST /com.cloud.grpc.test.ByeService/sayBye HTTP/2.0" 200 18 "-" "grpc-java-netty/1.15.1" "-"

10.182.139.117 - - [18/Aug/2022:15:04:13 +0800] "POST /com.cloud.grpc.test.TestService/test HTTP/2.0" 200 19 "-" "grpc-java-netty/1.15.1" "-"
```

负载均衡展示：

累计发送13条RPC请求，两个docker容器日志如下：

第一个，端口 2，3，4：

```shell
server accept say bye
server accept say bye
server accept say bye
server accept say bye
server accept say bye
server accept say bye
server accept say hello
server accept say hello
server accept say hello
server accept say hello
```

第二个，端口 5，6，7：

```shell
server accept say bye
server accept say bye
server accept say hello
```

### 为什么gRPC做负载均衡很棘手

> <https://zhuanlan.zhihu.com/p/349734892>

gRPC使用粘性连接，这意味着从客户端到服务器建立连接时，相同的连接会尽可能的长时间复用于多个请求，这么做时为了避免TCP握手消耗的时间和资源。
所以，当同一个客户端发送大量请求时，都会去同一个实例。

## 选择断开

客户端关闭channel，即可关闭对应的RPC连接。

服务端目前只找到关闭整个server的方法，指定关闭与某个客户端的连接暂无。
