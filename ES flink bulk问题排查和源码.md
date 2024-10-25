# ES bulk相关

## 源码分析

本次源码分析是从Flink的角度触发的，从Flink的ElasticsearchSink出发看看bulk的运行流程。

首先创建一个ElasticsearchSink，一个很简单的例子如下：

```java
public ElasticsearchSink<Object> buildElasticsearchSink(ElasticsearchSinkFunction<Object> sinkFunction) {
    List<String> hosts = Arrays.asList("127.0.0.1:9200");
    ElasticsearchSink.Builder<T> esSinkBuilder = new ElasticsearchSink.Builder<>(
        hosts,
        sinkFunction
    );

    // 设置触发bulk的最大action个数，设置-1跳过该配置
    esSinkBuilder.setBulkFlushMaxActions(...);
    // 设置触发bulk的最大buffer，默认单位mb，设置-1跳过该配置
    esSinkBuilder.setBulkFlushMaxSizeMb(...);
    // 设置bulk执行间隔，默认单位ms，设置-1跳过该配置
    esSinkBuilder.setBulkFlushInterval(...);

    // 设置bulk的重试策略是否开启
    esSinkBuilder.setBulkFlushBackoff(...);
    // 设置bulk的重试间隔机制
    esSinkBuilder.setBulkFlushBackoffType(...);
    // 设置bulk的重试次数
    esSinkBuilder.setBulkFlushBackoffRetries(...);
    // 设置bulk的重试间隔
    esSinkBuilder.setBulkFlushBackoffDelay(...);

    // 设置bulk的异常回调
    esSinkBuilder.setFailureHandler(..);

    // 设置bulk的es client配置
    // 通常情况下，用于设置client的超时时间等
    esSinkBuilder.setRestClientFactory(...);

    return esSinkBuilder.build();
}
```

ElasticsearchSink是ElasticsearchSinkBase的一个实现类，实际上的功能都在ElasticsearchSinkBase和Elasticsearch7ApiCallBridge中，往下看：

先看Elasticsearch7ApiCallBridge，这个类是ElasticsearchSinkBase中引用的一个对象，主要作用是：

```java
public class Elasticsearch7ApiCallBridge
        implements ElasticsearchApiCallBridge<RestHighLevelClient> {
    
    // 创建es连接，使用builder中的restClientFactory对client做修改
    public RestHighLevelClient createClient(Map<String, String> clientConfig) {}

    // 创建bulkProcess，指定listener
    public BulkProcessor.Builder createBulkProcessorBuilder(RestHighLevelClient client, BulkProcessor.Listener listener) {}

    // 从bulk返回值中获取失败信息
    public Throwable extractFailureCauseFromBulkItemResponse(BulkItemResponse bulkItemResponse) {}

    // 设置重试机制
    public void configureBulkProcessorBackoff(BulkProcessor.Builder builder,@Nullable ElasticsearchSinkBase.BulkFlushBackoffPolicy flushBackoffPolicy) {}

    // 设置bulk缓存列表，每次add进来的request都会存储在这个indexer中
    public RequestIndexer createBulkProcessorIndexer(BulkProcessor bulkProcessor,boolean flushOnCheckpoint,AtomicLong numPendingRequestsRef) {}

    // 验证当前client是否可用
    public void verifyClientConnection(RestHighLevelClient client) throws IOException {}
}
```

在ElasticsearchSinkBase#open中，创建bulkProcessor时，除了上文提过的action、size、interval和重试机制之外，还有一个concurrentRequests参数，代表着允许同时进行的bulk的数量，默认是1。这个值在原生的Flink中无法指定。

在BulkProcessor#startFlushTask中，会创建一个fixedDelay的定时任务，用于在指定的interval时触发flush()将bulk传递给ES。同样的，在每次向缓冲区中插入request，会校验action和size。

真正的execute逻辑在BulkRequestHandler中，当执行遇到报错时，会在client中调用Retry#onFailure，这里会根据配置的数值进行重试。当concurrentRequests为0时，主线程会通过CountDownLatch#await()的机制强制等待bulk的返回；因为有new Semaphore(concurrentRequests > 0 ? concurrentRequests : 1)的限制，实际上并行的bulk不会超过concurrentRequests的限制，但是异步请求和不阻塞主线程的操作使得在此期间新的bulk请求可以在后台生成，也就意味着后台最多维护concurrentRequests+1个bulk请求。