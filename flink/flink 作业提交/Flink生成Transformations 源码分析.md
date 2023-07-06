# Flink生成Transformations

> A Transformation represents the operation that creates a DataStream. Every DataStream has an underlying Transformation that is the origin of said DataStream.
> API operations such as DataStream#map create a tree of Transformations underneath. When the stream program is to be executed this graph is translated to a StreamGraph using StreamGraphGenerator.
> A Transformation does not necessarily correspond to a physical operation at runtime. Some operations are only logical concepts. Examples of this are union, split/select data stream, partitioning.
> 一个Transformation类表示创建DataStream的操作，每个DataStream都有一个底层的Transformation，表示该数据流的起源。
> DataStream中的各种API操作，例如map()、filter()等操作会创建一个转换树，当执行程序时，会使用StreamGraphGenerator类转换成StreamGraph。
> Transformation有物理转换和逻辑转换。

先来看看Transformation类源码：

```java
public abstract class Transformation<T> {
	...
	protected static Integer idCounter = 0;
    protected final int id;
    protected String name;
    protected TypeInformation<T> outputType;
    ...
    private int parallelism;
    private int maxParallelism = -1;
    ...
    private String uid;
    ...
    
    public static int getNewNodeId() {
        Integer var0 = idCounter;
        idCounter = idCounter + 1;
        return idCounter;
    }
    
    public Transformation(String name, TypeInformation<T> outputType, int parallelism) {
        ...
        this.id = getNewNodeId();
        ...
    }
    ...
}
```

在DataStream类中，声明有一个Transformation<T>类型的变量transformation。

```java
	//在union算子中，会通过列表收集全部流中的transformation
	public final DataStream<T> union(DataStream<T>... streams) {
        List<Transformation<T>> unionedTransforms = new ArrayList();
        unionedTransforms.add(this.transformation);
        DataStream[] var3 = streams;
        int var4 = streams.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            DataStream<T> newStream = var3[var5];
            if (!this.getType().equals(newStream.getType())) {
                throw new IllegalArgumentException("Cannot union streams of different types: " + this.getType() + " and " + newStream.getType());
            }

            unionedTransforms.add(newStream.getTransformation());
        }

        return new DataStream(this.environment, new UnionTransformation(unionedTransforms));
    }
    
    public SingleOutputStreamOperator<T> assignTimestampsAndWatermarks(WatermarkStrategy<T> watermarkStrategy) {
        WatermarkStrategy<T> cleanedStrategy = (WatermarkStrategy)this.clean(watermarkStrategy);
        int inputParallelism = this.getTransformation().getParallelism();
        TimestampsAndWatermarksTransformation<T> transformation = new TimestampsAndWatermarksTransformation("Timestamps/Watermarks", inputParallelism, this.getTransformation(), cleanedStrategy);
        this.getExecutionEnvironment().addOperator(transformation);
        return new SingleOutputStreamOperator(this.getExecutionEnvironment(), transformation);
    }
    
    //keyBy，map，flatMap，process，filter都会执行本方法
    protected <R> SingleOutputStreamOperator<R> doTransform(String operatorName, TypeInformation<R> outTypeInfo, StreamOperatorFactory<R> operatorFactory) {
        this.transformation.getOutputType();
        OneInputTransformation<T, R> resultTransform = new OneInputTransformation(this.transformation, operatorName, operatorFactory, outTypeInfo, this.environment.getParallelism());
        SingleOutputStreamOperator<R> returnStream = new SingleOutputStreamOperator(this.environment, resultTransform);
        this.getExecutionEnvironment().addOperator(resultTransform);
        return returnStream;
    }
    
    public DataStreamSink<T> addSink(SinkFunction<T> sinkFunction) {
        this.transformation.getOutputType();
        if (sinkFunction instanceof InputTypeConfigurable) {
            ((InputTypeConfigurable)sinkFunction).setInputType(this.getType(), this.getExecutionConfig());
        }

        StreamSink<T> sinkOperator = new StreamSink((SinkFunction)this.clean(sinkFunction));
        DataStreamSink<T> sink = new DataStreamSink(this, sinkOperator);
        this.getExecutionEnvironment().addOperator(sink.getTransformation());
        return sink;
    }
```

再举两个例子：

```java
public class OneInputTransformation<IN, OUT> extends PhysicalTransformation<OUT> {
    private final Transformation<IN> input;
    private final StreamOperatorFactory<OUT> operatorFactory;
    private KeySelector<IN, ?> stateKeySelector;
    private TypeInformation<?> stateKeyType;
    ...
}

public class LegacySinkTransformation<T> extends PhysicalTransformation<Object> {
    private final Transformation<T> input;
    private final StreamOperatorFactory<Object> operatorFactory;
    private KeySelector<T, ?> stateKeySelector;
    private TypeInformation<?> stateKeyType;
    ...
}
```

最后引入一个TransformationTranslator概念：

> A `TransformationTranslator` is responsible for translating a given [`Transformation`](https://nightlies.apache.org/flink/flink-docs-release-1.14/api/java/org/apache/flink/api/dag/Transformation.html) to its runtime implementation depending on the execution mode.
>
> TransformationTranslator负责根据执行模式(批处理/流处理)将给定的Transformation转换为其运行时实现。

```java
public interface TransformationTranslator<OUT, T extends Transformation<OUT>> {
    
    Collection<Integer> translateForBatch(T var1, TransformationTranslator.Context var2);

    Collection<Integer> translateForStreaming(T var1, TransformationTranslator.Context var2);

    public interface Context {
        StreamGraph getStreamGraph();

        Collection<Integer> getStreamNodeIds(Transformation<?> var1);

        String getSlotSharingGroup();

        long getDefaultBufferTimeout();

        ReadableConfig getGraphGeneratorConfig();
    }
}
```