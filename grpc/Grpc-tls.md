# gRPC tls

## 概述

gRPC具有SSL、TLS集成的能力，可以是使用其对服务器进行数据验证，并对客户端和服务器之间的数据传输进行加密。

## 生成tls 证书和密钥

以下openssl命令基于windows。Linux可能存在不同命令。

```shell
# -des3可删除，是否需要密码
# 生成ca.key
openssl genrsa -des3 -out ca.key 2048

# 生成ca.csr
openssl req -new -key ca.key -out ca.csr

# 生成ca.crt
openssl x509 -req -days 365 -in ca.csr -signkey ca.key -out ca.crt
```

找到openssl.cnf文件，修改如下参数：

```shell
# 开启copy_extensions
copy_extensions = copy

# 找到标签[v3_req]，添加：
subjectAltName = @alt_names

# 新建标签[alt_names]，添加：
DNS.1 = localhost
DNS.2 = *.com
DNS.3 = *
```

继续openssl命令：

```shell
# 生成server.key
openssl genpkey -algorithm RSA -out server.key

# 生成server.csr
openssl req -new -nodes -key server.key -out server.csr -days 3650 -config ./openssl.cnf -extensions v3_req

# 生成server.pem
openssl x509 -req -days 365 -in server.csr -out server.pem -CA ca.crt -CAkey ca.key -CAcreateserial -extfile ./openssl.cnf -extensions v3_req
```

最终得到的文件中，使用server.pem和server.key。

## proto文件

```shell
syntax = "proto3";

option java_multiple_files = true;
option java_package = "com.grpc.test.proto";
option java_outer_classname = "HelloWorldProto";
option objc_class_prefix = "HLW";

package com.grpc.test.proto;

service Greeter {
  rpc SayHello (HelloRequest) returns (HelloReply) {}
}

message HelloRequest {
  string name = 1;
}

message HelloReply {
  string message = 1;
}
```

## Server with tls

```java
@Component
public class GreeterServiceImpl extends GreeterGrpc.GreeterImplBase implements InitializingBean {
    @Override
    public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
        System.out.println("accept hello request and send reply");

        HelloReply helloReply = HelloReply
            .newBuilder()
            .setMessage("name : " + request.getName())
            .build();

        responseObserver.onNext(helloReply);
        responseObserver.onCompleted();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("init Greeter service on port 50051");

//        注释掉的是非tls写法
//        ServerBuilder
//                .forPort(50051)
//                .addService(new GreeterServiceImpl())
//                .build()
//                .start();

        Grpc
            .newServerBuilderForPort(
                    50051,
                    TlsServerCredentials.create(
                            // 读取证书
                            this.getClass().getResourceAsStream("/server.pem"),
                            // 读取密钥
                            this.getClass().getResourceAsStream("/server.key")
                    )
            )
            .addService(new GreeterServiceImpl())
            .build()
            .start();
    }
}
```

## Client with tls

```java
public class GreeterClient {
    public static void greet(String host, int port, String message, String pem) throws IOException {
//        注释掉的是非tls写法
//        ManagedChannel channel = ManagedChannelBuilder
//                .forAddress(host, port)
//                .usePlaintext()
//                .build();

        TlsChannelCredentials.Builder tlsBuilder = TlsChannelCredentials.newBuilder();
        tlsBuilder
            // 读取证书
            .trustManager(new File(pem));

        ManagedChannel channel = Grpc
            .newChannelBuilderForAddress(host, port, tlsBuilder.build())
            .build();

        GreeterGrpc.GreeterBlockingStub blockingStub = GreeterGrpc.newBlockingStub(channel);

        try {
            HelloRequest request = HelloRequest.newBuilder().setName(message).build();
            HelloReply response = blockingStub.sayHello(request);
            System.out.println("response = " + response.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

## 本地执行结果

修改host文件，新增：

```shell
127.0.0.1 hello.com hello
```

- host：localhost
- host：hello.com
- host：hello
  返回结果正常，但是基于openssl.cnf通配符的DNS访问速度很慢。
  说明以下，如果openssl.cnf文件中删除*后，hello无法访问
- host：127.0.0.1
  返回结果异常，报错信息如下：

  ```shell
    java.security.cert.CertificateException: No subject alternative names matching IP address 127.0.0.1 found
  ```

## Linux 环境

同本地环境执行

有一点需要注意，打包客户端时，jar中META-INF/services/io.grpc.LoadBalancerProvider文件需要手动修改为：

```shell
io.grpc.internal.PickFirstLoadBalancerProvider
io.grpc.util.SecretRoundRobinLoadBalancerProvider$Provider
```

打包导致该文件的错误问题暂时不明。

## Nginx修改

以下Nginx配置，实现了grpc和https在同一个端口(443)上进行分别转发。

```shell
server {
   listen 443 ssl http2;
   server_name hello.com;

   ssl on;
   ssl_certificate /root/dafeihou/grpc/server.pem;
   ssl_certificate_key /root/dafeihou/grpc/server.key;

   location /hello {
      proxy_set_header Host $host;
      proxy_set_header X-Real-IP $remote_addr;
      proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
      proxy_set_header X-Forwarded-Proto https;
      proxy_pass http://127.0.0.1:8080/hello;   # 目的地址
      proxy_redirect http:// https://;
   }

   location /com.grpc.test.proto.Greeter/SayHello {
      grpc_pass grpcs://127.0.0.1:50051;
   }
}
```

Nginx access日志如下：

```shell
10.182.139.117 - - [22/Aug/2022:16:47:30 +0800] "GET /hello HTTP/1.1" 200 5 "-" "curl/7.29.0" "-"
10.182.139.117 - - [22/Aug/2022:16:47:35 +0800] "POST /com.grpc.test.proto.Greeter/SayHello HTTP/2.0" 200 17 "-" "grpc-java-netty/1.42.1" "-"
```

## 参考网址

> <https://www.nginx.com/blog/nginx-1-13-10-grpc/> Nginx配置gRPC
> <https://grpc.io/docs/guides/auth/#java> gRPC with tls 官网说明
> <https://github.com/grpc/grpc-java/tree/d4fa0ecc07495097453b0a2848765f076b9e714c/examples/example-tls/src/main/java/io/grpc/examples/helloworldtls> gRPC with tls github 官方范例
> <https://stackoverflow.com/questions/55484043/how-to-fix-could-not-find-policy-pick-first-with-google-tts-java-client> LoadBalancerProvider文件异常处理