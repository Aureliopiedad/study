# Flink UI页面统计无法获取的问题

## 问题描述

Flink的UI界面中，每个算子链都会有Flink提供的`Records Received`、`Records sent`等统计，但是在某个项目中，这几个指标一直在转圈，日志中也没有报错等信息(因为日志级别是info)。

## 结论

是因为Flink使用的版本和项目中的kafka-client的版本不一致，在使用Flink kafka consumer和producer的时候，使用了某个老版本的kafka-client中的metric API，这个API在项目指定的kafka-client版本中已经被删除了。

## 定位过程

第一种方法是将日志级别调整成debug日志，会在`org.apache.flink.runtime.metrics.dump.MetricDumpSerialization.MetricDumpSerializer#serialize`方法中出现报错信息，但是debug日志确实多，加上有些时候不太方便调整日志界别。

先根据浏览器的开发者模式，确定返回值对应的pojo类是`org.apache.flink.runtime.rest.messages.job.JobDetailsInfo`。

一层层向下找，发现生成`JobDetailsInfo`的地方位于`org.apache.flink.runtime.rest.handler.job.JobDetailsHandler#createJobDetailsInfo`。

发现这个类中的`org.apache.flink.runtime.rest.handler.job.JobDetailsHandler#createJobVertexDetailsInfo`会构建实际需要的`org.apache.flink.runtime.rest.messages.job.metrics.IOMetricsInfo`。

在构建`IOMetricsInfo`的过程中，在`org.apache.flink.runtime.rest.handler.util.MutableIOMetrics#addIOMetrics`中会调用`org.apache.flink.runtime.rest.handler.legacy.metrics.MetricFetcher#update`，在这个update()方法中，会调用`org.apache.flink.runtime.rest.handler.legacy.metrics.MetricFetcherImpl#queryMetrics`。

在`org.apache.flink.runtime.metrics.dump.MetricQueryService#queryMetrics`中，会调用`org.apache.flink.runtime.metrics.dump.MetricDumpSerialization.MetricDumpSerializer#serialize`，在这个方法里，会实际调用`org.apache.flink.streaming.connectors.kafka.internals.metrics.KafkaMetricWrapper`，这个wrapper就是调用kafka老api的地方。