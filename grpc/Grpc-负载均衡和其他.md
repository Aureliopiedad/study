# 一些其他关于gRPC的发现

## 客户端发送h2请求，到达Nginx后解析成h2c请求发给客户端

> <https://www.nginx.com/blog/nginx-1-13-10-grpc/#grpc_pass>

### Nginx配置修改

```shell
server {
   listen 443 ssl http2;
   server_name hello.com;

   # 添加客户端使用的证书和私钥
   ssl on;
   ssl_certificate /root/dafeihou/grpc/server.pem;
   ssl_certificate_key /root/dafeihou/grpc/server.key;

   location /com.grpc.test.proto.Greeter/SayHello {
      # 将grpcs修正为grpc，意味着从nginx转发出去的流量是不加密的
      grpc_pass grpc://127.0.0.1:50051;
   }

   location /com.grpc.test.proto.Bye/SayBye {
      # 将grpcs修正为grpc，意味着从nginx转发出去的流量是不加密的
      grpc_pass grpc://127.0.0.1:50052;
   }
}
```

### 客户端修改

客户端需要发送Tls加密请求，和Nginx共用证书和密钥

### 服务端修改

服务端只需要接受明文流量

## Nginx http2问题

同一个端口，不同的配置。只要其中一个开启http2，其他配置会自动兼容http2。

## 负载均衡问题

> <https://grpc.io/blog/grpc-load-balancing/>

虽然由Nginx或者k8s可以做到一定程度的负载均衡，但是长连接、多路复用、并行通信的特性导致负载均衡并没有想象中好用。

### 负载均衡可选项

#### 服务端实现负载均衡

优点 | 缺点
---------|----------
 客户端配置简单 | 代理均衡器需要部署在数据流上
 客户端不需要对服务端有依赖 | 较高延迟
 可以由不被信任的客户端发出 | 负载均衡吞吐量限制gRPC扩展性

#### 客户端实现负载均衡

优点 | 缺点
---------|----------
 性能较高 | 需要客户端实现负载均衡，较复杂
  / | 需要客户端持续跟踪服务端负载和运行状态
  / | 客户端和服务端不同的编程语言交互问题
  / | 必须是可信任的客户端

#### 可选方案

Nginx暂时没有官方的解决方案。(官方原话：Nginx coming soon)
可以使用Service Mesh，使用istio或envoy。

### K8s官方

> <https://kubernetes.io/blog/2018/11/07/grpc-load-balancing-on-kubernetes-without-tears/>

K8s官方提供了三种解决方案：

- 客户端自身维护一个负载均衡
- 客户端根据K8s提供的DNS记录选择发送
- 轻量级代理，文档中采取Linkerd做服务代理

> <https://istio.io/latest/blog/2021/proxyless-grpc/>

istio关于gRPC的描述。

### Nginx

虽然文档里说Nginx暂不支持，但是还是有效果的。
测试时服务端为3个副本。

不通过Nginx转发：同一个channel中全部的请求均发往同一个pod

通过Nginx转发：三个pod分别接收到9、2、9条请求。

### istio

通过istio做代理，测试时有两个副本，两个副本分别接受20、20条请求。

#### 重要：Istio负载均衡配置流程

```shell
# 获取 istio入网网关 pod配置
kubectl get deployment -n istio-system istio-ingressgateway -o yaml > istio-ingressgateway-deployment.yaml
# 获取 istio入网网关 svc配置
kubectl -n istio-system get service istio-ingressgateway -o  yaml > istio-ingressgateway.yaml
```

打开istio-ingressgateway-deployment.yaml，找到并新增以下配置：

```yaml
    # 前略
        name: istio-proxy
        ports:
        - containerPort: 9090 # 新增istio监听grpc端口
          protocol: TCP # 新增istio监听grpc端口
        - containerPort: 15021
          protocol: TCP
        - containerPort: 8080
          protocol: TCP
        - containerPort: 8443
          protocol: TCP
        - containerPort: 15012
          protocol: TCP
        - containerPort: 15443
          protocol: TCP
        - containerPort: 15090
          name: http-envoy-prom
          protocol: TCP
    # 后略
```

打开istio-ingressgateway.yaml，找到并新增以下配置：

```yaml
# 前略
spec:
  clusterIP: 10.104.211.7
  externalTrafficPolicy: Cluster
  ports:
  - name: grpc # 新增grpc端口
    nodePort: 31399 # 对外暴露端口，gRPC请求直接端口
    port: 9090 # istio入网网关将流量转发到该配置的端口下
    protocol: TCP 
    targetPort: 9090 # 入网网关pod中新增的端口
  - name: status-port
    nodePort: 28805
    port: 15021
    protocol: TCP
    targetPort: 15021
  - name: http2
    nodePort: 20005
    port: 80
    protocol: TCP
    targetPort: 8080
  - name: https
    nodePort: 41273
    port: 443
    protocol: TCP
    targetPort: 8443
  - name: tcp-istiod
    nodePort: 23435
    port: 15012
    protocol: TCP
    targetPort: 15012
  - name: tls
    nodePort: 55359
    port: 15443
    protocol: TCP
    targetPort: 15443
# 后略
```

重启istio 入网网关：

```shell
kubectl apply -f istio-ingressgateway-deployment.yaml
kubectl apply -f istio-ingressgateway.yaml 
```

查看istio-ingressgateway是否成功新增端口：

```shell
kubectl get svc -A | grep istio-ingressgateway
```

以上，istio配置已完成。开始进行服务端配置。

创建k8s配置文件如下：

```yaml
# 启动服务端所用的deployment
apiVersion: apps/v1
kind: Deployment
metadata:
  name: grpc-deployment
  labels:
    app: grpc-server
  namespace: cloud-grpc # 事先创建或更换命名域空间
spec:
  replicas: 1 # 为测试负载均衡，可以增加副本数
  selector:
    matchLabels:
      app: grpc-server
  template:
    metadata:
      labels:
        app: grpc-server
    spec:
      containers:
      - name: grpc-server
        image: grpc-dafeihou:v1
        imagePullPolicy: IfNotPresent
        ports:
        - containerPort: 8080 # grpc以spring启动，8080为spring端口
          name: "spring"
        - containerPort: 50051 # 第一个grpc端口
          name: "grpc-hello"
        - containerPort: 50052 # 第二个grpc端口
          name: "grpc-bye"
---
# 对应deployemnt使用的svc
apiVersion: v1
kind: Service
metadata:
  name: grpc-service
  namespace: cloud-grpc
spec:
  # 由于端口不需要对外暴露，所以cluster即可
  selector:
    app: grpc-server
  ports:
  - port: 8080
    targetPort: 8080
    name: app
  - port: 50051
    targetPort: 50051
    name: grpc-50051 # 注意，必须以grpc开头
    protocol: TCP
  - port: 50052
    targetPort: 50052
    name: grpc-50052 # 注意，必须以grpc开头
---
# istio gateway
apiVersion: networking.istio.io/v1beta1
kind: Gateway
metadata:
  name: grpc-gateway
  namespace: cloud-grpc
spec:
  selector:
    istio: ingressgateway # 使用istio默认的入网网关
  servers:
  # istio ingressgateway中配置，将http流量转入80端口
  - port:
      number: 80
      name: http
      protocol: HTTP
    hosts:
    - "hello.com" # 由于nginx中配置了proxy_set_header Host，这里可以接收到host
  # istio ingressgateway中配置，将grpc流量转入9090
  - port:
      number: 9090
      name: grpc
      protocol: GRPC
    hosts:
    # 待验证，nginx转发后，grpc流量会丢失host，导致必须使用通配
    # 更新，已解决，需要在nginx配置中添加grpc_set_header Host $host;
    - "*" 
---
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: grpc-vs
  namespace: cloud-grpc
spec:
  gateways:
  - grpc-gateway
  hosts:
  - "*" # 已经过gateway过滤
  http:
  # spring中的测试接口
  - name: app-route-8080
    match:
    - uri:
        prefix: "/hello"
    route:
    - destination:
        host: grpc-service.cloud-grpc.svc.cluster.local
        port:
          number: 8080
    # grpc
  - name: grpc-50051
    match:
    - uri:
        # 和nginx中location一致
        prefix: "/com.grpc.test.proto.Greeter/SayHello"
    route:
    - destination:
        host: grpc-service.cloud-grpc.svc.cluster.local
        port:
          number: 50051
  # grpc
  - name: grpc-route-50052
    match:
    - uri:
        # 和nginx中location一致
        prefix: "/com.grpc.test.proto.Bye/SayBye"
    route:
    - destination:
        host: grpc-service.cloud-grpc.svc.cluster.local
        port:
          number: 50052
```

启动服务端：

```shell
# 手动注入istio，也可以配置自动注入
istioctl kube-inject -f deployment.yaml | kubectl apply -f -
```

附上Nginx配置：

```shell
server {
   listen 443 ssl http2;
   server_name hello.com;

   ssl on;
   ssl_certificate /root/dafeihou/grpc/server.pem;
   ssl_certificate_key /root/dafeihou/grpc/server.key;

   location /hello {
      proxy_set_header Host $host;
      proxy_http_version 1.1; # 由于istio的原因需要制定http版本
      proxy_pass http://10.182.63.222:20005/hello;
   }

   location /com.grpc.test.proto.Greeter/SayHello {
      grpc_set_header Host $host;
      grpc_pass grpc://10.182.63.222:31399;
   }

   location /com.grpc.test.proto.Bye/SayBye {
      grpc_set_header Host $host;
      grpc_pass grpc://10.182.63.222:31399;
   }
}
```
