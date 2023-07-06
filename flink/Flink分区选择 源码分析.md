# Flink分区选择 源码分析

先看最基本的分区逻辑选择接口：

```java
public interface ChannelSelector<T extends IOReadableWritable> {

	//初始化通道选择器，参数为连接到相应输出的总输出通道个数
    void setup(int var1);
	
	//返回当前数据应该写入的逻辑通道的索引，为广播流调用此方法会抛出异常UnsupportedOperationException
    int selectChannel(T var1);

	//是否广播
    boolean isBroadcast();
}
```

分区选择的转化和转化的实现类：

```java
//在对DataStream使用keyBy()的时候生成PartitionTransformation
public class PartitionTransformation<T> extends Transformation<T> {
    //记录的是流中上一个转换
    private final Transformation<T> input;
    private final StreamPartitioner<T> partitioner;
    //分发模式，在1.14.0更改为StreamExchangeMode
    //分为三种模式，批处理模式(生产者产生整个结果)、流模式(生产者和消费者同时在线)、未定义
    private final ShuffleMode shuffleMode;

    public PartitionTransformation(Transformation<T> input, StreamPartitioner<T> partitioner) {
        this(input, partitioner, ShuffleMode.UNDEFINED);
    }

    public PartitionTransformation(Transformation<T> input, StreamPartitioner<T> partitioner, ShuffleMode shuffleMode) {
        super("Partition", input.getOutputType(), input.getParallelism());
        this.input = input;
        this.partitioner = partitioner;
        this.shuffleMode = (ShuffleMode)Preconditions.checkNotNull(shuffleMode);
    }

    public StreamPartitioner<T> getPartitioner() {
        return this.partitioner;
    }

    public ShuffleMode getShuffleMode() {
        return this.shuffleMode;
    }

    public List<Transformation<?>> getTransitivePredecessors() {
        List<Transformation<?>> result = Lists.newArrayList();
        result.add(this);
        result.addAll(this.input.getTransitivePredecessors());
        return result;
    }

    public List<Transformation<?>> getInputs() {
        return Collections.singletonList(this.input);
    }
}

//和其他转化一样，在StreamExecutionEnvironment.execute()执行后被调用
public class PartitionTransformationTranslator<OUT> extends SimpleTransformationTranslator<OUT, PartitionTransformation<OUT>> {
    public PartitionTransformationTranslator() {
    }

    protected Collection<Integer> translateForBatchInternal(PartitionTransformation<OUT> transformation, Context context) {
        return this.translateInternal(transformation, context);
    }

    protected Collection<Integer> translateForStreamingInternal(PartitionTransformation<OUT> transformation, Context context) {
        return this.translateInternal(transformation, context);
    }

    private Collection<Integer> translateInternal(PartitionTransformation<OUT> transformation, Context context) {
        Preconditions.checkNotNull(transformation);
        Preconditions.checkNotNull(context);
        StreamGraph streamGraph = context.getStreamGraph();
        List<Transformation<?>> parentTransformations = transformation.getInputs();
        Preconditions.checkState(parentTransformations.size() == 1, "Expected exactly one input transformation but found " + parentTransformations.size());
        Transformation<?> input = (Transformation)parentTransformations.get(0);
        List<Integer> resultIds = new ArrayList();
        Iterator var7 = context.getStreamNodeIds(input).iterator();

        while(var7.hasNext()) {
            Integer inputId = (Integer)var7.next();
            int virtualId = Transformation.getNewNodeId();
            streamGraph.addVirtualPartitionNode(inputId, virtualId, transformation.getPartitioner(), transformation.getShuffleMode());
            resultIds.add(virtualId);
        }

        return resultIds;
    }
}

```

分区器，在生成JobGraph才会调用，这里不做详述。

在看具体的实现类之前，有一个枚举类SubtaskStateMapper：

```java
public enum SubtaskStateMapper {
    ARBITRARY {
        //循环分配
        public int[] getOldSubtasks(int newSubtaskIndex, int oldNumberOfSubtasks, int newNumberOfSubtasks) {
            return ROUND_ROBIN.getOldSubtasks(newSubtaskIndex, oldNumberOfSubtasks, newNumberOfSubtasks);
        }
    },
    //在1.14.0被移除
    DISCARD_EXTRA_STATE {
        //多余的被丢弃
        public int[] getOldSubtasks(int newSubtaskIndex, int oldNumberOfSubtasks, int newNumberOfSubtasks) {
            return newSubtaskIndex >= oldNumberOfSubtasks ? SubtaskStateMapper.EMPTY : new int[]{newSubtaskIndex};
        }
    },
    FIRST {
        //将子任务恢复到第一个子任务
        public int[] getOldSubtasks(int newSubtaskIndex, int oldNumberOfSubtasks, int newNumberOfSubtasks) {
            return newSubtaskIndex == 0 ? IntStream.range(0, oldNumberOfSubtasks).toArray() : SubtaskStateMapper.EMPTY;
        }
    },
    FULL {
        //回复到全部子任务
        public int[] getOldSubtasks(int newSubtaskIndex, int oldNumberOfSubtasks, int newNumberOfSubtasks) {
            return IntStream.range(0, oldNumberOfSubtasks).toArray();
        }

        public boolean isAmbiguous() {
            return true;
        }
    },
    RANGE {
        //根据哈希再次分配
        public int[] getOldSubtasks(int newSubtaskIndex, int oldNumberOfSubtasks, int newNumberOfSubtasks) {
            int maxParallelism = 32768;
            KeyGroupRange newRange = KeyGroupRangeAssignment.computeKeyGroupRangeForOperatorIndex(maxParallelism, newNumberOfSubtasks, newSubtaskIndex);
            int start = KeyGroupRangeAssignment.computeOperatorIndexForKeyGroup(maxParallelism, oldNumberOfSubtasks, newRange.getStartKeyGroup());
            int end = KeyGroupRangeAssignment.computeOperatorIndexForKeyGroup(maxParallelism, oldNumberOfSubtasks, newRange.getEndKeyGroup());
            return IntStream.range(start, end + 1).toArray();
        }

        public boolean isAmbiguous() {
            return true;
        }
    },
    ROUND_ROBIN {
        //以循环的方式重新分配
        public int[] getOldSubtasks(int newSubtaskIndex, int oldNumberOfSubtasks, int newNumberOfSubtasks) {
            IntArrayList subtasks = new IntArrayList(oldNumberOfSubtasks / newNumberOfSubtasks + 1);

            for(int subtask = newSubtaskIndex; subtask < oldNumberOfSubtasks; subtask += newNumberOfSubtasks) {
                subtasks.add(subtask);
            }

            return subtasks.toArray();
        }
    },
    UNSUPPORTED {
        public int[] getOldSubtasks(int newSubtaskIndex, int oldNumberOfSubtasks, int newNumberOfSubtasks) {
            throw new UnsupportedOperationException("Cannot rescale the given pointwise partitioner.\nDid you change the partitioner to forward or rescale?\nIt may also help to add an explicit shuffle().");
        }
    };

    private static final int[] EMPTY = new int[0];
    ...
}
```

Stream API中常见的分区选择器实现类由如下几种：

## StreamPartitioner

StreamPartitioner是下面全部partitioner的父类，进行了一些基础实现。

```java
public abstract class StreamPartitioner<T> implements ChannelSelector<SerializationDelegate<StreamRecord<T>>>, Serializable {
    private static final long serialVersionUID = 1L;
    protected int numberOfChannels;

    public StreamPartitioner() {
    }

    public void setup(int numberOfChannels) {
        this.numberOfChannels = numberOfChannels;
    }

    public boolean isBroadcast() {
        return false;
    }

    public abstract StreamPartitioner<T> copy();

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            StreamPartitioner<?> that = (StreamPartitioner)o;
            return this.numberOfChannels == that.numberOfChannels;
        } else {
            return false;
        }
    }

    public int hashCode() {
        return Objects.hash(new Object[]{this.numberOfChannels});
    }

	//当上游在恢复数据期间进行缩放，指定分区器行为
    public SubtaskStateMapper getUpstreamSubtaskStateMapper() {
        return SubtaskStateMapper.ARBITRARY;
    }
	//当下游在恢复数据期间进行缩放，指定分区器行为
    public abstract SubtaskStateMapper getDownstreamSubtaskStateMapper();
	
	//是否Pointwise
    public abstract boolean isPointwise();
}
```

## KeyGroupStreamPartitioner

根据键组key group索引选择目标通道。

```java
public class KeyGroupStreamPartitioner<T, K> extends StreamPartitioner<T> implements ConfigurableStreamPartitioner {
	...
	private final KeySelector<T, K> keySelector;
    private int maxParallelism;
	...
	public int selectChannel(SerializationDelegate<StreamRecord<T>> record) {
        Object key;
        try {
            key = this.keySelector.getKey(((StreamRecord)record.getInstance()).getValue());
        } catch (Exception var4) {
            throw new RuntimeException("Could not extract key from " + ((StreamRecord)record.getInstance()).getValue(), var4);
        }
		
        //交给KeyGroupRangeAssignment判断所在索引
        return KeyGroupRangeAssignment.assignKeyToParallelOperator(key, this.maxParallelism, this.numberOfChannels);
    }
    
    //当下游在恢复数据期间进行缩放，指定分区器行为
    public SubtaskStateMapper getDownstreamSubtaskStateMapper() {
        return SubtaskStateMapper.RANGE;
    }
    
    public boolean isPointwise() {
        return false;
    }
    
    public int hashCode() {
        return Objects.hash(new Object[]{super.hashCode(), this.keySelector, this.maxParallelism});
    }
    ...
}

public final class KeyGroupRangeAssignment {
    ...
    public static int assignKeyToParallelOperator(Object key, int maxParallelism, int parallelism) {
        Preconditions.checkNotNull(key, "Assigned key must not be null!");
        return computeOperatorIndexForKeyGroup(maxParallelism, parallelism, assignToKeyGroup(key, maxParallelism));
    }
    
    //第二次哈希
    public static int assignToKeyGroup(Object key, int maxParallelism) {
        Preconditions.checkNotNull(key, "Assigned key must not be null!");
        return computeKeyGroupForKeyHash(key.hashCode(), maxParallelism);
    }
    
    //第一次哈希
    public static int computeKeyGroupForKeyHash(int keyHash, int maxParallelism) {
        return MathUtils.murmurHash(keyHash) % maxParallelism;
    }
    
    //传入的keyGroupId只会在0-maxParallelism之间
    public static int computeOperatorIndexForKeyGroup(int maxParallelism, int parallelism, int keyGroupId) {
        return keyGroupId * parallelism / maxParallelism;
    }
    ...
}

public interface KeySelector<IN, KEY> extends Function, Serializable {
    KEY getKey(IN var1) throws Exception;
}
```

## BroadcastPartitioner

```java
public class BroadcastPartitioner<T> extends StreamPartitioner<T> {
    private static final long serialVersionUID = 1L;

    public BroadcastPartitioner() {
    }

    public int selectChannel(SerializationDelegate<StreamRecord<T>> record) {
        throw new UnsupportedOperationException("Broadcast partitioner does not support select channels.");
    }

    public SubtaskStateMapper getUpstreamSubtaskStateMapper() {
        return SubtaskStateMapper.DISCARD_EXTRA_STATE;
    }

    public SubtaskStateMapper getDownstreamSubtaskStateMapper() {
        return SubtaskStateMapper.ROUND_ROBIN;
    }

    public boolean isBroadcast() {
        return true;
    }

    public StreamPartitioner<T> copy() {
        return this;
    }

    public boolean isPointwise() {
        return false;
    }

    public String toString() {
        return "BROADCAST";
    }
}
```

## GlobalPartitioner

将数据发送到子任务id=0的下游算子

```java
public class GlobalPartitioner<T> extends StreamPartitioner<T> {
    private static final long serialVersionUID = 1L;

    public GlobalPartitioner() {
    }

    public int selectChannel(SerializationDelegate<StreamRecord<T>> record) {
        return 0;
    }

    public StreamPartitioner<T> copy() {
        return this;
    }

    public SubtaskStateMapper getDownstreamSubtaskStateMapper() {
        return SubtaskStateMapper.FIRST;
    }

    public boolean isPointwise() {
        return false;
    }

    public String toString() {
        return "GLOBAL";
    }
}
```

## ForwardPartitioner

将元素转发到本地环境的下游算子

```java
public class ForwardPartitioner<T> extends StreamPartitioner<T> {
    private static final long serialVersionUID = 1L;

    public ForwardPartitioner() {
    }

    public int selectChannel(SerializationDelegate<StreamRecord<T>> record) {
        return 0;
    }

    public StreamPartitioner<T> copy() {
        return this;
    }
	
	//和Global的区别
    public boolean isPointwise() {
        return true;
    }

    public String toString() {
        return "FORWARD";
    }

	//和Global的区别
    public SubtaskStateMapper getDownstreamSubtaskStateMapper() {
        return SubtaskStateMapper.UNSUPPORTED;
    }

	//和Global的区别
    public SubtaskStateMapper getUpstreamSubtaskStateMapper() {
        return SubtaskStateMapper.UNSUPPORTED;
    }
}
```

## RebalancePartitioner

通过循环来平均分配数据

```java
public class RebalancePartitioner<T> extends StreamPartitioner<T> {
    private static final long serialVersionUID = 1L;
    private int nextChannelToSendTo;

    public RebalancePartitioner() {
    }

    public void setup(int numberOfChannels) {
        super.setup(numberOfChannels);
        this.nextChannelToSendTo = ThreadLocalRandom.current().nextInt(numberOfChannels);
    }

    public int selectChannel(SerializationDelegate<StreamRecord<T>> record) {
        this.nextChannelToSendTo = (this.nextChannelToSendTo + 1) % this.numberOfChannels;
        return this.nextChannelToSendTo;
    }

    public SubtaskStateMapper getDownstreamSubtaskStateMapper() {
        return SubtaskStateMapper.ROUND_ROBIN;
    }

    public StreamPartitioner<T> copy() {
        return this;
    }

    public boolean isPointwise() {
        return false;
    }

    public String toString() {
        return "REBALANCE";
    }
}
```

## ForwardPartitioner和RebalancePartitioner扩展

在StreamGraph类中，有一个addEdgeInternal()方法：

```java
	...
    //存储非分区操作节点
    private Map<Integer, Tuple2<Integer, OutputTag>> virtualSideOutputNodes;
	//存储分区操作节点
    private Map<Integer, Tuple3<Integer, StreamPartitioner<?>, ShuffleMode>> virtualPartitionNodes;
	...

	private void addEdgeInternal(Integer upStreamVertexID, Integer downStreamVertexID, int typeNumber, StreamPartitioner<?> partitioner, List<String> outputNames, OutputTag outputTag, ShuffleMode shuffleMode) {
        int virtualId;
        if (this.virtualSideOutputNodes.containsKey(upStreamVertexID)) {
            ...
        } else if (this.virtualPartitionNodes.containsKey(upStreamVertexID)) {
            ...
        } else {
            StreamNode upstreamNode = this.getStreamNode(upStreamVertexID);
            StreamNode downstreamNode = this.getStreamNode(downStreamVertexID);
            if (partitioner == null && upstreamNode.getParallelism() == downstreamNode.getParallelism()) {
                //当上下游并行度相同时，默认采用ForwardPartitioner，进行一对一分区
                partitioner = new ForwardPartitioner();
            } else if (partitioner == null) {
                //当并行度不相同，默认采用RebalancePartitioner，循环分配
                partitioner = new RebalancePartitioner();
            }
			
            //若指定ForwardPartitioner但是上下游并行度不相同时抛出异常
            if (partitioner instanceof ForwardPartitioner && upstreamNode.getParallelism() != downstreamNode.getParallelism()) {
                throw new UnsupportedOperationException(...);
            }

            if (shuffleMode == null) {
                shuffleMode = ShuffleMode.UNDEFINED;
            }

            StreamEdge edge = new StreamEdge(upstreamNode, downstreamNode, typeNumber, (StreamPartitioner)partitioner, outputTag, shuffleMode);
            this.getStreamNode(edge.getSourceId()).addOutEdge(edge);
            this.getStreamNode(edge.getTargetId()).addInEdge(edge);
        }

    }
	...
```

## RescalePartitioner

以循环方式分配

```java
public class RescalePartitioner<T> extends StreamPartitioner<T> {
    private static final long serialVersionUID = 1L;
    private int nextChannelToSendTo = -1;

    public RescalePartitioner() {
    }

    public int selectChannel(SerializationDelegate<StreamRecord<T>> record) {
        if (++this.nextChannelToSendTo >= this.numberOfChannels) {
            this.nextChannelToSendTo = 0;
        }

        return this.nextChannelToSendTo;
    }

    public SubtaskStateMapper getDownstreamSubtaskStateMapper() {
        return SubtaskStateMapper.UNSUPPORTED;
    }

    public SubtaskStateMapper getUpstreamSubtaskStateMapper() {
        return SubtaskStateMapper.UNSUPPORTED;
    }

    public StreamPartitioner<T> copy() {
        return this;
    }

    public String toString() {
        return "RESCALE";
    }

	//和RebalancePartitioner不同的地方
    public boolean isPointwise() {
        return true;
    }
}
```

## ShufflePartitioner

随机分配

```java
public class ShufflePartitioner<T> extends StreamPartitioner<T> {
    private static final long serialVersionUID = 1L;
    private Random random = new Random();

    public ShufflePartitioner() {
    }

    public int selectChannel(SerializationDelegate<StreamRecord<T>> record) {
        return this.random.nextInt(this.numberOfChannels);
    }

    public SubtaskStateMapper getDownstreamSubtaskStateMapper() {
        return SubtaskStateMapper.ROUND_ROBIN;
    }

    public StreamPartitioner<T> copy() {
        return new ShufflePartitioner();
    }

    public boolean isPointwise() {
        return false;
    }

    public String toString() {
        return "SHUFFLE";
    }
}
```