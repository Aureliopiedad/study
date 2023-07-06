# Flink算子详解

用户能通过算子将一个或多个DataStream转化。

## 关于DataStream#clean()

在很多算子源码中，Flink使用了该方法。

该方法的存在的原因是，Flink使用了很多内部类，如果对内部类进行序列化，而它的外部类不支持序列化的话，会抛出异常。clean()的作用在于，将内部类中对外部类的引用改为null。

```java
public class DataStream<T> {
    ...
    protected <F> F clean(F f) {
        return this.getExecutionEnvironment().clean(f);
    }
    ...
}

public class StreamExecutionEnvironment {
    ...
    public <F> F clean(F f) {
        if (this.getConfig().isClosureCleanerEnabled()) {
            ClosureCleaner.clean(f, this.getConfig().getClosureCleanerLevel(), true);
        }

        ClosureCleaner.ensureSerializable(f);
        return f;
    }
    ...
}
public class ExecutionConfig implements Serializable, Archiveable<ArchivedExecutionConfig> {
    ...
    //默认情况下，closureCleanerLevel=RECURSIVE
    public boolean isClosureCleanerEnabled() {
        return this.closureCleanerLevel != ExecutionConfig.ClosureCleanerLevel.NONE;
    }
    ...
}

public class ClosureCleaner {
    ...
    public static void clean(Object func, ClosureCleanerLevel level, boolean checkSerializable) {
        clean(func, level, checkSerializable, Collections.newSetFromMap(new IdentityHashMap()));
    }

    private static void clean(Object func, ClosureCleanerLevel level, boolean checkSerializable, Set<Object> visited) {
        //大概就是利用反射遍历字段，如果当前字段属性名以"this$"开头，将该引用设为null
        ...
    }

    //尝试序列化
    public static void ensureSerializable(Object obj) {
        try {
            InstantiationUtil.serializeObject(obj);
        } catch (Exception var2) {
            throw new InvalidProgramException("Object " + obj + " is not serializable", var2);
        }
    }
    ...
}
```

## 关于StreamRecord

在一个job中流动的数据共有四种，分别为：StreamRecord(业务数据的包装类)、Watermark、StreamStatus(表示某一条闲置或活动)和LatencyMarker(延迟处理)，这四个类都继承自StreamElement。

```java
public abstract class StreamElement {
    public StreamElement() {
    }

    public final boolean isWatermark() {
        return this.getClass() == Watermark.class;
    }

    public final boolean isStreamStatus() {
        return this.getClass() == StreamStatus.class;
    }

    public final boolean isRecord() {
        return this.getClass() == StreamRecord.class;
    }

    public final boolean isLatencyMarker() {
        return this.getClass() == LatencyMarker.class;
    }

    //执行本方法生成StreamRecord
    public final <E> StreamRecord<E> asRecord() {
        return (StreamRecord)this;
    }

    public final Watermark asWatermark() {
        return (Watermark)this;
    }

    public final StreamStatus asStreamStatus() {
        return (StreamStatus)this;
    }

    public final LatencyMarker asLatencyMarker() {
        return (LatencyMarker)this;
    }
}
```

在后续介绍的算子中，可以看到实际传进算子的数据是StreamRecord类型的。

```java
public final class StreamRecord<T> extends StreamElement {
    private T value;
    private long timestamp;
    private boolean hasTimestamp;
    ...
}
```

顺便一提，对于StreamRecord，其中的时间戳字段在这里赋值：

```java
public class TimestampsAndWatermarksOperator<T> extends AbstractStreamOperator<T> implements OneInputStreamOperator<T, T>, ProcessingTimeCallback {
    ...
    public void processElement(StreamRecord<T> element) throws Exception {
        T event = element.getValue();
        long previousTimestamp = element.hasTimestamp() ? element.getTimestamp() : -9223372036854775808L;
        long newTimestamp = this.timestampAssigner.extractTimestamp(event, previousTimestamp);
        element.setTimestamp(newTimestamp);
        this.output.collect(element);
        this.watermarkGenerator.onEvent(event, newTimestamp, this.wmOutput);
    }
    ...
}
```

对于TimestampsAndWatermarksOperator，也有一个转化叫做：

```java
public class TimestampsAndWatermarksTransformation<IN> extends PhysicalTransformation<IN> {
    ...
}
```

## 流转化

### map()

将DataStream转化为DataStream。每获取一个元素就产生一个元素。

```java
public class DataStream<T> {
    ...
    public <R> SingleOutputStreamOperator<R> map(MapFunction<T, R> mapper) {
        TypeInformation<R> outType = TypeExtractor.getMapReturnTypes((MapFunction)this.clean(mapper), this.getType(), Utils.getCallLocationName(), true);
        return this.map(mapper, outType);
    }

    public <R> SingleOutputStreamOperator<R> map(MapFunction<T, R> mapper, TypeInformation<R> outputType) {
        return this.transform("Map", outputType, (OneInputStreamOperator)(new StreamMap((MapFunction)this.clean(mapper))));
    }
    ...
}

public interface MapFunction<T, O> extends Function, Serializable {
    O map(T var1) throws Exception;
}

public class StreamMap<IN, OUT> extends AbstractUdfStreamOperator<OUT, MapFunction<IN, OUT>> implements OneInputStreamOperator<IN, OUT> {
    private static final long serialVersionUID = 1L;

    public StreamMap(MapFunction<IN, OUT> mapper) {
        super(mapper);
        this.chainingStrategy = ChainingStrategy.ALWAYS;
    }

    public void processElement(StreamRecord<IN> element) throws Exception {
        this.output.collect(
            element.replace(
                ((MapFunction)this.userFunction)
                    .map(element.getValue())
            )
        );
    }
}
```

### flatMap()

将DataStream转化为DataStream。每获取一个元素就产生零个、一个或多个元素。

```java
public class DataStream<T> {
    ...
    public <R> SingleOutputStreamOperator<R> flatMap(FlatMapFunction<T, R> flatMapper) {
        TypeInformation<R> outType = TypeExtractor.getFlatMapReturnTypes((FlatMapFunction)this.clean(flatMapper), this.getType(), Utils.getCallLocationName(), true);
        return this.flatMap(flatMapper, outType);
    }

    public <R> SingleOutputStreamOperator<R> flatMap(FlatMapFunction<T, R> flatMapper, TypeInformation<R> outputType) {
        return this.transform("Flat Map", outputType, (OneInputStreamOperator)(new StreamFlatMap((FlatMapFunction)this.clean(flatMapper))));
    }
    ...
}

public interface FlatMapFunction<T, O> extends Function, Serializable {
    void flatMap(T var1, Collector<O> var2) throws Exception;
}

public class StreamFlatMap<IN, OUT> extends AbstractUdfStreamOperator<OUT, FlatMapFunction<IN, OUT>> implements OneInputStreamOperator<IN, OUT> {
    private static final long serialVersionUID = 1L;
    private transient TimestampedCollector<OUT> collector;

    public StreamFlatMap(FlatMapFunction<IN, OUT> flatMapper) {
        super(flatMapper);
        this.chainingStrategy = ChainingStrategy.ALWAYS;
    }

    public void open() throws Exception {
        super.open();
        this.collector = new TimestampedCollector(this.output);
    }

    public void processElement(StreamRecord<IN> element) throws Exception {
        this.collector.setTimestamp(element);
        ((FlatMapFunction)this.userFunction).flatMap(element.getValue(), this.collector);
    }
}
```

### filter

将DataStream转化为DataStream。每获取一个元素获取一个布尔值，保留true的数据。

```java
public class DataStream<T> {
    ...
    public SingleOutputStreamOperator<T> filter(FilterFunction<T> filter) {
        return this.transform("Filter", this.getType(), (OneInputStreamOperator)(new StreamFilter((FilterFunction)this.clean(filter))));
    }
    ...
}

public interface FilterFunction<T> extends Function, Serializable {
    boolean filter(T var1) throws Exception;
}

public class StreamFilter<IN> extends AbstractUdfStreamOperator<IN, FilterFunction<IN>> implements OneInputStreamOperator<IN, IN> {
    private static final long serialVersionUID = 1L;

    public StreamFilter(FilterFunction<IN> filterFunction) {
        super(filterFunction);
        this.chainingStrategy = ChainingStrategy.ALWAYS;
    }

    public void processElement(StreamRecord<IN> element) throws Exception {
        if (((FilterFunction)this.userFunction).filter(element.getValue())) {
            this.output.collect(element);
        }

    }
}
```

### keyBy

将DataStream转化为KeyedStream。在逻辑上将流划分成不相交的分区，具有相同键的数据被分配到同一个分区，在内部是通过哈希分区实现的。

注意，有两种情况不能设为key：

- 一个没有实现hashcode()的pojo对象。
- 一个数组。

```java
public class DataStream<T> {
    ...
    public <K> KeyedStream<T, K> keyBy(KeySelector<T, K> key) {
        Preconditions.checkNotNull(key);
        return new KeyedStream(this, (KeySelector)this.clean(key));
    }

    public <K> KeyedStream<T, K> keyBy(KeySelector<T, K> key, TypeInformation<K> keyType) {
        Preconditions.checkNotNull(key);
        Preconditions.checkNotNull(keyType);
        return new KeyedStream(this, (KeySelector)this.clean(key), keyType);
    }

    private KeyedStream<T, Tuple> keyBy(Keys<T> keys) {
        return new KeyedStream(this, (KeySelector)this.clean(KeySelectorUtil.getSelectorForKeys(keys, this.getType(), this.getExecutionConfig())));
    }
    ...
}

public interface KeySelector<IN, KEY> extends Function, Serializable {
    KEY getKey(IN var1) throws Exception;
}

public class KeyedStream<T, KEY> extends DataStream<T> {
    ...
    public KeyedStream(DataStream<T> dataStream, KeySelector<T, KEY> keySelector) {
        this(dataStream, keySelector, TypeExtractor.getKeySelectorTypes(keySelector, dataStream.getType()));
    }

    //在本构造器方法中传入KeyGroupStreamPartitioner，作为一个分区选择器
    public KeyedStream(DataStream<T> dataStream, KeySelector<T, KEY> keySelector, TypeInformation<KEY> keyType) {
        this(dataStream, new PartitionTransformation(dataStream.getTransformation(), new KeyGroupStreamPartitioner(keySelector, 128)), keySelector, keyType);
    }

    @Internal
    KeyedStream(DataStream<T> stream, PartitionTransformation<T> partitionTransformation, KeySelector<T, KEY> keySelector, TypeInformation<KEY> keyType) {
        //执行DataStream的构造方法，将PartitionTransformation作为转化传入新的DataStream
        super(stream.getExecutionEnvironment(), partitionTransformation);
        this.keySelector = (KeySelector)this.clean(keySelector);
        this.keyType = this.validateKeyType(keyType);
    }
    ...
}
```

### reduce

将keyedStream转为DataStream，用最新的数据和上次处理的结果进行操作。

```java
public class DataStream<T> {
    ...
    public SingleOutputStreamOperator<T> reduce(ReduceFunction<T> reducer) {
        ReduceTransformation<T, KEY> reduce = new ReduceTransformation(
            "Keyed Reduce", 
            this.environment.getParallelism(), 
            this.transformation, 
            (ReduceFunction)this.clean(reducer), 
            this.keySelector, 
            this.getKeyType());
        this.getExecutionEnvironment().addOperator(reduce);
        return new SingleOutputStreamOperator(this.getExecutionEnvironment(), reduce);
    }
    ...
}

public interface ReduceFunction<T> extends Function, Serializable {
    T reduce(T var1, T var2) throws Exception;
}
```