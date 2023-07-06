# Flink-基础应用

## Flink简介

Flink是一个分布式计算框架，用于对有界和无界数据流进行有状态的计算。

| 关键字介绍   |                                                              |
| ------------ | :----------------------------------------------------------- |
| 流           | 任何类型的数据都作为事件流产生的。                           |
| 有界流       | 有界流定义了数据的范围，有明确定义的开始和结束。在执行计算之前就可以摄取全部的数据。<br />处理有界流不需要进行有序摄取，因为可以对流进行排序。有界流处理也被称为批处理。 |
| 无界流       | 无界流有一个起始点，不存在定义的结束点，在数据生成时并不会停止并持续提供数据。<br />无界流需要持续处理。处理无界流需要以特定的顺序摄取，以便推断结果的完整性。 |
| 有状态的计算 | 在数据流中，许多操作只关注一次单独的事件，但是有些操作会记住许多事件的信息。<br />这些操作被认为是有状态的。 |

| 流的其他分类 |                                          |
| ------------ | ---------------------------------------- |
| 实时流       | 在流生成时进行处理。                     |
| 记录流       | 将流持久化到存储系统，并在之后进行处理。 |

### Flink 有界流Demo

```java
public static void main(String[] args) throws Exception{
    StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
    
    List<Data> list = new ArrayList<>();
    list.add(new Data(1));
    list.add(new Data(2));
    DataStreamSource<Data> dataSource = env.fromCollection(list).setParallelism(1);
    dataSource.print();
    
    env.execute();
}
```

### Flink 无界流Demo

```java
public static void main(String[] args) throws Exception {
    StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
    
    Properties properties = new Properties();
    properties.setProperty("bootstrap.servers", "localhost:9092");
    properties.setProperty("group.id", "flink-consumer");
    
    FlinkKafkaConsumer<String> flinkKafkaConsumer = new FlinkKafkaConsumer<String>("testTopic", new KafkaDeserializationSchema<String>() {
        @Override
        public boolean isEndOfStream(String s) {
            return false;
        }

        @Override
        public String deserialize(ConsumerRecord<byte[], byte[]> consumerRecord) throws Exception {
            return new String(consumerRecord.value());
        }

        @Override
        public TypeInformation<String> getProducedType() {
            return PojoTypeInfo.of(String.class);
        }
    }, properties);
    
    env.addSource(flinkKafkaConsumer).name("kafka data").setParallelism(1).print();
    env.execute();
}
```

### Flink 状态简述

有状态的计算是流处理的关键，稍复杂的流处理场景都需要记录状态，然后在新流入数据的基础上不断更新状态。
下面几个场景都需要使用流处理的状态功能：

- 对重复数据去重，需要记录哪些数据已经流入应用
- 检查输入流是否满足某种需求，需要保存之前流入的数据
- 对一个窗口内的数据进行分析
- 机器学习，根据流入数据不断更新机器学习的参数

## Flink 安装与配置

为运行Flink，本地环境需要安装java。
下载Flink release并解压，链接为1.11.2版本。
https://archive.apache.org/dist/flink/flink-1.11.2/flink-1.11.2-bin-scala_2.11.tgz

进入Flink解压目录下conf文件夹，编辑flink-conf.yaml
提供端口并对外暴露UI界面：
rest.port=8081
rest.address=0.0.0.0

执行Flink解压目录下bin文件夹下start-cluster.sh文件，启动flink服务。

根据需要添加环境变量。

开放防火墙端口，访问http://{ip}:8081，显示如下界面：
![image-20211012100840908](C:\Users\dafeihou\AppData\Roaming\Typora\typora-user-images\image-20211012100840908.png)

## Flink的使用

flink run [options] <jar-file> <arguments>
	常见[options] :
		-c, --class <classname>: 指定jar包中main函数位置，当仅当打包时未指定main可用。参数为需要执行的main函数所在类名。
		-C, --classpath <url>: 在集群中添加链接，保证集群中的节点都可以访问。
		-p, --parallelism <parallelism>: 设置任务并行度，默认1。
		-py, --python <pythonFile>: 针对python脚本。

关于java程序的打包，为了能够通过命令行或web界面执行打包的JAR文件，程序需要通过
`StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();`
获取环境信息。当JAR被提交到命令行或web界面后，该环境为集群环境；如果调用Flink程序的方式，该环境为本地环境。
打包程序只要简单地将所有相关的类导出为 JAR 文件，JAR 文件的 manifest 必须指向包含程序*入口点*（拥有公共 `main` 方法）的类。实现的最简单方法是将 *main-class* 写入 manifest 中（比如 
`main-class:org.apache.flinkexample.MyProgram`）。*main-class* 属性与 Java 虚拟机通过指令 
`java -jar pathToTheJarFile` 执行 JAR 文件时寻找 main 方法的类是相同的。大多数 IDE 提供了在导出 JAR 文件时自动包含该属性的功能。
搜索 JAR 文件 manifest 中的 main-class 或 program-class 属性。如果两个属性同时存在，program-class 属性会优先于 main-class 属性。对于 JAR manifest 中两个属性都不存在的情况，命令行和 web 界面支持手动传入入口点类名参数。

## 简单的Flink Demo与代码讲解

### Flink Demo V1 基础

```java
public class main {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        DataStream<Data> transactions = env
                .addSource(new DataSource())
                .name("addSource");

        DataStream<DataAfter> alerts = transactions
                .keyBy(Data::getDataId)
                .process(new DataProcess())
                .name("process");

        alerts
                .addSink(new DataSink())
                .name("addSink");

        env.execute();
    }
}

public class DataSource implements SourceFunction<Data> {
    private boolean flag = true;

    @Override
    public void run(SourceContext<Data> sourceContext) throws Exception {
        while (flag) {
            int data = (int)(Math.random() * 10);
            sourceContext.collect(new Data(1, data));
            Thread.sleep(1000);
        }
    }

    @Override
    public void cancel() {
        System.out.println("cancel");
        flag = false;
    }
}

public class DataProcess extends ProcessFunction<Data, DataAfter> {
    @Override
    public void processElement(Data data, ProcessFunction<Data, DataAfter>.Context context, Collector<DataAfter> collector) throws Exception {
        DataAfter dataAfter = new DataAfter(data.getDataId(), data.toString());
        collector.collect(dataAfter);
    }
}

public class DataSink implements SinkFunction<DataAfter> {
    @Override
    public void invoke(DataAfter value, Context context) throws Exception {
        System.out.println(value);
    }
}
```

本例输出为：

```json
DataAfter{dataId=1, data='Data{dataId=1, data=7}'}
DataAfter{dataId=1, data='Data{dataId=1, data=6}'}
...
```

StreamExecutionEnvironment 用于设置执行环境。 任务执行环境用于定义任务的属性、创建数据源以及最终启动任务的执行。

数据源可以从外部系统中接收数据，将数据送到Flink程序中，绑定数据源的name属性可以方便调试，快速定位。在本例中，因为使用了keyBy()，所以手动设置了相同的key。

在某些使用场景下，数据需要被划分到多个并发上进行处理，被划分的数据又需要保证某些数据必须被划分到一起，例如相同账户、id等，此时需要使用DataStream的keyBy()方法对数据流进行分区，保证同一个task处理同一个key的数据。

process()方法对流绑定一个操作，这个操作会对流上的每个数据调用定义好的函数，通常，process()会在keyBy()后被调用。注：process(ProcessFunction<T, R> processFunction))是官方文档给出的示例，但是实际上该方法已过期，作为替代可以使用map、flatMap等。

本例中sink仅仅是输出，不做描述。

### Flink Demo V2 附加状态

在V1程序的基础上，增加需求：需要记录上一个数据的某些信息。

```java
public class DataProcessState extends KeyedProcessFunction<Integer, Data, DataAfter> {
    private ValueState<Integer> dataState;

    @Override
    public void processElement(Data data, Context context, Collector<DataAfter> collector) throws Exception {
        int id = data.getDataId();

        Integer lastData = dataState.value();
        if (lastData != null) {
            System.out.println("last is : " + lastData);
        }
        dataState.update(data.getData());

        collector.collect(new DataAfter(id, data.toString()));
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        ValueStateDescriptor<Integer> descriptor = new ValueStateDescriptor<>("data", Integer.class);
        dataState = getRuntimeContext().getState(descriptor);
    }
}
```

本例输出为：

```json
DataAfter{dataId=1, data='Data{dataId=1, data=7}'}
last is : 7
DataAfter{dataId=1, data='Data{dataId=1, data=6}'}
last is : 6
DataAfter{dataId=1, data='Data{dataId=1, data=2}'}
last is : 2
...
```

注意，process(KeyedProcessFunction<KEY, T, R> keyedProcessFunction)方法并没有过期。

在多个事件之间存储信息就需要使用到状态，Flink中最基础的状态类型是ValueState，是一种keyed state，被应用于keyed context提供的操作中，在V1中调用keyBy()后，可以使用该类型。在一个操作中，keyed state的作用域默认是属于所属key的。

ValueState需要使用ValueStateDescriptor创建，ValueStateDescriptor包含了Flink如何管理变量的一些元数据信息。ValueState作用域仅限于当前key。

状态需要使用open()方法来注册。ValueState提供了三个用于交互的方法，update()用于更新状态，value()用于获取状态值，clear()用于清空状态。当一个key还没有状态，例如刚启动或刚调用clear()，value()会返回null。

### Flink Demo V3 附加定时器：

在本例中，KeyedProcessFunction允许设置计时器，该计时器将在将来的某个时间点执行某方法。

在V2的基础上增加需求：当检测到连续的两个数据相差过大(2)时，设置一个一分钟后触发的定时器；定时器触发前，停止记录上一个数据；当定时器触发后，再次开始记录。

```java
public class DataProcessState extends KeyedProcessFunction<Integer, Data, DataAfter> {
    private ValueState<Integer> dataState;
    private ValueState<Boolean> ifState;
    private ValueState<Long> timeState;

    @Override
    public void processElement(Data data, Context context, Collector<DataAfter> collector) throws Exception {
        int id = data.getDataId();
        int thisData = data.getData();

        Integer lastData = dataState.value();
        Boolean b = ifState.value();
        if (lastData != null && (b == null || !b)) {
            if (Math.abs(thisData - lastData) <= 2) {
                System.out.println("last is : " + lastData);
            }
            else {
                ifState.update(true);
                long timer = context.timerService().currentProcessingTime() + 60000L;
                context.timerService().registerProcessingTimeTimer(timer);
                timeState.update(timer);
            }
        }
        dataState.update(thisData);

        collector.collect(new DataAfter(id, data.toString()));
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        dataState = getRuntimeContext().getState(new ValueStateDescriptor<Integer>("id", Integer.class));
        ifState = getRuntimeContext().getState(new ValueStateDescriptor<Boolean>("if", Boolean.class));
        timeState = getRuntimeContext().getState(new ValueStateDescriptor<Long>("timer", Long.class));
    }

    @Override
    public void onTimer(long timestamp, OnTimerContext ctx, Collector<DataAfter> out) throws Exception {
        Long timer = timeState.value();
        ctx.timerService().deleteProcessingTimeTimer(timer);
        timeState.clear();
        ifState.clear();
    }
}
```

本例输出为：

```json
DataAfter{dataId=1, data='Data{dataId=1, data=7}'}
last is : 7
DataAfter{dataId=1, data='Data{dataId=1, data=9}'}
DataAfter{dataId=1, data='Data{dataId=1, data=2}'}
DataAfter{dataId=1, data='Data{dataId=1, data=6}'}
...
DataAfter{dataId=1, data='Data{dataId=1, data=6}'}
last is : 6
...
```

在KeyedProcessFunction中，processElement()方法提供了定时器服务Context来调用。该定时器服务可以查询当前时间、注册定时器和删除定时器。

当定时器触发时，会调用onTimer()方法，通过重写本方法实现一个回调逻辑。

要删除一个定时器，需要记录这个定时器的触发时间，可以通过新建一个时间状态来实现。在本例中，通过时间状态记录定时器被触发的时间，在触发完成后，根据时间状态的值删除定时器。

## Flink execute流程：

第一步，获取job名

```java
	//对于不提供job名的execute方法，flink会提供默认的DEFAULT_JOB_NAME：Flink Streaming Job，通过env中的getJobName方法。
	env.execute();
	
	env.execute({jobName});
```

第二步，获取streamGraph，详细过程见文档
> Flink生成StreamGraph 源码分析

```java
public class StreamExecutionEnvironment {
	...
	public StreamGraph getStreamGraph(String jobName) {
		return getStreamGraph(jobName, true);
	}
	
	//clearTransformations为true代表清空之前注册的transformations
	public StreamGraph getStreamGraph(String jobName, boolean clearTransformations) {
		StreamGraph streamGraph = getStreamGraphGenerator().setJobName(jobName).generate();
		if (clearTransformations) {
			this.transformations.clear();
		}
		return streamGraph;
	}
	...
}
```

## Flink架构介绍：

一个Flink集群总是包含一个JobManager和一个或多个TaskManager。

![image-20211019104722233](C:\Users\dafeihou\AppData\Roaming\Typora\typora-user-images\image-20211019104722233.png)

- JobManager负责job的提交、监控和资源管理。决定何时调度下一个/组task、对完成或失败的task作出反应、协调检查点并从失败中恢复。始终至少有一个JobManager，在HA中也可以配置多个。
  - ResourceManager负责集群中资源提供、回收和分配，负责管理task slot。Flink为不同的环境实现了对应的resourceManager。
  - Dispatcher提供一个rest接口，负责提交Flink程序并执行，每个提交的作业都会启动一个新的JobMaster，还运行WebUI。
  - JobMaster负责管理单个JobGraph的执行，Flink集群中可以同时运行多个作业，每个作业都有自己的JobMaster。
- TaskManager负责运行进程，负责实际的Tasks的运行，必须始终至少一个TaskManager，最小资源调度单位是task slot。task slot的数量表示并发执行的task的数量，一个task slot中可以执行多个算子。
- 同时，需要一个client提交Flink Job。

## 概念介绍：

### 执行模式：

- 批处理是有界数据流处理的典范，在批处理中，可以在计算开始前进行数据的排序、统计等操作后再输出结果。
- 流处理正相反，需要不断地处理到达的数据。

在Flink中，程序由用户自定义算子转换而来的DataFlow组成，这些流式DataFlow形成有向图，以一个或多个source开始，到一个或多个sink结束。

Flink本质上是分布式并行程序，在执行期间，一个流有一个或多个流分区，每个算子有一个或多个子任务。每个子任务彼此独立，并在不同线程中运行，或是不同机器、容器中运行。

在Flink中存在STREAMING和BATCH模式。BATCH模式只能用于有边界的Flink程序，边界是数据源的一个属性，在执行前就直到所有数据源的输入是否已知或者是否会有新的数据出现，对一个job而言，如果所有源都是有边界的，那job就是有界的。举例说明：

```java
public class NumberSequenceSource implements Source<Long, NumberSequenceSource.NumberSequenceSplit, Collection<NumberSequenceSource.NumberSequenceSplit>>, ResultTypeQueryable<Long> {
	...
	public Boundedness getBoundedness() {
        return Boundedness.BOUNDED;
    }
	...
}

public enum Boundedness {
    BOUNDED,
    CONTINUOUS_UNBOUNDED;

    private Boundedness() {
    }
}
```

而对于StreamExecutionEnvironment#addSource()方法，如果不指定执行模式，Flink会自动设置为无边界模式：

```java
public class StreamExecutionEnvironment {
	...
	public <OUT> DataStreamSource<OUT> addSource(SourceFunction<OUT> function) {
        return this.addSource(function, "Custom Source");
    }

    public <OUT> DataStreamSource<OUT> addSource(SourceFunction<OUT> function, String sourceName) {
        return this.addSource(function, sourceName, (TypeInformation)null);
    }

    public <OUT> DataStreamSource<OUT> addSource(SourceFunction<OUT> function, TypeInformation<OUT> typeInfo) {
        return this.addSource(function, "Custom Source", typeInfo);
    }

    public <OUT> DataStreamSource<OUT> addSource(SourceFunction<OUT> function, String sourceName, TypeInformation<OUT> typeInfo) {
        return this.addSource(function, sourceName, typeInfo, Boundedness.CONTINUOUS_UNBOUNDED);
    }

    private <OUT> DataStreamSource<OUT> addSource(SourceFunction<OUT> function, String sourceName, @Nullable TypeInformation<OUT> typeInfo, Boundedness boundedness) {
        Preconditions.checkNotNull(function);
        Preconditions.checkNotNull(sourceName);
        Preconditions.checkNotNull(boundedness);
        TypeInformation<OUT> resolvedTypeInfo = this.getTypeInfo(function, sourceName, SourceFunction.class, typeInfo);
        boolean isParallel = function instanceof ParallelSourceFunction;
        this.clean(function);
        StreamSource<OUT, ?> sourceOperator = new StreamSource(function);
        return new DataStreamSource(this, resolvedTypeInfo, sourceOperator, isParallel, sourceName, boundedness);
    }
	...
}
```

一般来说，STREAMING模式可以用于有边界任务和无边界任务，但是对于有边界任务，BATCH模式会更高效。

执行模式可以通过Flink设置更改(java代码或提交时更改)：

```java

	StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
	env.setRuntimeMode(RuntimeExecutionMode.BATCH);
	
public enum RuntimeExecutionMode {
    STREAMING,
    BATCH,
    AUTOMATIC;

    private RuntimeExecutionMode() {
    }
}
```

```bash
	$ ./bin/flink run -D execution.runtime-mode=BATCH {}.jar
```

- 在STREAMING模式下，所有任务都需要一直在线，网络shuffle是流水线式的，在缓冲(可能)后立马发送给下游。数据不存在处理顺序，先到先处理。
- 而在BATCH下，一个job的任务可以被分为多个阶段执行，在进入下一个阶段前完全处理全部数据，也就是下游开始工作时，上游任务可以下线。数据存在处理顺序：广播输入>常规输入>键控输入。当多个键控输入时，Flink优先处理某个键中的全部记录。

### 算子：

- 常见DataSet算子：
  - fromCollection
  - readTextFile
  - map
  - flatMap
  - filter
  - collect
  - ...
- 常见DataStram算子：
  - addSource
  - map
  - flatMap
  - filter
  - keyBy
  - window
  - addSink
  - ...

算子子任务数就是对应算子的并行度。在同一程序中，不同算子可能有不同并行度。

Flink算子之间可以通过一对一模式或重新分发模式传输数据：

- 一对一模式：保留元素的分区和顺序信息，同一分区的数据只能进入到下游算子的同一分区。
- 重新分发模式：会重新更改数据所在的流分区，当你在程序中选择不同的操作符会将数据发送到不同的目标子任务。例如keyBy通过散列键分区，rebalance()随机重新分发。

### 状态：

Flink中的算子是有状态的，Flink 应用程序的状态访问都在本地进行，因为这有助于其提高吞吐量和降低延迟。通常情况下 Flink 应用程序都是将状态存储在 JVM 堆上，但如果状态太大，我们也可以选择将其以结构化数据格式存储在高速磁盘中(关于状态后端详见容错处理)。一般来说，状态分为keyed state和operator state(非键控状态)和broadcast state。

一般来说operator state应用场景不如键控状态多，绑定到并行操作符实例的状态(例如kafka)，常被用于source和sink算子上，用来保存流入数据的偏移量或对输出数据做缓存。可以使用ListStateDescriptor。

broadcast state能让下游的全部算子实例共享一个流的状态，这个状态用于在所有子任务之间维护相同的状态，是Map格式的。使用的是MapStateDescriptor。

不同的状态类型：

- ValueState<T>：保存一个可以更新和检索的值，每个值都对应当前key
- ListState<T>：保存一个元素的列表，可以追加数据或检索
- ReducingState<T>：保存一个值，表示添加到状态的所有值的集合，内置ReduceFunction进行聚合。
- AggregatingState<IN，OUT>：类似ReducingState，但是保存的值的类型可能不同，内置AggregateFunction进行聚合。
- MapState<UK，UV>：维护一个映射列表。

状态创建时由StateDescriptor创建，根据不同的状态类型，可以有ValueStateDescriptor，ListStateDescriptor等。在RIchFunction下由RuntimeContext读取：

```java
public abstract class StateDescriptor<S extends State, T> implements Serializable {
    ...
    @Nonnull
    //只支持处理时间，从检查点恢复时TTL是否开启必须和之前保持一致，TTL配置并不会保存在检查点
    private StateTtlConfig ttlConfig;
    ...
    //需要先创建StateTtlConfig，指定数据有效期，更新策略，过期还未清理的数据的可见性等
    public void enableTimeToLive(StateTtlConfig ttlConfig) {
        Preconditions.checkNotNull(ttlConfig);
        Preconditions.checkArgument(ttlConfig.getUpdateType() != UpdateType.Disabled && this.queryableStateName == null, "Queryable state is currently not supported with TTL");
        this.ttlConfig = ttlConfig;
    }
    ...
}

public interface RuntimeContext {
    ...
    @PublicEvolving
    <T> ValueState<T> getState(ValueStateDescriptor<T> var1);

    @PublicEvolving
    <T> ListState<T> getListState(ListStateDescriptor<T> var1);

    @PublicEvolving
    <T> ReducingState<T> getReducingState(ReducingStateDescriptor<T> var1);

    @PublicEvolving
    <IN, ACC, OUT> AggregatingState<IN, OUT> getAggregatingState(AggregatingStateDescriptor<IN, ACC, OUT> var1);

    @PublicEvolving
    <UK, UV> MapState<UK, UV> getMapState(MapStateDescriptor<UK, UV> var1);
    ...
}
```

在Flink中，缩放指的是改变算子并行度，Flink目前不支持动态改变并行度，所以需要停止并修改后从保存点修复。在不考虑状态的时候，只需要做好数据流的分配即可，但是在考虑状态的时候，需要从存储中获取状态取回给各个subtask。在引入key group之前，Flink中的key是按照哈希和并行度除余后分配，检查点发生的时候，状态数据顺序写入存储，但是回复的时候却效率低下。因此引入了key group的概念。

key group是keyed state分配的原子单位，且Flink作业中，key group的数量和最大并行度相同，每个subtask会处理一个到多个key group。每个subtask处理的key group以KeyGroupRange表示：

```java
public class KeyGroup {
    private final int keyGroupId;
    private final ThrowingIterator<KeyGroupEntry> keyGroupEntries;

    KeyGroup(int keyGroupId, ThrowingIterator<KeyGroupEntry> keyGroupEntries) {
        this.keyGroupId = keyGroupId;
        this.keyGroupEntries = keyGroupEntries;
    }

    public int getKeyGroupId() {
        return this.keyGroupId;
    }

    public ThrowingIterator<KeyGroupEntry> getKeyGroupEntries() {
        return this.keyGroupEntries;
    }
}

public class KeyGroupEntry {
    private final int kvStateId;
    private final byte[] key;
    private final byte[] value;

    KeyGroupEntry(int kvStateId, byte[] key, byte[] value) {
        this.kvStateId = kvStateId;
        this.key = key;
        this.value = value;
    }

    public int getKvStateId() {
        return this.kvStateId;
    }

    public byte[] getKey() {
        return this.key;
    }

    public byte[] getValue() {
        return this.value;
    }
}

public class KeyGroupRange implements KeyGroupsList, Serializable {
    private static final long serialVersionUID = 4869121477592070607L;
    public static final KeyGroupRange EMPTY_KEY_GROUP_RANGE = new KeyGroupRange();
    private final int startKeyGroup;
    private final int endKeyGroup;

    private KeyGroupRange() {
        this.startKeyGroup = 0;
        this.endKeyGroup = -1;
    }

    public KeyGroupRange(int startKeyGroup, int endKeyGroup) {
        Preconditions.checkArgument(startKeyGroup >= 0);
        Preconditions.checkArgument(startKeyGroup <= endKeyGroup);
        this.startKeyGroup = startKeyGroup;
        this.endKeyGroup = endKeyGroup;
        Preconditions.checkArgument(this.getNumberOfKeyGroups() >= 0, "Potential overflow detected.");
    }

    public boolean contains(int keyGroup) {
        return keyGroup >= this.startKeyGroup && keyGroup <= this.endKeyGroup;
    }

    public KeyGroupRange getIntersection(KeyGroupRange other) {
        int start = Math.max(this.startKeyGroup, other.startKeyGroup);
        int end = Math.min(this.endKeyGroup, other.endKeyGroup);
        return start <= end ? new KeyGroupRange(start, end) : EMPTY_KEY_GROUP_RANGE;
    }

    public int getNumberOfKeyGroups() {
        return 1 + this.endKeyGroup - this.startKeyGroup;
    }

    ...

    public int getKeyGroupId(int idx) {
        if (idx >= 0 && idx <= this.getNumberOfKeyGroups()) {
            return this.startKeyGroup + idx;
        } else {
            throw new IndexOutOfBoundsException("Key group index out of bounds: " + idx);
        }
    }

    public boolean equals(Object o) {
        ...
    }

    public int hashCode() {
        int result = this.startKeyGroup;
        result = 31 * result + this.endKeyGroup;
        return result;
    }

    public String toString() {
        return "KeyGroupRange{startKeyGroup=" + this.startKeyGroup + ", endKeyGroup=" + this.endKeyGroup + '}';
    }

    public Iterator<Integer> iterator() {
        return new KeyGroupRange.KeyGroupIterator();
    }

    public static KeyGroupRange of(int startKeyGroup, int endKeyGroup) {
        return startKeyGroup <= endKeyGroup ? new KeyGroupRange(startKeyGroup, endKeyGroup) : EMPTY_KEY_GROUP_RANGE;
    }

    private final class KeyGroupIterator implements Iterator<Integer> {
        ...
    }
}

public final class KeyGroupRangeAssignment {
    public static final int DEFAULT_LOWER_BOUND_MAX_PARALLELISM = 128;
    public static final int UPPER_BOUND_MAX_PARALLELISM = 32768;

    private KeyGroupRangeAssignment() {
        throw new AssertionError();
    }

    public static int assignKeyToParallelOperator(Object key, int maxParallelism, int parallelism) {
        Preconditions.checkNotNull(key, "Assigned key must not be null!");
        return computeOperatorIndexForKeyGroup(maxParallelism, parallelism, assignToKeyGroup(key, maxParallelism));
    }

    //对键值进行双重哈希，确定每个key应该被分入哪个key group
    public static int assignToKeyGroup(Object key, int maxParallelism) {
        Preconditions.checkNotNull(key, "Assigned key must not be null!");
        return computeKeyGroupForKeyHash(key.hashCode(), maxParallelism);
    }

    //双重哈希的第二次哈希实现
    public static int computeKeyGroupForKeyHash(int keyHash, int maxParallelism) {
        return MathUtils.murmurHash(keyHash) % maxParallelism;
    }

    //根据单独算子并行度和最大并行度和算子序号计算当前算子被分配的key group
    public static KeyGroupRange computeKeyGroupRangeForOperatorIndex(int maxParallelism, int parallelism, int operatorIndex) {
        checkParallelismPreconditions(parallelism);
        checkParallelismPreconditions(maxParallelism);
        Preconditions.checkArgument(maxParallelism >= parallelism, "Maximum parallelism must not be smaller than parallelism.");
        int start = (operatorIndex * maxParallelism + parallelism - 1) / parallelism;
        int end = ((operatorIndex + 1) * maxParallelism - 1) / parallelism;
        return new KeyGroupRange(start, end);
    }

    public static int computeOperatorIndexForKeyGroup(int maxParallelism, int parallelism, int keyGroupId) {
        return keyGroupId * parallelism / maxParallelism;
    }

    public static int computeDefaultMaxParallelism(int operatorParallelism) {
        checkParallelismPreconditions(operatorParallelism);
        return Math.min(Math.max(MathUtils.roundUpToPowerOfTwo(operatorParallelism + operatorParallelism / 2), 128), 32768);
    }

    //Flink支持的最大最小并行度
    public static void checkParallelismPreconditions(int parallelism) {
        Preconditions.checkArgument(parallelism > 0 && parallelism <= 32768, "Operator parallelism not within bounds: " + parallelism);
    }
}
```

看完了Keyed State，还有一种状态是operator state，每个算子子任务共享一个状态，所有进入该算子实例的数据都可以访问和更新这个状态。operator state对缩放的应对方式：

```java
public interface CheckpointedFunction {
    //执行检查点时，如何写入存储
    void snapshotState(FunctionSnapshotContext var1) throws Exception;

    //从检查点恢复时或Flink作业第一次执行时，向本地状态填充数据(第一次执行则为默认值)
    void initializeState(FunctionInitializationContext var1) throws Exception;
}

public class main {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.enableCheckpointing(5000L);
        env.addSource(new DataSource()).print();
        env.execute();
    }
}

public class DataSource implements CheckpointedFunction, SourceFunction<Integer> {
    @Override
    public void snapshotState(FunctionSnapshotContext functionSnapshotContext) throws Exception {
        System.out.println("snap");
    }

    @Override
    public void initializeState(FunctionInitializationContext functionInitializationContext) throws Exception {
        System.out.println("init source");
    }

    @Override
    public void run(SourceContext sourceContext) throws Exception {
        while (true) {
            sourceContext.collect(1);
            Thread.sleep(1000);
        }
    }

    @Override
    public void cancel() {

    }
}
```

本例输出为：

```json
init source
2> 1
1> 1
2> 1
1> 1
snap
2> 1
1> 1
2> 1
1> 1
2> 1
snap
...
```

### 快照：

Flink能够提供高容错的、精确到一次计算的语义。快照会将数据源中消费记录的偏移量记录下来，并将算子获取到该数据(记录的偏移量对应的数据)的状态。当发生故障时，Flink会恢复上次存储的状态，重置数据源从状态中记录的上次消费的偏移量开始重新消费出来。快照在执行时会异步获取状态并存储，并不会阻塞正在进行的数据处理逻辑。

### 执行环境：

每个Flink应用都需要有执行环境，即

```java
    StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
```

DataStream API会将应用构建为一个job graph，并附加到StreamExecutionEnvironment。当调用execute()方法时，此graph就被打包并发送到JobManager上，后者对作业并行处理并将子任务分发给Task Manager来执行。每个作业的并行子任务将在task slot中执行。如果没有执行execute()，应用就不会被执行。

执行环境提供了job执行的方法，例如并行度、检查点参数和与外部的数据交换。StreamExecutionEnvironment是执行程序的上下文， LocalStreamEnvironment 将导致在当前 JVM 中执行，RemoteStreamEnvironment 将导致在远程设置上执行。

```java
public class StreamExecutionEnvironment {
    ...

    public StreamExecutionEnvironment() {
        this(new Configuration());
    }

    @PublicEvolving
    public StreamExecutionEnvironment(Configuration configuration) {
        this(configuration, (ClassLoader)null);
    }

    @PublicEvolving
    public StreamExecutionEnvironment(Configuration configuration, ClassLoader userClassloader) {
        this(new DefaultExecutorServiceLoader(), configuration, userClassloader);
    }

    @PublicEvolving
    public StreamExecutionEnvironment(PipelineExecutorServiceLoader executorServiceLoader, Configuration configuration, ClassLoader userClassloader) {
        ...
    }

    protected Configuration getConfiguration() {
        return this.configuration;
    }

    protected ClassLoader getUserClassloader() {
        return this.userClassloader;
    }

    public ExecutionConfig getConfig() {
        return this.config;
    }

    public List<Tuple2<String, DistributedCacheEntry>> getCachedFiles() {
    	//记录文件位置，分为注册后和上传后
        return this.cacheFile;
    }

    @PublicEvolving
    public List<JobListener> getJobListeners() {
    	//由registerJobListener注册，用于通知特定作业状态更改的监听器
        return this.jobListeners;
    }

    public StreamExecutionEnvironment setParallelism(int parallelism) {
        this.config.setParallelism(parallelism);
        return this;
    }

    @PublicEvolving
    public StreamExecutionEnvironment setRuntimeMode(RuntimeExecutionMode executionMode) {
    	//设置运行模式，三种：AUTOMATIC(当有无界流时切换为streaming)/BATCH(批处理)/STREAMING(启动检查点、支持处理时间和时间时间)
    	//官方建议是在提交程序时使用execution.runtime-mode指定模式
        ...
    }

    public StreamExecutionEnvironment setMaxParallelism(int maxParallelism) {
    	//指定最大并行度，动态缩放的上限(还是需要停止job重启)
        ...
    }

    public int getParallelism() {
        return this.config.getParallelism();
    }

    public int getMaxParallelism() {
        return this.config.getMaxParallelism();
    }

    public StreamExecutionEnvironment setBufferTimeout(long timeoutMillis) {
    	//设置刷新输出缓冲器的最大时间频率
    	//特殊输入：0-每条记录后都刷新；-1-仅在缓冲区已满时刷新
        ...
    }

    public long getBufferTimeout() {
        return this.bufferTimeout;
    }

    @PublicEvolving
    public StreamExecutionEnvironment disableOperatorChaining() {
        this.isChainingEnabled = false;
        return this;
    }

    @PublicEvolving
    public boolean isChainingEnabled() {
        return this.isChainingEnabled;
    }

    public CheckpointConfig getCheckpointConfig() {
        return this.checkpointCfg;
    }

    public StreamExecutionEnvironment enableCheckpointing(long interval) {
        this.checkpointCfg.setCheckpointInterval(interval);
        return this;
    }

    public StreamExecutionEnvironment enableCheckpointing(long interval, CheckpointingMode mode) {
        this.checkpointCfg.setCheckpointingMode(mode);
        this.checkpointCfg.setCheckpointInterval(interval);
        return this;
    }

    ...

    public long getCheckpointInterval() {
        return this.checkpointCfg.getCheckpointInterval();
    }

    ...

    @PublicEvolving
    public boolean isUnalignedCheckpointsEnabled() {
        return this.checkpointCfg.isUnalignedCheckpointsEnabled();
    }

    @PublicEvolving
    public boolean isForceUnalignedCheckpoints() {
        return this.checkpointCfg.isForceUnalignedCheckpoints();
    }

    public CheckpointingMode getCheckpointingMode() {
        return this.checkpointCfg.getCheckpointingMode();
    }

    @PublicEvolving
    public StreamExecutionEnvironment setStateBackend(StateBackend backend) {
        this.defaultStateBackend = (StateBackend)Preconditions.checkNotNull(backend);
        return this;
    }

    @PublicEvolving
    public StateBackend getStateBackend() {
        return this.defaultStateBackend;
    }

    @PublicEvolving
    public StreamExecutionEnvironment setDefaultSavepointDirectory(String savepointDirectory) {
        Preconditions.checkNotNull(savepointDirectory);
        return this.setDefaultSavepointDirectory(new Path(savepointDirectory));
    }

    @PublicEvolving
    public StreamExecutionEnvironment setDefaultSavepointDirectory(URI savepointDirectory) {
        Preconditions.checkNotNull(savepointDirectory);
        return this.setDefaultSavepointDirectory(new Path(savepointDirectory));
    }

    @PublicEvolving
    public StreamExecutionEnvironment setDefaultSavepointDirectory(Path savepointDirectory) {
        this.defaultSavepointDirectory = (Path)Preconditions.checkNotNull(savepointDirectory);
        return this;
    }

    @Nullable
    @PublicEvolving
    public Path getDefaultSavepointDirectory() {
        return this.defaultSavepointDirectory;
    }

    @PublicEvolving
    public void setRestartStrategy(RestartStrategyConfiguration restartStrategyConfiguration) {
        this.config.setRestartStrategy(restartStrategyConfiguration);
    }

    @PublicEvolving
    public RestartStrategyConfiguration getRestartStrategy() {
        return this.config.getRestartStrategy();
    }

    ...

    public <T extends Serializer<?> & Serializable> void addDefaultKryoSerializer(Class<?> type, T serializer) {
        this.config.addDefaultKryoSerializer(type, serializer);
    }

    public void addDefaultKryoSerializer(Class<?> type, Class<? extends Serializer<?>> serializerClass) {
        this.config.addDefaultKryoSerializer(type, serializerClass);
    }

    public <T extends Serializer<?> & Serializable> void registerTypeWithKryoSerializer(Class<?> type, T serializer) {
        this.config.registerTypeWithKryoSerializer(type, serializer);
    }

    public void registerTypeWithKryoSerializer(Class<?> type, Class<? extends Serializer> serializerClass) {
        this.config.registerTypeWithKryoSerializer(type, serializerClass);
    }

    public void registerType(Class<?> type) {
        ...
    }

   ...

    @PublicEvolving
    public void configure(ReadableConfig configuration, ClassLoader classLoader) {
    	//更改部分设置
		...
    }

    ...

    public DataStreamSource<Long> fromSequence(long from, long to) {
    	//创建一个只包含数字long的新数据流
        ...
    }

    @SafeVarargs
    public final <OUT> DataStreamSource<OUT> fromElements(OUT... data) {
    	//从给定的元素中创建新的数据流，类型必须一致
        ...
    }

    @SafeVarargs
    public final <OUT> DataStreamSource<OUT> fromElements(Class<OUT> type, OUT... data) {
        ...
    }

    public <OUT> DataStreamSource<OUT> fromCollection(Collection<OUT> data) {
    	//从给定非空集合创建数据流
        ...
    }

    public <OUT> DataStreamSource<OUT> fromCollection(Collection<OUT> data, TypeInformation<OUT> typeInfo) {
        ...
    }

    public <OUT> DataStreamSource<OUT> fromCollection(Iterator<OUT> data, Class<OUT> type) {
        return this.fromCollection(data, TypeExtractor.getForClass(type));
    }

    public <OUT> DataStreamSource<OUT> fromCollection(Iterator<OUT> data, TypeInformation<OUT> typeInfo) {
        ...
    }

    public <OUT> DataStreamSource<OUT> fromParallelCollection(SplittableIterator<OUT> iterator, Class<OUT> type) {
    	//从一个可拆分的迭代器中创建数据源
    	//通过spilt拆分为迭代器数组
        return this.fromParallelCollection(iterator, TypeExtractor.getForClass(type));
    }

    public <OUT> DataStreamSource<OUT> fromParallelCollection(SplittableIterator<OUT> iterator, TypeInformation<OUT> typeInfo) {
        return this.fromParallelCollection(iterator, typeInfo, "Parallel Collection Source");
    }

    private <OUT> DataStreamSource<OUT> fromParallelCollection(SplittableIterator<OUT> iterator, TypeInformation<OUT> typeInfo, String operatorName) {
        ...
    }

    public DataStreamSource<String> readTextFile(String filePath) {
        return this.readTextFile(filePath, "UTF-8");
    }
    
    //逐行读取文件作为字符串返回
    public DataStreamSource<String> readTextFile(String filePath, String charsetName) {
        ...
    }

    public <OUT> DataStreamSource<OUT> readFile(FileInputFormat<OUT> inputFormat, String filePath) {
    	//设置只读一次，而非定期读取
        return this.readFile(inputFormat, filePath, FileProcessingMode.PROCESS_ONCE, -1L);
    }

    ...

    @PublicEvolving
    public <OUT> DataStreamSource<OUT> readFile(FileInputFormat<OUT> inputFormat, String filePath, FileProcessingMode watchType, long interval) {
        ...
    }

    ...

    @PublicEvolving
    //基于给定的输入类型，读取文件，根据watchtype的不同决定是定期读取或只读一次
    public <OUT> DataStreamSource<OUT> readFile(FileInputFormat<OUT> inputFormat, String filePath, FileProcessingMode watchType, long interval, TypeInformation<OUT> typeInformation) {
        //Flink读取文件被分成两个任务，目录监控和数据读取。
        //监控由一个并行度=1的任务实现，读取由多个并行运行的任务执行，并行度和job的并行度相同
        //监控由ContinuousFileMonitoringFunction<OUT>类提供实现
        //读取由ContinuousFileReaderOperatorFactory类提供实现
        //需要实现InputFormat#nextRecord()和reachedEnd()方法
        ...
    }

    ...

    @PublicEvolving
    public DataStreamSource<String> socketTextStream(String hostname, int port, String delimiter, long maxRetry) {
    	//delimiter代表将接受的数据拆分，maxRetry代表最大重试间隔，0为立即终止，-1为永远重试
        return this.addSource(new SocketTextStreamFunction(hostname, port, delimiter, maxRetry), (String)"Socket Stream");
    }

    ...

    @PublicEvolving
    public DataStreamSource<String> socketTextStream(String hostname, int port, String delimiter) {
        return this.socketTextStream(hostname, port, delimiter, 0L);
    }

    @PublicEvolving
    public DataStreamSource<String> socketTextStream(String hostname, int port) {
        return this.socketTextStream(hostname, port, "\n");
    }

    @PublicEvolving
    public <OUT> DataStreamSource<OUT> createInput(InputFormat<OUT, ?> inputFormat) {
    	//创建通用输入
        return this.createInput(inputFormat, TypeExtractor.getInputFormatTypes(inputFormat));
    }

    @PublicEvolving
    public <OUT> DataStreamSource<OUT> createInput(InputFormat<OUT, ?> inputFormat, TypeInformation<OUT> typeInfo) {
        ...
    }

    private <OUT> DataStreamSource<OUT> createInput(InputFormat<OUT, ?> inputFormat, TypeInformation<OUT> typeInfo, String sourceName) {
        ...
    }

    private <OUT> DataStreamSource<OUT> createFileInput(FileInputFormat<OUT> inputFormat, TypeInformation<OUT> typeInfo, String sourceName, FileProcessingMode monitoringMode, long interval) {
        ...
    }

    public <OUT> DataStreamSource<OUT> addSource(SourceFunction<OUT> function) {
        return this.addSource(function, "Custom Source");
    }

    public <OUT> DataStreamSource<OUT> addSource(SourceFunction<OUT> function, String sourceName) {
        return this.addSource(function, sourceName, (TypeInformation)null);
    }

    public <OUT> DataStreamSource<OUT> addSource(SourceFunction<OUT> function, TypeInformation<OUT> typeInfo) {
        return this.addSource(function, "Custom Source", typeInfo);
    }

    public <OUT> DataStreamSource<OUT> addSource(SourceFunction<OUT> function, String sourceName, TypeInformation<OUT> typeInfo) {
        return this.addSource(function, sourceName, typeInfo, Boundedness.CONTINUOUS_UNBOUNDED);
    }

    private <OUT> DataStreamSource<OUT> addSource(SourceFunction<OUT> function, String sourceName, @Nullable TypeInformation<OUT> typeInfo, Boundedness boundedness) {
        ...
    }

    @Experimental
    public <OUT> DataStreamSource<OUT> fromSource(Source<OUT, ?, ?> source, WatermarkStrategy<OUT> timestampsAndWatermarks, String sourceName) {
        return this.fromSource(source, timestampsAndWatermarks, sourceName, (TypeInformation)null);
    }

    @Experimental
    public <OUT> DataStreamSource<OUT> fromSource(Source<OUT, ?, ?> source, WatermarkStrategy<OUT> timestampsAndWatermarks, String sourceName, TypeInformation<OUT> typeInfo) {
        //Source三个参数分别为来源产生的数据类型、处理时拆分的数据类型、分片枚举器的检查点类型
        ...
    }

    public JobExecutionResult execute() throws Exception {
        return this.execute(this.getJobName());
    }

    public JobExecutionResult execute(String jobName) throws Exception {
        Preconditions.checkNotNull(jobName, "Streaming Job name should not be null.");
        //通过StreamGraphGenerator，调用generate，生成StreamGraph
        //生成StreamGraph在另外文件中介绍
        return this.execute(this.getStreamGraph(jobName));
    }

    @Internal
    public JobExecutionResult execute(StreamGraph streamGraph) throws Exception {
    	//将job graph传入executeAsync
        JobClient jobClient = this.executeAsync(streamGraph);

        try {
            Object jobExecutionResult;
            //判断提交模式是附加模式还是分离模式
            if (this.configuration.getBoolean(DeploymentOptions.ATTACHED)) {
                jobExecutionResult = (JobExecutionResult)jobClient.getJobExecutionResult().get();
            } else {
                jobExecutionResult = new DetachedJobExecutionResult(jobClient.getJobID());
            }

            this.jobListeners.forEach((jobListener) -> {
                //当job成功执行，监听触发
                jobListener.onJobExecuted(jobExecutionResult, (Throwable)null);
            });
            return (JobExecutionResult)jobExecutionResult;
        } catch (Throwable var5) {
            Throwable strippedException = ExceptionUtils.stripExecutionException(var5);
            this.jobListeners.forEach((jobListener) -> {
                //当job执行失败，监听c
                jobListener.onJobExecuted((JobExecutionResult)null, strippedException);
            });
            ExceptionUtils.rethrowException(strippedException);
            return null;
        }
    }

    @PublicEvolving
    public void registerJobListener(JobListener jobListener) {
        //注册监听器
        Preconditions.checkNotNull(jobListener, "JobListener cannot be null");
        this.jobListeners.add(jobListener);
    }

    @PublicEvolving
    public void clearJobListeners() {
        this.jobListeners.clear();
    }

    @PublicEvolving
    public final JobClient executeAsync() throws Exception {
        return this.executeAsync(this.getJobName());
    }

    @PublicEvolving
    public JobClient executeAsync(String jobName) throws Exception {
        return this.executeAsync(this.getStreamGraph((String)Preconditions.checkNotNull(jobName)));
    }

    @Internal
    public JobClient executeAsync(StreamGraph streamGraph) throws Exception {
        Preconditions.checkNotNull(streamGraph, "StreamGraph cannot be null.");
        Preconditions.checkNotNull(this.configuration.get(DeploymentOptions.TARGET), "No execution.target specified in your configuration file.");
        //生成一个负责执行流水线的实体即用户job的工厂
        PipelineExecutorFactory executorFactory = this.executorServiceLoader.getExecutorFactory(this.configuration);
        Preconditions.checkNotNull(executorFactory, "Cannot find compatible factory for specified execution.target (=%s)", new Object[]{this.configuration.get(DeploymentOptions.TARGET)});
        //根据配置提供一个JobClient，允许与正在执行的job进行交互
        CompletableFuture jobClientFuture = executorFactory.getExecutor(this.configuration).execute(streamGraph, this.configuration, this.userClassloader);

        try {
            JobClient jobClient = (JobClient)jobClientFuture.get();
            this.jobListeners.forEach((jobListener) -> {
                //表示job提交成功
                jobListener.onJobSubmitted(jobClient, (Throwable)null);
            });
            return jobClient;
        } catch (ExecutionException var6) {
            Throwable strippedException = ExceptionUtils.stripExecutionException(var6);
            this.jobListeners.forEach((jobListener) -> {
                //表示job提交失败
                jobListener.onJobSubmitted((JobClient)null, strippedException);
            });
            throw new FlinkException(String.format("Failed to execute job '%s'.", streamGraph.getJobName()), strippedException);
        }
    }

    @Internal
    public StreamGraph getStreamGraph() {
        return this.getStreamGraph(this.getJobName());
    }

    @Internal
    public StreamGraph getStreamGraph(String jobName) {
        return this.getStreamGraph(jobName, true);
    }

    @Internal
    public StreamGraph getStreamGraph(String jobName, boolean clearTransformations) {
        //通过StreamGraphGenerator生成streamGraph，在另外的文件中介绍
        StreamGraph streamGraph = this.getStreamGraphGenerator().setJobName(jobName).generate();
        if (clearTransformations) {
            this.transformations.clear();
        }

        return streamGraph;
    }

    private StreamGraphGenerator getStreamGraphGenerator() {
        if (this.transformations.size() <= 0) {
            throw new IllegalStateException("No operators defined in streaming topology. Cannot execute.");
        } else {
            RuntimeExecutionMode executionMode = (RuntimeExecutionMode)this.configuration.get(ExecutionOptions.RUNTIME_MODE);
            return (new StreamGraphGenerator(this.transformations, this.config, this.checkpointCfg, this.getConfiguration()))
                .setRuntimeExecutionMode(executionMode)
                .setStateBackend(this.defaultStateBackend)
                .setSavepointDir(this.defaultSavepointDirectory)
                .setChaining(this.isChainingEnabled)
                .setUserArtifacts(this.cacheFile)
                .setTimeCharacteristic(this.timeCharacteristic)
                .setDefaultBufferTimeout(this.bufferTimeout);
        }
    }

    public String getExecutionPlan() {
        return this.getStreamGraph(this.getJobName(), false).getStreamingPlanAsJSON();
    }

    @Internal
    public <F> F clean(F f) {
        if (this.getConfig().isClosureCleanerEnabled()) {
            ClosureCleaner.clean(f, this.getConfig().getClosureCleanerLevel(), true);
        }

        ClosureCleaner.ensureSerializable(f);
        return f;
    }

    @Internal
    public void addOperator(Transformation<?> transformation) {
        Preconditions.checkNotNull(transformation, "transformation must not be null.");
        this.transformations.add(transformation);
    }

    public static StreamExecutionEnvironment getExecutionEnvironment() {
        return getExecutionEnvironment(new Configuration());
    }

    public static StreamExecutionEnvironment getExecutionEnvironment(Configuration configuration) {
        return (StreamExecutionEnvironment)Utils.resolveFactory(threadLocalContextEnvironmentFactory, contextEnvironmentFactory).map((factory) -> {
            return factory.createExecutionEnvironment(configuration);
        }).orElseGet(() -> {
            return createLocalEnvironment(configuration);
        });
    }

    public static LocalStreamEnvironment createLocalEnvironment() {
        return createLocalEnvironment(defaultLocalParallelism);
    }

    public static LocalStreamEnvironment createLocalEnvironment(int parallelism) {
        return createLocalEnvironment(parallelism, new Configuration());
    }

    public static LocalStreamEnvironment createLocalEnvironment(int parallelism, Configuration configuration) {
        Configuration copyOfConfiguration = new Configuration();
        copyOfConfiguration.addAll(configuration);
        copyOfConfiguration.set(CoreOptions.DEFAULT_PARALLELISM, parallelism);
        return createLocalEnvironment(copyOfConfiguration);
    }

    public static LocalStreamEnvironment createLocalEnvironment(Configuration configuration) {
        if (configuration.getOptional(CoreOptions.DEFAULT_PARALLELISM).isPresent()) {
            return new LocalStreamEnvironment(configuration);
        } else {
            Configuration copyOfConfiguration = new Configuration();
            copyOfConfiguration.addAll(configuration);
            copyOfConfiguration.set(CoreOptions.DEFAULT_PARALLELISM, defaultLocalParallelism);
            return new LocalStreamEnvironment(copyOfConfiguration);
        }
    }

    @PublicEvolving
    public static StreamExecutionEnvironment createLocalEnvironmentWithWebUI(Configuration conf) {
        Preconditions.checkNotNull(conf, "conf");
        if (!conf.contains(RestOptions.PORT)) {
            conf.setInteger(RestOptions.PORT, (Integer)RestOptions.PORT.defaultValue());
        }

        return createLocalEnvironment(conf);
    }

    public static StreamExecutionEnvironment createRemoteEnvironment(String host, int port, String... jarFiles) {
        return new RemoteStreamEnvironment(host, port, jarFiles);
    }

    public static StreamExecutionEnvironment createRemoteEnvironment(String host, int port, int parallelism, String... jarFiles) {
        RemoteStreamEnvironment env = new RemoteStreamEnvironment(host, port, jarFiles);
        env.setParallelism(parallelism);
        return env;
    }

    public static StreamExecutionEnvironment createRemoteEnvironment(String host, int port, Configuration clientConfig, String... jarFiles) {
        return new RemoteStreamEnvironment(host, port, clientConfig, jarFiles);
    }

    @PublicEvolving
    public static int getDefaultLocalParallelism() {
        return defaultLocalParallelism;
    }

    @PublicEvolving
    public static void setDefaultLocalParallelism(int parallelism) {
        defaultLocalParallelism = parallelism;
    }

    protected static void initializeContextEnvironment(StreamExecutionEnvironmentFactory ctx) {
        contextEnvironmentFactory = ctx;
        threadLocalContextEnvironmentFactory.set(contextEnvironmentFactory);
    }

    protected static void resetContextEnvironment() {
        contextEnvironmentFactory = null;
        threadLocalContextEnvironmentFactory.remove();
    }

    public void registerCachedFile(String filePath, String name) {
        this.registerCachedFile(filePath, name, false);
    }

    public void registerCachedFile(String filePath, String name, boolean executable) {
        this.cacheFile.add(new Tuple2(name, new DistributedCacheEntry(filePath, executable)));
    }

    private <OUT, T extends TypeInformation<OUT>> T getTypeInfo(Object source, String sourceName, Class<?> baseSourceClass, TypeInformation<OUT> typeInfo) {
        TypeInformation<OUT> resolvedTypeInfo = typeInfo;
        if (typeInfo == null && source instanceof ResultTypeQueryable) {
            resolvedTypeInfo = ((ResultTypeQueryable)source).getProducedType();
        }

        if (resolvedTypeInfo == null) {
            try {
                resolvedTypeInfo = TypeExtractor.createTypeInfo(baseSourceClass, source.getClass(), 0, (TypeInformation)null, (TypeInformation)null);
            } catch (InvalidTypesException var7) {
                resolvedTypeInfo = new MissingTypeInfo(sourceName, var7);
            }
        }

        return (TypeInformation)resolvedTypeInfo;
    }

    private String getJobName() {
        return this.configuration.getString(PipelineOptions.NAME, "Flink Streaming Job");
    }

    static {
        DEFAULT_TIME_CHARACTERISTIC = TimeCharacteristic.EventTime;
        contextEnvironmentFactory = null;
        threadLocalContextEnvironmentFactory = new ThreadLocal();
        defaultLocalParallelism = Runtime.getRuntime().availableProcessors();
    }
}

```

通常使用getExecutionEnvironment()即可，该方法做正确的处理。LocalEnvironment将导致在当前JVM中运行，RemoteEnvironment将导致在远程上执行。

注意，在main方法执行时，只触发算子被创建和形成有向图的过程。当execute方法被触发时，才会真正执行算子。

### 时间：

- 事件时间 event time：事件产生的时间，记录的是设备生产或存储事件的时间
- 摄取时间 ingestion time：Flink读取事件时记录的时间
- 处理时间 process time：Flink pipeline中具体算子处理事件的时间

![image-20211012102035599](C:\Users\dafeihou\AppData\Roaming\Typora\typora-user-images\image-20211012102035599.png)

在之前的Flink版本中，可以通过StreamExecutionEnvironment.setStreamTimeCharacteristic()设置时间类别。从Flink1.12开始，默认的流时间特性默认为事件时间，修改时间类别的函数开始过期。摘自官网文档：

> In Flink 1.12 the default stream time characteristic has been changed to [`EventTime`](https://nightlies.apache.org/flink/flink-docs-release-1.14/api/java/org/apache/flink/streaming/api/TimeCharacteristic.html#EventTime), thus you don't need to call this method for enabling event-time support anymore. Explicitly using processing-time windows and timers works in event-time mode. If you need to disable watermarks, please use [`ExecutionConfig.setAutoWatermarkInterval(long)`](https://nightlies.apache.org/flink/flink-docs-release-1.14/api/java/org/apache/flink/api/common/ExecutionConfig.html#setAutoWatermarkInterval-long-). If you are using [`IngestionTime`](https://nightlies.apache.org/flink/flink-docs-release-1.14/api/java/org/apache/flink/streaming/api/TimeCharacteristic.html#IngestionTime), please manually set an appropriate [`WatermarkStrategy`](https://nightlies.apache.org/flink/flink-docs-release-1.14/api/java/org/apache/flink/api/common/eventtime/WatermarkStrategy.html). If you are using generic "time window" operations (for example [`KeyedStream.timeWindow(org.apache.flink.streaming.api.windowing.time.Time)`](https://nightlies.apache.org/flink/flink-docs-release-1.14/api/java/org/apache/flink/streaming/api/datastream/KeyedStream.html#timeWindow-org.apache.flink.streaming.api.windowing.time.Time-) that change behaviour based on the time characteristic, please use equivalent operations that explicitly specify processing time or event time.
>
> 在Flink1.12中，默认的流时间特性已更改为事件时间，因此您不再需要调用StreamExecutionEnvironment.setStreamTimeCharacteristic()来启用事件时间支持。显式使用处理时间窗口和计时器再事件时间模式下工作。如果您需要禁用水印，使用ExecutionConfig.setAutoWatermarkInterval(long)。如果使用摄取时间，请手动设置合适的[`WatermarkStrategy`](https://nightlies.apache.org/flink/flink-docs-release-1.14/api/java/org/apache/flink/api/common/eventtime/WatermarkStrategy.html)，如果在使用通用的时间窗口操作，请使用等效操作明确指定处理时间或者事件时间。

如果想使用事件时间，需要额外给Flink提供一个时间戳提取器和水印生成器，Flink将使用他们追踪事件时间的进度。

### 水印：

事件时间指的是数据流中每个元素或者或者事件自带的时间属性，一般是事件发生的事件。由于事件从发生到进入Flink算子之间有很多环节，一个较早发生的事件因为各种延迟可能较晚到达，因此使用事件时间可能会造成事件到达乱序。因此，只有事件时间需要使用水印。

一般来说，在使用事件时间的时候，Flink接收到的事件流是乱序的，因此，在接收到某些事件时，不能直接作为已排序的流释放，可能还有较早的事件尚未到达，但是无休止的等待是不行的，因此，需要一种策略，该策略定义在给定时间戳的事件中Flink何时停止等待较早事件的归来。即水印的作用，定义何时停止等待较早的事件。

Flink中事件时间的处理取决于水印生成器，该生成器将带有时间戳的特殊元素插入流中形成水印。事件时间t的水印代表t之前的事件都(很可能)已到达。当source关闭的时候，会发出带有时间戳Long.MAX_VALUE的最终水印，将来不会有输入到达。

当拥有多条并行流的时候，源的每个并行实例都会生成独立的水印。当水印流经算子，会更新算子的事件时间，算子会根据事件时间为下游生成一个新的水印，若一个算子消费多个输入流，这些算子的事件时间就是所有输入流提供的最小值。

当数据源的某个分区/分片在一段时间内未发送事件时间数据，则不会生成新的水印，但是由于下游算子会根据最小值提供水印，可能会导致问题，所以需要检测空闲输入。

- 一个时间戳为t的水印假定后续到达的事件时间戳都大于t
- 假如Flink算子接收到一个迟到的数据，提供了一些机制来处理迟到的数据
- 水印必须单调递增
- 水印设置的宽松，准确度得到提升，但是应用延迟变高，计算所需时间拉长；水印设置紧凑，会产生不少迟到数据，影响计算结果的准确度。

#### 简单的水印使用：

```java
    WatermarkStrategy<Data> strategy = WatermarkStrategy
            .<Data>forBoundedOutOfOrderness(Duration.ofSeconds(20))
            .withTimestampAssigner((data, time) -> data.getTimestamp());

    DataStream<Data> waterMask = transactions.assignTimestampsAndWatermarks(strategy);
```

WatermarkStrategy<T>接口，定义了如何在源中生成水印，是生成水印和分配记录内部时间戳的构建器，常见方法如下：

- createWatermarkGenerator，基于当前策略创建一个水印生成器。该生成器会基于事件或定期生成水印

- createTimestampAssigner，生成一个时间戳分配器，为元素事件分配事件时间戳

- withTimestampAssigner，创建一个WaterStrategy，使用给定的timestampAssigner

- forMonotonousTimestamps，当时间戳单调递增时，创建水印的策略。相当于forBoundedOutOfOrderness(Duration.ofMillis(0L))。

- forBoundedOutOfOrderness，当时间戳为乱序的情况下创建策略，但是可以根据参数设置上限。当参数为B的时候，一旦遇上时间戳为T的事件，就不会再遇见早于(T - B)的事件。水印是周期生成的，这种策略引入的延迟是周期间隔长度加上参数B。

- noWatermarks，无水印

- withIdleness，检测空闲输入并标记为空闲状态，参数为检测时间

除了在WatermarkStrategy中设置timestampAssigner外，还可以在source阶段给每个数据设置时间戳，注意，如果后续还有上述操作，会覆盖source阶段的设置。

```java
    @Override
    public void run(SourceContext<Data> sourceContext) throws Exception {
        while (flag) {
            int data = (int)(Math.random() * 10);
            Data data1 = new Data(1, data);
            data1.setTimestamp(System.currentTimeMillis());
            sourceContext.collectWithTimestamp(data1, data1.getTimestamp());
            Thread.sleep(1000);
            sourceContext.emitWatermark(new Watermark(System.currentTimeMillis()));
        }
    }
```

在source中，将collect换成collectWithTimestamp，就可以从源发出一个附加给定时间轴的元素。同时emitWatermark方法会发出一个给定的水印。

创建水印构建器WatermarkStrategy之后，需要在DataStream中添加水印。DataStream<T>类中，提供了assignTimestampsAndWatermarks(WatermarkStrategy<T>)方法，为数据流中的元素事件分配时间戳并生成水印，用给定的WatermarkStrategy创建TimestampAssigner和WatermarkGenerator。对于数据流中的全部数据，都会调用

```java
    long extractTimestamp(T var1, long var2);
    void onEvent(T var1, long var2, WatermarkOutput var4);
```

前者为每个元素调用，分配时间戳；后者为每个元素调用，允许水印生成器检查并记住事件时间戳或根据事件本身发出水印(标记生成器)。同时，向DataStream中添加构建器后，会定期调用

```java
    void onPeriodicEmit(WatermarkOutput var1);
```

该函数会被定期调用(周期生成器)，默认间隔为200ms，而且可能会发出或不发出新的水印。该方法调用的间隔取决于

```java
    env.getConfig().getAutoWatermarkInterval();
```

注意，如果输入setAutoWatermarkInterval(0)，则禁止发射水印。

以下为BoundedOutOfOrdernessWatermarks<T>的源码，即上文使用的水印生成器：

```java
public class BoundedOutOfOrdernessWatermarks<T> implements WatermarkGenerator<T> {
    private long maxTimestamp;
    private final long outOfOrdernessMillis;

    public BoundedOutOfOrdernessWatermarks(Duration maxOutOfOrderness) {
        Preconditions.checkNotNull(maxOutOfOrderness, "maxOutOfOrderness");
        Preconditions.checkArgument(!maxOutOfOrderness.isNegative(), "maxOutOfOrderness cannot be negative");
        this.outOfOrdernessMillis = maxOutOfOrderness.toMillis();
        this.maxTimestamp = -9223372036854775808L + this.outOfOrdernessMillis + 1L;
    }

    public void onEvent(T event, long eventTimestamp, WatermarkOutput output) {
        this.maxTimestamp = Math.max(this.maxTimestamp, eventTimestamp);
    }

    public void onPeriodicEmit(WatermarkOutput output) {
        output.emitWatermark(new Watermark(this.maxTimestamp - this.outOfOrdernessMillis - 1L));
    }
}
```

在构造器中，将我们传入的参数上限(上文中为20s)记录为outOfOrdernessMillis，在每个数据传进Flink时，用变量maxTimestamp记录目前为止最大的事件时间戳。等到定期调用onPeriodicEmit()时，会发出(maxTimestamp - outOfOrdernessMillis - 1)时间戳的水印，即上文说的延迟。

### 窗口：

窗口提供了一个从流处理到批处理的桥梁，Flink窗口分析取决于两个主要的抽象操作：

- window assigners：将事件分给窗口

- window functions：处理窗口数据

窗口的生命周期分为创建和销毁，开始时间和结束时间时基于自然时间创建的。当第一个元素携带自己的时间戳(事件时间或处理时间)到达窗口，窗口被建立；经过指定的时间和延迟时间后，窗口被销毁。此外，窗口还需要触发器triggers和驱逐器evictors，决定何时调用窗口函数和再函数执行前删除窗口中收集的部分元素。

窗口的生命周期也可以通过计数来执行，当窗口内的事件数量到达窗口要求的数值时，窗口才会触发计算，可以自定义触发器，但是无法应对超时和处理部分窗口。

注意，如果不使用键控事件流(keyBy)，程序就不能并行处理。不使用键控，所有的数据都会被划分到一个窗口中，只能被一个task处理。

#### 窗口的分类/窗口分配器：

对于键控流，使用window()定义；非键控流，使用windowAll()。

- 翻滚窗口 tumbling windows：有一个固定的长度，且不会重复。分为基于时间的和基于计数的。

- 滑动窗口 slidding windows：指定两个参数，第一个参数是窗口大小，第二个参数是窗口启动频率，即滑动大小。滑动窗口和滚动窗口的区别是可能有重复的部分。如果滑动大小小于窗口大小，窗口会重叠。分为基于时间的和基于计数的。

- 会话窗口 session windows：会话窗口根据会话的间隔把数据分配到不同的窗口。会话窗口不重叠，没有固定的起止时间。

- 全局窗口 global windows：全局窗口会把所有相同的key数据放进一个window，没有自然的窗口结束时间，需要自己指定触发器。除了全局窗口，其他都是基于时间(处理时间或事件时间)的。在很多情况下，使用ProcessFunction会较好。

#### 窗口应用函数：

基本的窗口内操作事件可以分为三种：

- 批量处理，processWindowFunction会缓存Iterable和窗口内容，供接下来全量计算

- 流处理，每当有事件被分配到窗口，都会调用reduceFunction或aggregateFunction来增量计算

- 或者结合两者，通过预聚合的增量计算结果在触发窗口的时候，提供给processWindowFunction做全量计算

##### ProcessWindowFunction的使用：

```java
public class StreamingJob {
    public static void main(String[] args) throws Exception{
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        DataStreamSource<Data> dataSource = env.addSource(new DataSource());
        dataSource
                .keyBy(Data::getData)
                .window(TumblingEventTimeWindows.of(Time.seconds(5)))
                .process(new WindowProcess())
                .print().setParallelism(1);
        env.execute();
    }
}
public class DataSource implements SourceFunction<Data> {
    @Override
    public void run(SourceContext sourceContext) throws Exception {
        for (int i = 0; i < 20; i ++) {
            Data data = new Data(1, System.currentTimeMillis());
            sourceContext.collectWithTimestamp(data, data.getTimestamp());
            Thread.sleep(1000);
        }
    }

    @Override
    public void cancel() {

    }
}
public class WindowProcess extends ProcessWindowFunction<Data, String, Integer, TimeWindow> {
    @Override
    public void process(Integer integer,
                        ProcessWindowFunction<Data, String, Integer, TimeWindow>.Context context,
                        Iterable<Data> iterable,
                        Collector<String> collector) throws Exception {
        StringBuffer stringBuffer = new StringBuffer();
        for (Data data : iterable) {
            stringBuffer.append(data.toString()).append("\n");
        }
        collector.collect(stringBuffer.toString());
    }
}
```

本例输出为：

```json
Data{data=1, timestamp=1634189195385}
Data{data=1, timestamp=1634189196401}
Data{data=1, timestamp=1634189197417}
Data{data=1, timestamp=1634189198432}
Data{data=1, timestamp=1634189199432}//第一个窗口，起止时间为[1634189195385,1634189200385)

Data{data=1, timestamp=1634189200432}
Data{data=1, timestamp=1634189201448}
Data{data=1, timestamp=1634189202463}
Data{data=1, timestamp=1634189203479}
Data{data=1, timestamp=1634189204495}//第二个窗口，起止时间为[1634189200385-1634189205385)

Data{data=1, timestamp=1634189205495}
Data{data=1, timestamp=1634189206510}
Data{data=1, timestamp=1634189207510}
Data{data=1, timestamp=1634189208510}
Data{data=1, timestamp=1634189209510}//[1634189205385,1634189210385)

Data{data=1, timestamp=1634189210511}
Data{data=1, timestamp=1634189211526}
Data{data=1, timestamp=1634189212526}
Data{data=1, timestamp=1634189213542}
Data{data=1, timestamp=1634189214542}//[1634189210385,1634189215385)
```

注意，使用ProcessWindowFunction的情况下，Flink会将全部数据收集后分配给各个窗口。因此不适用于无界流。

##### ReduceFunction的使用：

```java
public class ReduceWindow implements ReduceFunction<Data> {
    @Override
    public Data reduce(Data data, Data t1) throws Exception {
        return data.getTimestamp() > t1.getTimestamp() ? data : t1;
    }
}
```

RudeuceFunction的局限性很大，因为ReduceFunction的接口将输入输出定为相同的类：

```java
public interface ReduceFunction<T> extends Function, Serializable {
    T reduce(T var1, T var2) throws Exception;
}
```

##### AggregateFunction的使用：

```java
public class AggregateWindow implements AggregateFunction<Data, String, String> {
    @Override
    public String createAccumulator() {
        return "";
    }

    @Override
    public String add(Data data, String s) {
        StringBuffer stringBuffer = new StringBuffer(s);
        stringBuffer.append(data.toString()).append("\n");
        return stringBuffer.toString();
    }

    @Override
    public String getResult(String s) {
        return s;
    }

    @Override
    public String merge(String s, String acc1) {
        return s + acc1;
    }
}
```

同样作为增量方法，AggregateFunction的扩展性更大：

```java
public interface AggregateFunction<IN, ACC, OUT> extends Function, Serializable {
    ACC createAccumulator();

    ACC add(IN var1, ACC var2);

    OUT getResult(ACC var1);

    ACC merge(ACC var1, ACC var2);
}
```

本例输出为：

```json
2> Data{data=1, timestamp=1634192960323}
Data{data=1, timestamp=1634192961339}
Data{data=1, timestamp=1634192962354}
Data{data=1, timestamp=1634192963370}
Data{data=1, timestamp=1634192964385}

1> Data{data=1, timestamp=1634192965401}
Data{data=1, timestamp=1634192966401}
Data{data=1, timestamp=1634192967417}
Data{data=1, timestamp=1634192968417}
Data{data=1, timestamp=1634192969432}

2> Data{data=1, timestamp=1634192970448}
Data{data=1, timestamp=1634192971448}
Data{data=1, timestamp=1634192972448}
Data{data=1, timestamp=1634192973448}
Data{data=1, timestamp=1634192974463}
...
```

##### 聚和增量方式：

```java
    .aggregate(new AggregateWindow(), new ProcessWindow())
```

如果采取这种方式，在ProcessWindowFunction中，每个窗口的Iterable<T> iterable都只会剩下一个元素，就是AggregateFunction结束后调用getResult和merge方法得到的数据。ReduceFunction同理。

#### 迟到数据处理：

- 窗口允许延迟：正常情况下，窗口触发计算完成之后就会被销毁，但是设定允许延迟allowedLateness(Time lateness)后，窗口会等待延迟时长后再销毁。在该区间内迟到的数据仍然可以进入窗口，并触发新的计算。
- 侧输出迟到数据：迟到的数据可以当作特殊的流，通过调用sideOutputLateData(OutputTag<T> outputTag)将迟到数据发送到指定的侧输出流里去，再进行下一步处理。

#### 时间窗口和时间对齐：

在Flink中，并不是明确说明第一个数据到达窗口时窗口创建，而是根据一定偏移量和窗口大小进行计算，见源码：

```java
public class TimeWindow extends Window {
    ...
    public static long getWindowStartWithOffset(long timestamp, long offset, long windowSize) {
        return timestamp - (timestamp - offset + windowSize) % windowSize;
    }
    ...
}
```

举例说明，当第一个数据事件时间为12:05，需要一个大小为1h的时间窗口时，该窗口创建时间为12:00，结束时间为13:00。

滚动窗口和滑动窗口都可以指定偏移量用于改变窗口的对齐方式，默认下offset是0，结果是timestamp-timestamp%windowSize。

### 数据管道&ETL：

Flink有一个经典应用：extract 抽取 Transform 转换 Load 加载 ETL管道任务。从一个或多个数据源获取数据，进行一些转换操作和数据补充，将结果存储起来。

#### 无状态的转换：map()和flatMap()

```java
public interface MapFunction<T, O> extends Function, Serializable {
    O map(T var1) throws Exception;
}
public interface FlatMapFunction<T, O> extends Function, Serializable {
    void flatMap(T var1, Collector<O> var2) throws Exception;
}
```

接口设计如上，mapFunction只支持一对一映射，flatMapFunction可以支持任意数量。

#### 键控流 keyed stream：

```java
public class DataStream<T> {
    ...
    public <K> KeyedStream<T, K> keyBy(KeySelector<T, K> key) {
        Preconditions.checkNotNull(key);
        return new KeyedStream(this, (KeySelector)this.clean(key));
    }
    ...
}
public interface KeySelector<IN, KEY> extends Function, Serializable {
    KEY getKey(IN var1) throws Exception;
}
```

 每个keyBy会通过shuffle来为数据流进行重新分区，设计网络通信、序列化、反序列化，总体来说开销很大。

#### 有状态的转换：

Flink为函数接口提供了一套Rich变体如下：

```java
public interface RichFunction extends Function {
    void open(Configuration var1) throws Exception;

    void close() throws Exception;

    RuntimeContext getRuntimeContext();

    IterationRuntimeContext getIterationRuntimeContext();

    void setRuntimeContext(RuntimeContext var1);
}
```

受影响的函数接口举例：

```java
public abstract class RichMapFunction<IN, OUT> extends AbstractRichFunction implements MapFunction<IN, OUT> {
    private static final long serialVersionUID = 1L;

    public RichMapFunction() {
    }

    public abstract OUT map(IN var1) throws Exception;
}
```

如果想使用状态，可以在open方法中声明状态。键控状态见上文状态demo。

非键控状态也被叫做算子的状态，一般用于source和sink。

#### 关联流 connected stream：

Flink支持单独的算子存在两个输入流。一般来说，一个数据流，一个控制流。

本例中，输出数据流中和控制流不匹配的数据。

```java
public class main {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        DataStream<String> control = env
                .fromElements("data1")
                .keyBy(x -> x);

        DataStream<String> streamOfWords = env
                .fromElements("data1", "data2", "data3")
                .keyBy(x -> x);

        control.connect(streamOfWords)
                .flatMap(new ControlFunction())
                .print();

        env.execute();
    }
}
public class ControlFunction extends RichCoFlatMapFunction<String, String, String> {
    private ValueState<Boolean> blocked;

    @Override
    public void open(Configuration parameters) {
        blocked = getRuntimeContext().getState(new ValueStateDescriptor<Boolean>("blocked", Boolean.class));
    }

    @Override
    public void flatMap1(String s, Collector<String> collector) throws Exception {
        blocked.update(Boolean.TRUE);
    }

    @Override
    public void flatMap2(String s, Collector<String> collector) throws Exception {
        if (blocked.value() == null) {
            collector.collect(s);
        }
    }
}
```

本例输出为：

```json
2> data3
1> data2
```

注意，只有当两个流一致的时候才能连接，在键控流中，存储的布尔类型的状态被相同键的两个流共享。造成上述输出的原因是算子并行度没有设为1。

RichCoFlatFunction是一种可以被用于一对连接流的FlatMapFunction，同时实现了RichFuntion接口。

```java
public abstract class RichCoFlatMapFunction<IN1, IN2, OUT> extends AbstractRichFunction implements CoFlatMapFunction<IN1, IN2, OUT> {
    private static final long serialVersionUID = 1L;

    public RichCoFlatMapFunction() {
    }
}
public interface CoFlatMapFunction<IN1, IN2, OUT> extends Function, Serializable {
    void flatMap1(IN1 var1, Collector<OUT> var2) throws Exception;

    void flatMap2(IN2 var1, Collector<OUT> var2) throws Exception;
}
```

flatMap1和flatMap2的执行顺序是由两个流的连接顺序决定的，在本例中control流的数据进入flatMap1，streamOfWords中的数据流入flatMap2。若是想控制调用顺序，可以自定义算子实现InputSelectable接口：

```java
public interface InputSelectable {
    InputSelection nextSelection();
}
```
该接口实现了输入流的调用顺序，简单实现：

```java
public class main {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        DataStream<Data> dataDataStream = env
                .setParallelism(1)
                .addSource(new DataSource());
        DataStream<Data> controllerStream = env
                .setParallelism(1)
                .addSource(new DataSource());
        controllerStream
                .connect(dataDataStream)
                .transform("test", Types.STRING, new TestOperator())
                .print();
        env.execute();
    }
}
public class TestOperator extends AbstractStreamOperator<String> implements TwoInputStreamOperator<Data, Data, String>, InputSelectable {
    @Override
    public InputSelection nextSelection() {
        return InputSelection.SECOND;
    }

    @Override
    public void processElement1(StreamRecord streamRecord) throws Exception {
        output.collect(new StreamRecord<>("ele1" + streamRecord.getValue().toString()));
    }

    @Override
    public void processElement2(StreamRecord streamRecord) throws Exception {
        output.collect(new StreamRecord<>("ele2" + streamRecord.getValue().toString()));
    }
}
```

本例输出为：

```json
ele2Data{dataId=1, data=0, timestamp=1634264829187}
ele2Data{dataId=1, data=1, timestamp=1634264829640}
ele2Data{dataId=1, data=2, timestamp=1634264829640}
ele2Data{dataId=1, data=3, timestamp=1634264829640}
ele2Data{dataId=1, data=4, timestamp=1634264829640}
ele2Data{dataId=1, data=5, timestamp=1634264829640}
ele2Data{dataId=1, data=6, timestamp=1634264829640}
ele2Data{dataId=1, data=7, timestamp=1634264829640}
ele2Data{dataId=1, data=8, timestamp=1634264829640}
ele2Data{dataId=1, data=9, timestamp=1634264829640}
```

在本例中，因为只选择了InputSelection.SECOND，所以只输出了第二个数据流的信息，对于调用第一个数据流的方法processElement1，并没有执行。此外，在本例中使用了自定义算子，使用了AbstractStreamOperator和TwoInputStreamOperator和OneInputStreamOperator。

### 事件驱动应用：

在之前使用ProcessWindowFunction的时候，将窗口声明和窗口应用函数分开：

```java
    dataSource
                .keyBy(Data::getData)
                .window(TumblingEventTimeWindows.of(Time.seconds(5)))
                .process(new WindowProcess())
                .print().setParallelism(1);
```

由于ProcessFunction将事件处理和计时器放在一起，可以直接通过内置计时器实现窗口操作。

先看KeyedProcessFunction类源码：

```java
//该类继承AbstractRichFunction、RichFunction，提供了open、close等用于实现状态或连接的方法。
//实现该类所需三个参数：K 传入数据的key类型 I 传入数据类型 O 输出数据类型。
public abstract class KeyedProcessFunction<K, I, O> extends AbstractRichFunction {
    private static final long serialVersionUID = 1L;

    public KeyedProcessFunction() {
    }
	
    //传出的每个数据都需要经过该方法处理
    public abstract void processElement(I var1, KeyedProcessFunction<K, I, O>.Context var2, Collector<O> var3) throws Exception;
	
    //当使用TimeService设置的计时器触发时调用
    public void onTimer(long timestamp, KeyedProcessFunction<K, I, O>.OnTimerContext ctx, Collector<O> out) throws Exception {
    }

    //当执行onTimer方法时，Flink提供一些信息给使用者
    public abstract class OnTimerContext extends KeyedProcessFunction<K, I, O>.Context {
        public OnTimerContext() {
            super();
        }

        //TimeDomain指定出发计时器是基于事件时间还是处理时间，自带定义EVENT_TIME和PROCESSING_TIME，由value方法获得
        public abstract TimeDomain timeDomain();
		
        //获取触发计时器的key
        public abstract K getCurrentKey();
    }
	
    //执行processElement或onTimer时，Flink提供一些信息
    public abstract class Context {
        public Context() {
        }
		
        //当前正在处理的元素的时间戳或触发计时器的时间戳
        public abstract Long timestamp();

        //用于查询时间和注册计时器的timeServer
        public abstract TimerService timerService();
		
        //侧输出
        public abstract <X> void output(OutputTag<X> var1, X var2);

        //获取当前元素key
        public abstract K getCurrentKey();
    }
}
public interface TimerService {
    String UNSUPPORTED_REGISTER_TIMER_MSG = "Setting timers is only supported on a keyed streams.";
    String UNSUPPORTED_DELETE_TIMER_MSG = "Deleting timers is only supported on a keyed streams.";

    long currentProcessingTime();

    long currentWatermark();

    void registerProcessingTimeTimer(long var1);

    void registerEventTimeTimer(long var1);

    void deleteProcessingTimeTimer(long var1);

    void deleteEventTimeTimer(long var1);
}
```

简单实现：

```java
public class StreamingJob {
    public static void main(String[] args) throws Exception{
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        DataStreamSource<Data> dataSource = env.addSource(new DataSource());
        WatermarkStrategy<Data> strategy = WatermarkStrategy.forMonotonousTimestamps();
        SingleOutputStreamOperator<String> op = dataSource
                .setParallelism(1)
                .assignTimestampsAndWatermarks(strategy)
                .keyBy(Data::getDataId)
                .process(new KeyedProcess(Time.seconds(5)));
        op.print();
        op.getSideOutput(KeyedProcess.lateData).print();
        env.execute();
    }
}
public class KeyedProcess extends KeyedProcessFunction<Integer, Data, String> {
    private final long duration;
    private ValueState<String> str;
    private ValueState<Long> time;
    public static final OutputTag<String> lateData = new OutputTag<String>("late data"){};

    public KeyedProcess(Time duration) {
        this.duration = duration.toMilliseconds();
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        str = getRuntimeContext().getState(new ValueStateDescriptor<String>("str", String.class));
        time = getRuntimeContext().getState(new ValueStateDescriptor<Long>("time", Long.class));
    }

    @Override
    public void processElement(Data data, KeyedProcessFunction<Integer, Data, String>.Context context, Collector<String> collector) throws Exception {
        long eventTime = data.getTimestamp();
        TimerService timerService = context.timerService();

        if (eventTime <= timerService.currentWatermark()) {
            //late data
            context.output(lateData, data.toString());
        }
        else {
            long endOfWindow = (eventTime - (eventTime % duration) + duration - 1L);
            timerService.registerEventTimeTimer(endOfWindow);
            if (time.value() == null) {
                time.update(endOfWindow);
            }
            str.update(str.value() == null ? "" : str.value() + data.toString() + "\n");
        }
    }

    @Override
    public void onTimer(long timestamp, KeyedProcessFunction<Integer, Data, String>.OnTimerContext ctx, Collector<String> out) throws Exception {
        String Str = str.value();
        out.collect(Str);
        this.str.clear();
        if (time.value() != null) {
            ctx.timerService().deleteEventTimeTimer(time.value());
            time.clear();
        }
    }
}
```

本例输出：

```json
...
2> Data{dataId=1, data=1, timestamp=1634282097437}
Data{dataId=1, data=1, timestamp=1634282098437}
Data{dataId=1, data=1, timestamp=1634282099453}
Data{dataId=1, data=1, timestamp=1634282100468}

2> Data{dataId=1, data=1, timestamp=1634282102484}
Data{dataId=1, data=1, timestamp=1634282103499}
Data{dataId=1, data=1, timestamp=1634282104499}
Data{dataId=1, data=1, timestamp=1634282105515}

2> Data{dataId=1, data=1, timestamp=1634282107562}
Data{dataId=1, data=1, timestamp=1634282108562}
Data{dataId=1, data=1, timestamp=1634282109562}
Data{dataId=1, data=1, timestamp=1634282110578}
...
```

注意，在本例中使用了旁路输出OutputTag，可以作为分流输出，在这里用作延迟数据处理。

### 通过快照实现容错处理：

Flink管理的键控状态由负责该键的taskManager保存在本地，Flink会定期获得这些状态的快照，并将这些快照复制到持久化位置。

快照是Flink镜像的通用术语，包括指向数据源的指针以及每个作业的有状态运算符的状态副本。

检查点是Flink自动执行的快照，目的是从故障恢复。通常情况下，Flink只会保存最近几个检查点(可配置)，并在job取消时删除，但是用户可以选择保留。

保存点是用户手动调用触发的快照。

当jobManager指示taskManager准备检查点时，记录全部source的偏移量，将带有编号的检查点插入数据流，每个检查点标记自己之前的数据和算子的状态。

拥有两个输入流的算子会执行对齐操作，检查点稍早的流会等待。

Flink存在三种语义，根据语义不同存在不一样的解决方式：

- At Most Once：对于一条数据，最多执行一次，无论成功失败(可能失败，丢失数据)

- At Least Once：对于一条数据，最少执行一次，直到收到成功消息(可能成功很多次)

- Exactly Once：精确到成功一次

当设定为精确一次时，需要保证对齐；其他设定下可以关闭对齐来提高性能。

默认情况下，状态保存在TaskManagers的内存中，检查点保存在JobManager的内存中，Flink支持各种途径存储检查点到状态后端state backends，通过StreamExecutionEnvironment#setStateBackend()指定。目前(1.14.0)，Flink内置了HashMap和EmbeddedRocksDB两种状态后端，在不设置的情况下，使用HashMapStateBackend。

默认情况下，检查点是禁用的，可以调用方法启动和设置：

```java
public class StreamExecutionEnvironment {
    ...
    //目前启动检查点的方法仅剩两种，其余都过期
    public StreamExecutionEnvironment enableCheckpointing(long interval) {
        this.checkpointCfg.setCheckpointInterval(interval);
        return this;
    }

    public StreamExecutionEnvironment enableCheckpointing(long interval, CheckpointingMode mode) {
        this.checkpointCfg.setCheckpointingMode(mode);
        this.checkpointCfg.setCheckpointInterval(interval);
        return this;
    }
    ...
    @PublicEvolving
    public StreamExecutionEnvironment setStateBackend(StateBackend backend) {
        this.defaultStateBackend = (StateBackend)Preconditions.checkNotNull(backend);
        return this;
    }
    ...
}

public interface StateBackend extends Serializable {
    //负责键控状态和其检查点
    <K> CheckpointableKeyedStateBackend<K> createKeyedStateBackend(...) throws Exception;
	
    //指定管理内存
    default <K> CheckpointableKeyedStateBackend<K> createKeyedStateBackend(...) throws Exception {
        return this.createKeyedStateBackend(env, jobID, operatorIdentifier, keySerializer, numberOfKeyGroups, keyGroupRange, kvStateRegistry, ttlTimeProvider, metricGroup, stateHandles, cancelStreamRegistry);
    }
	
    //负责算子状态
    OperatorStateBackend createOperatorStateBackend(...) throws Exception;

    default boolean useManagedMemory() {
        return false;
    }
}

public class CheckpointConfig implements Serializable {
	...
    //设置检查点类型 默认精确一次
    public void setCheckpointingMode(CheckpointingMode checkpointingMode) {
        this.checkpointingMode = (CheckpointingMode)Objects.requireNonNull(checkpointingMode);
    }
    
    
    //设置定期检查的时间间隔，默认-1
    public void setCheckpointInterval(long checkpointInterval) {
        if (checkpointInterval < 10L) {
            throw new IllegalArgumentException(String.format("Checkpoint interval must be larger than or equal to %s ms", 10L));
        } else {
            this.checkpointInterval = checkpointInterval;
        }
    }
    
    //设置每次检查点所需时间，超时就抛弃。默认10分钟，600000L
    public void setCheckpointTimeout(long checkpointTimeout) {
        if (checkpointTimeout < 10L) {
            throw new IllegalArgumentException(String.format("Checkpoint timeout must be larger than or equal to %s ms", 10L));
        } else {
            this.checkpointTimeout = checkpointTimeout;
        }
    }
    
    //设置两次检查点之间最小暂停间隔，默认0
    public void setMinPauseBetweenCheckpoints(long minPauseBetweenCheckpoints) {
        if (minPauseBetweenCheckpoints < 0L) {
            throw new IllegalArgumentException("Pause value must be zero or positive");
        } else {
            this.minPauseBetweenCheckpoints = minPauseBetweenCheckpoints;
        }
    }
    
    //设置同时执行的最大检查点数量，默认1
    public void setMaxConcurrentCheckpoints(int maxConcurrentCheckpoints) {
        if (maxConcurrentCheckpoints < 1) {
            throw new IllegalArgumentException("The maximum number of concurrent attempts must be at least one.");
        } else {
            this.maxConcurrentCheckpoints = maxConcurrentCheckpoints;
        }
    }
    
    //设置检查点允许最大连续失败次数，默认-1
    public void setTolerableCheckpointFailureNumber(int tolerableCheckpointFailureNumber) {
        if (tolerableCheckpointFailureNumber < 0) {
            throw new IllegalArgumentException("The tolerable failure checkpoint number must be non-negative.");
        } else {
            this.tolerableCheckpointFailureNumber = tolerableCheckpointFailureNumber;
        }
    }
    
    //设置存储，还有String、URI、Path类型的参数。没有默认值。注意后三种类型需要使用FileSystemCheckpointStorage
    public void setCheckpointStorage(CheckpointStorage storage) {
        Preconditions.checkNotNull(storage, "Checkpoint storage must not be null");
        this.storage = storage;
    }
    
    //设置在job取消后是否保存外部检查点
    public void enableExternalizedCheckpoints(CheckpointConfig.ExternalizedCheckpointCleanup cleanupMode) {
        this.externalizedCheckpointCleanup = (CheckpointConfig.ExternalizedCheckpointCleanup)Preconditions.checkNotNull(cleanupMode);
    }
    
    public static enum ExternalizedCheckpointCleanup {
        DELETE_ON_CANCELLATION(true),
        RETAIN_ON_CANCELLATION(false);

        private final boolean deleteOnCancellation;

        private ExternalizedCheckpointCleanup(boolean deleteOnCancellation) {
            this.deleteOnCancellation = deleteOnCancellation;
        }

        public boolean deleteOnCancellation() {
            return this.deleteOnCancellation;
        }
    }
    ...
}

public enum CheckpointingMode {
    EXACTLY_ONCE,
    AT_LEAST_ONCE;

    private CheckpointingMode() {
    }
}

public interface CheckpointStorage extends Serializable {
    CompletedCheckpointStorageLocation resolveCheckpoint(String var1) throws IOException;

    CheckpointStorageAccess createCheckpointStorage(JobID var1) throws IOException;
}
```

注意，如果想实现该机制，数据源需要能够将流回滚到最近的定义点，例如kafka。

实现检查点的核心机制是流屏障barriers，这些屏障被注入到数据流中和流一起移动，不会超过记录并严格按照顺序。每个屏障带有快照id，记录该屏障前的数据(状态)。

当算子接收到全部输入流的屏障n，当前算子就对状态做一次快照，成功将屏障以广播形式传给下游，等到屏障流经sink，sink向jobManager确认，当jobManager确认全部的sink都发出本次快照后，本次快照生成。

有一个特殊情况，就是接受多个输入流的算子，这个时候屏障会对多个数据流进行对齐。

从上游某个数据源接收到快照屏障n，这个时候算子会停止对该流的处理，直到剩下的流将快照屏障n发送过来。这种机制避免了两个流的效率不一致导致的算子快照错误。算子暂停对流的处理不会导致数据阻塞，而是将数据缓存，缓存的数据会优先处理。

当发生故障的时候，Flink会选择最近的检查点，Flink重新部署整个分布式数据流(默认是全重置，可以在设置中修改为区域重置)，并为全部算子提供当前快照状态，同时将源设置为从快照位置读取。

在1.11版本之后，Flink提供了非对齐的检查点方式，旨在消除屏障对齐的风险-job出现反压，导致数据流动速度大幅减慢。但是非对齐方式增加了额外的IO压力。用户设置的保存点必须对齐。

当算子的所有输入流中第一个屏障到达算子的输入缓冲区时(并非到达算子)，立即将这个屏障发往下游(输出缓冲区末尾)。Flink会标记两部分数据：第一个屏障所在流中输入和输出缓冲区中的数据、其他流中位于当前屏障之前的数据。Flink将标记数据和状态做快照。

注意，故障恢复遵循集群的默认重启政策和job中定义的重启政策，并且以后者为先。集群的配置文件是flink-conf.yaml，其中参数restart-strategy定义具体的策略，详细配置如下：

- none, off, disable：不采取重启政策

  ```java
  	StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
  	env.setRestartStrategy(RestartStrategies.noRestart());
  ```

- fixeddelay, fixed-delay：固定延时重启策略按照给定的次数尝试重启策略，在连续的两次重启尝试之间，会等待固定时间。详细配置由restart-strategy.fixed-delay.attempts Integer 次数 和restart-strategy.fixed-delay.delay Duration 时间间隔 决定。

  ```java
  	StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
  	env.setRestartStrategy(RestartStrategies.fixedDelayRestart(3, Time.of(10, TimeUnit.MINUTES)));
  ```

- failurerate, failure-rate：故障率重启策略，当每个单位时间发生故障的次数超过设定的限制时，作业失败。restart-strategy.failure-rate.delay 时间间隔，restart-strategy.failure-rate.failure-rate-interval 单位时间，restart-strategy.failure-rate.max-failures-per-interval 重启次数。

  ```java
  	StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
  	env.setRestartStrategy(RestartStrategies.failureRateRestart(3, Time.of(5, TimeUnit.MINUTES), Time.of(10, TimeUnit.SECONDS)));
  ```

- 在java中配置使用集群的策略

  ```java
  	StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
  	env.setRestartStrategy(new RestartStrategies.FallbackRestartStrategyConfiguration());
  ```

Flink支持不同的故障恢复策略，该策略需要通过配置flink-conf.yaml文件中的jobmanager.execution.failover-strategy进行配置。

- 全面重启：full，Task发生故障的时候会重启作业中所有的task进行故障恢复。
- 局部重启：region，将作业中的Task划分为多个region，当task发生故障，会找出需要故障恢复的最小region集合。

此处的region指的是使用DataStream和流式table/sql作业的所有数据交换的task集合。出错的task所在的region需要重启，如果要重启的region需要消费的数据不能完全访问，则产出数据的region也需要重启，需要重启的region的下游region也需要重启。

另外，批处理的恢复不使用检查点，而是使用数据流的重放实现的。

### Flink API种类：

Flink为流处理提供四种级别的API，由底层向上分别为：

- stateful stream processing 有状态实时流处理，抽象实现是ProcessFunction。
- core api 包括DataStream API应用于有界无界流处理和DataSet API应用于有界数据集场景，包括transformations、window等
- table api，以表为中心的声明式编程
- 最顶层抽象是sql，程序实现是sql查询表达式。

### DataStream API

```java
public class DataStream<T> {
    //在给定的执行环境下创建DataStream
    protected final StreamExecutionEnvironment environment;
    //每个DataStream都有一个底层Transformation，对DataStream API的每次调用，都会转换成一个对应的Transformation，执行环境会记录全部的Transformation。
    protected final Transformation<T> transformation;

    public DataStream(StreamExecutionEnvironment environment, Transformation<T> transformation) {
        //构造函数，会检查非空
    }

    ...

    public ExecutionConfig getExecutionConfig() {
        return this.environment.getConfig();
    }

    @SafeVarargs
    public final DataStream<T> union(DataStream<T>... streams) {
        //通过合并相同类型的DataStream输出创建一个新的DataStream
        List<Transformation<T>> unionedTransforms = new ArrayList();
        unionedTransforms.add(this.transformation);
        DataStream[] var3 = streams;
        int var4 = streams.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            DataStream<T> newStream = var3[var5];
            if (!this.getType().equals(newStream.getType())) {
                throw new IllegalArgumentException("Cannot union streams of different types: " + this.getType() + " and " + newStream.getType());
            }
			//记录下需要添加的流的Transformation
            unionedTransforms.add(newStream.getTransformation());
        }
		//具体的合并设置：并行度为当前流的并行度
        return new DataStream(this.environment, new UnionTransformation(unionedTransforms));
    }

    public <R> ConnectedStreams<T, R> connect(DataStream<R> dataStream) {
    	//通过将不同类型(可能)的DataStream输出成一个新的ConnectedStreams
    	//使用该运算符连接的DataStream可以和CoFunctions一起使用，包括CoMapFunction、CoFlatMapFunction、CoProcessFunction、KeyedCoProcessFunction等
    	//在这里将当前流定义为first，参数中的流为second
        return new ConnectedStreams(this.environment, this, dataStream);
    }

    @PublicEvolving
    public <R> BroadcastConnectedStream<T, R> connect(BroadcastStream<R> broadcastStream) {
    	//将当前DataStream或KeyedStream与BroadcastStream连接创建，后者可用broadcast()方法创建。
        ...
    }

    public <K> KeyedStream<T, K> keyBy(KeySelector<T, K> key) {
        ...
    }

    ...

    private KeyedStream<T, Tuple> keyBy(Keys<T> keys) {
        ...
    }

    ...

    public <K> DataStream<T> partitionCustom(Partitioner<K> partitioner, KeySelector<T, K> keySelector) {
    	//对键进行分区
        ...
    }

    private <K> DataStream<T> partitionCustom(Partitioner<K> partitioner, Keys<T> keys) {
        ...
    }

    public DataStream<T> broadcast() {
        //将分区类型设置为广播
        ...
    }

    @PublicEvolving
    public BroadcastStream<T> broadcast(MapStateDescriptor<?, ?>... broadcastStateDescriptors) {
        //将分区类型设置为广播，拥有和输入一样多的广播状态
        //调用selectChannel()会抛出异常，在isBroadcast()中返回true
        ...
    }

    @PublicEvolving
    public DataStream<T> shuffle() {
        //设置分区类型为随机分区
        //调用selectChannel()中使用Radom
        ...
    }

    public DataStream<T> forward() {
        //设置分区格式，将元素转发到下一个操作的本地子任务
        //调用selectChannel()直接返回0
        //isPointwise属性为true
        ...
    }

    public DataStream<T> rebalance() {
        //设置分区格式，将元素以循环方式均匀分布到下一个操作实例
        //调用selectChannel()，通过变量+1除余的方式均匀分布
        ...
    }

    @PublicEvolving
    public DataStream<T> rescale() {
        //设置分区格式，将元素以循环方式均匀分布到下一个操作实例
        //调用selectChannel()，变量+1从0到下游并行度之间循环
        //isPointwise属性为true
        ...
    }

    @PublicEvolving
    public DataStream<T> global() {
        //设置分区格式，将输出值转到下一个处理运算符的第一个实例
        //调用selectChannel()直接返回0
        ...
    }

    @PublicEvolving
    public IterativeStream<T> iterate() {
        return new IterativeStream(this, 0L);
    }

    @PublicEvolving
    public IterativeStream<T> iterate(long maxWaitTimeMillis) {
        //指定流的哪一部分该流向什么地方
        //IterativeStream会记录当前转换，IterativeStream#closeWith()方法用于设定结束迭代的条件，满足条件的元素会重新进入IterativeStream头部再进行一次计算
        //举例说明：[0-10]的集合进行iterate，添加map算子(i->i-2)，添加close条件是fliter(i>0)，将iterate流print
        //得到输出(换行省略)：012345678910 12345678 123456 1234 12 
        return new IterativeStream(this, maxWaitTimeMillis);
    }

    public <R> SingleOutputStreamOperator<R> map(MapFunction<T, R> mapper) {
        ...
    }

    public <R> SingleOutputStreamOperator<R> map(MapFunction<T, R> mapper, TypeInformation<R> outputType) {
        ...
    }

    public <R> SingleOutputStreamOperator<R> flatMap(FlatMapFunction<T, R> flatMapper) {
        ...
    }

    public <R> SingleOutputStreamOperator<R> flatMap(FlatMapFunction<T, R> flatMapper, TypeInformation<R> outputType) {
        ...
    }

    @PublicEvolving
    public <R> SingleOutputStreamOperator<R> process(ProcessFunction<T, R> processFunction) {
        ...
    }

    @Internal
    public <R> SingleOutputStreamOperator<R> process(ProcessFunction<T, R> processFunction, TypeInformation<R> outputType) {
        ...
    }

    public SingleOutputStreamOperator<T> filter(FilterFunction<T> filter) {
        ...
    }

    @PublicEvolving
    public <R extends Tuple> SingleOutputStreamOperator<R> project(int... fieldIndexes) {
        //只能应用于Tuple数据流，根据参数索引获取元素子集
        ...
    }

    ...

    public <T2> JoinedStreams<T, T2> join(DataStream<T2> otherStream) {
        return new JoinedStreams(this, otherStream);
    }

    ...

    public AllWindowedStream<T, GlobalWindow> countWindowAll(long size) {
        //计数翻滚窗口
        ...
    }

    public AllWindowedStream<T, GlobalWindow> countWindowAll(long size, long slide) {
        //计数滑动窗口
        ...
    }

    @PublicEvolving
    public <W extends Window> AllWindowedStream<T, W> windowAll(WindowAssigner<? super T, W> assigner) {
        //通过指定触发器判断元素放入哪个窗口
        //也通过触发器判断什么时候执行窗口函数
        ...
    }

    public SingleOutputStreamOperator<T> assignTimestampsAndWatermarks(WatermarkStrategy<T> watermarkStrategy) {
        //为数据流中的元素分配时间戳并生成水印
        ...
    }

    ...

    @PublicEvolving
    public DataStreamSink<T> print() {
        //由PrintSinkFunction类提供实现
        //相当于addSink
        ...
    }

    @PublicEvolving
    public DataStreamSink<T> printToErr() {
        ...
    }

    @PublicEvolving
    public DataStreamSink<T> print(String sinkIdentifier) {
        //指定前缀
        ...
    }

    @PublicEvolving
    public DataStreamSink<T> printToErr(String sinkIdentifier) {
        ...
    }

    ...

    @PublicEvolving
    public DataStreamSink<T> writeToSocket(String hostName, int port, SerializationSchema<T> schema) {
        //相当于addSink，指定序列化方式
        ...
    }

    ...

    @PublicEvolving
    public <R> SingleOutputStreamOperator<R> transform(String operatorName, TypeInformation<R> outTypeInfo, OneInputStreamOperator<T, R> operator) {
        //用户自定义运算符
        ...
    }

    @PublicEvolving
    public <R> SingleOutputStreamOperator<R> transform(String operatorName, TypeInformation<R> outTypeInfo, OneInputStreamOperatorFactory<T, R> operatorFactory) {
        return this.doTransform(operatorName, outTypeInfo, operatorFactory);
    }

    protected <R> SingleOutputStreamOperator<R> doTransform(String operatorName, TypeInformation<R> outTypeInfo, StreamOperatorFactory<R> operatorFactory) {
        ...
    }

    protected DataStream<T> setConnectionType(StreamPartitioner<T> partitioner) {
        //指定分区器
        ...
    }

    public DataStreamSink<T> addSink(SinkFunction<T> sinkFunction) {
        //只有具备sink之后，execute才会被执行
        ...
    }

    @Experimental
    public DataStreamSink<T> sinkTo(Sink<T, ?, ?, ?> sink) {
        //四个参数：输入数据类型、提交给接收器暂存的数据类型、接收器写入类型、聚合可提交的数据类型
        ...
    }

    public CloseableIterator<T> executeAndCollect() throws Exception {
        return this.executeAndCollect("DataStream Collect");
    }

    public CloseableIterator<T> executeAndCollect(String jobExecutionName) throws Exception {
    	//触发分布式执行并返回一个迭代器
        return this.executeAndCollectWithClient(jobExecutionName).iterator;
    }

    public List<T> executeAndCollect(int limit) throws Exception {
        return this.executeAndCollect("DataStream Collect", limit);
    }

    public List<T> executeAndCollect(String jobExecutionName, int limit) throws Exception {
        //执行executeAndCollectWithClient，返回列表限制长度
        ...
    }

    ClientAndIterator<T> executeAndCollectWithClient(String jobExecutionName) throws Exception {
        //执行env.executeAsync(name)
        ...
    }

    @Internal
    public Transformation<T> getTransformation() {
        return this.transformation;
    }
}
```

