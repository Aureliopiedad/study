# Flink生成StreamGraph

## StreamGraph的简单介绍

先介绍StreamGraph：

> Class representing the streaming topology. It contains all the information necessary to build the jobgraph for the execution.
>
> StreamGraph是表示一个流拓扑的类，包含为执行构建job图所需的信息。

使用DataStream API开发的应用程序中，会先转换为Transformation，然后被转化为StreamGraph。

StreamGraph是最初最简单的Graph，在后续Flink还会生成JobGraph、ExecutionGraph、物理执行图。

![StramGraph](D:\学习资料\微服务与分布式\flink\flink 作业提交\StreamGraph.png)

## 组件介绍

### StreamNode

StreamNode是StreamGraph的节点，从Transformation转化而来，可以简单理解为一个StreamNode就是一个算子。

由于Flink自带的优化和算子链的存在，所有最终StreamNode的数量和Task数量可能不同。

### StreamEdge

StreamEdge用于连接两个StreamNode，一个StreamNode可以有多个StreamEdge作为出或入；一个StreamEdge中包括了旁路输出、分区器等信息。

由于Flink自带的优化和算子链的存在，所有最终并非全部StreamEdge都会转化为顶点之间的边。

### StreamGraphGenerator

从transformations到StreamGraph的生成器。

## 生成StreamGraph流程一览

生成StreamGraph是在Flink Client中进行的，提交前应该完成。

以下代码基于Flink 版本 1.14.2。

```java
public class StreamExecutionEnvironment {
	...
	public JobExecutionResult execute() throws Exception {
        return execute(getStreamGraph());
    }

    public StreamGraph getStreamGraph() {
        return getStreamGraph(true);
    }
    
    public StreamGraph getStreamGraph(boolean clearTransformations) {
        // 这一步的实际执行过程为：
        // 1.先检查transformations是否为空，为空则报错
        // 2.根据配置和transformations生成一个StreamGraphGenerator
        // 3.根据生成的StreamGraphGenerator调用generate()
        final StreamGraph streamGraph  
            = getStreamGraphGenerator(transformations).generate();
        if (clearTransformations) {
            transformations.clear();
        }
        return streamGraph;
    }
	...
}

public class StreamGraphGenerator {
	...
	public StreamGraph generate() {
		//通过配置文件生成一个初始StreamGraph
		...
		//初始化alreadyTransformed为一个空HashMap
		alreadyTransformed = new HashMap<>();

        //以下是真正的执行代码
		for (Transformation<?> transformation : transformations) {
            transform(transformation);
        }

        streamGraph.setSlotSharingGroupResource(slotSharingGroupResources);

        setFineGrainedGlobalStreamExchangeMode(streamGraph);

        for (StreamNode node : streamGraph.getStreamNodes()) {
            if (node.getInEdges().stream().anyMatch(this::shouldDisableUnalignedCheckpointing)) {
                for (StreamEdge edge : node.getInEdges()) {
                    edge.setSupportsUnalignedCheckpoints(false);
                }
            }
        }

        final StreamGraph builtStreamGraph = streamGraph;

        alreadyTransformed.clear();
        alreadyTransformed = null;
        streamGraph = null;

        return builtStreamGraph;
	}

    private Collection<Integer> transform(Transformation<?> transform) {
        // 当该transformation已经转换完成后，存入map
        if (alreadyTransformed.containsKey(transform)) {
            return alreadyTransformed.get(transform);
        }

        // 进行并行度检查
        if (transform.getMaxParallelism() <= 0) {
            // if the max parallelism hasn't been set, then first use the job wide max parallelism
            // from the ExecutionConfig.
            int globalMaxParallelismFromConfig = executionConfig.getMaxParallelism();
            if (globalMaxParallelismFromConfig > 0) {
                transform.setMaxParallelism(globalMaxParallelismFromConfig);
            }
        }

        transform
            .getSlotSharingGroup()
                .ifPresent(
                    // 暂时不看
                    ...
                );

        // 为了防止transformation无法获取输出类型
        // 当获取类型为MissingTypeInfo时，抛出异常
        transform.getOutputType();

        // 根据Transformation转换为TransformationTranslator
        final TransformationTranslator<?, Transformation<?>> translator =
            (TransformationTranslator<?, Transformation<?>>)translatorMap.get(transform.getClass());

        Collection<Integer> transformedIds;
        if (translator != null) {
            transformedIds = translate(translator, transform);
        } else {
            // 暂时不看
            transformedIds = legacyTransform(transform);
        }

        // need this check because the iterate transformation adds itself before
        // transforming the feedback edges
        if (!alreadyTransformed.containsKey(transform)) {
            alreadyTransformed.put(transform, transformedIds);
        }

        return transformedIds;
    }

    // 
    private Collection<Integer> translate(
        final TransformationTranslator<?, Transformation<?>> translator,
        final Transformation<?> transform) {

        checkNotNull(translator);
        checkNotNull(transform);

        final List<Collection<Integer>> allInputIds = getParentInputIds(transform.getInputs());

        ...
    }
	...
}
```

### Transformation和Translator映射关系

| Transformation | Translator |
| --- | ---|
| OneInputTransformation | OneInputTransformationTranslator |
| TwoInputTransformation | TwoInputTransformationTranslator |
| MultipleInputTransformation | MultiInputTransformationTranslator |
| KeyedMultipleInputTransformation | MultiInputTransformationTranslator |
| SourceTransformation | SourceTransformationTranslator |
| SinkTransformation | SinkTransformationTranslator |
| LegacySinkTransformation | LegacySinkTransformationTranslator |
| LegacySourceTransformation | LegacySourceTransformationTranslator |
| UnionTransformation | UnionTransformationTranslator |
| PartitionTransformation | PartitionTransformationTranslator |
| SideOutputTransformation | SideOutputTransformationTranslator |
| ReduceTransformation | ReduceTransformationTranslator |

## 源码

先来大致看下StreamGraph类：

```java
public class StreamGraph implements Pipeline {
	...
    private String jobName;
    private final ExecutionConfig executionConfig;
    private final CheckpointConfig checkpointConfig;
    ...
    private Map<Integer, StreamNode> streamNodes;
    private Set<Integer> sources;
    private Set<Integer> sinks;
    ...
}
```

还有StreamNode类：

```java
public class StreamNode {
    private final int id;
    private int parallelism;
    private int maxParallelism;
    private ResourceSpec minResources;
    private ResourceSpec preferredResources;
    ...
    private StreamOperatorFactory<?> operatorFactory;
    ...
    private List<StreamEdge> inEdges;
    private List<StreamEdge> outEdges;
    ...
    private String transformationUID;
    ...
}
```

程序入口在org.apache.flink.streaming.api.environment.StreamExecutionEnvironment中：

```java
public class StreamExecutionEnvironment {
	...
	public JobExecutionResult execute() throws Exception {
        return this.execute(this.getJobName());
    }

    public JobExecutionResult execute(String jobName) throws Exception {
        ...
        return this.execute(this.getStreamGraph(jobName));
    }

    @Internal
    public JobExecutionResult execute(StreamGraph streamGraph) throws Exception {
    	//调用executeAsync(StreamGraph)
    	...
    }
    
    public StreamGraph getStreamGraph(String jobName) {
        return this.getStreamGraph(jobName, true);
    }
    
    public StreamGraph getStreamGraph(String jobName, boolean clearTransformations) {
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
        	//1.11 无此配置
            RuntimeExecutionMode executionMode = (RuntimeExecutionMode)this.configuration.get(ExecutionOptions.RUNTIME_MODE);
            return (
            	//通过构造方法加载全部算子，执行环境配置，检查点
                new StreamGraphGenerator(this.transformations, this.config, this.checkpointCfg, this.getConfiguration()))
                //执行模式 1.11暂无
                .setRuntimeExecutionMode(executionMode)
                //状态后端 默认null
                .setStateBackend(this.defaultStateBackend)
                //保存点目录 1.11暂无
                .setSavepointDir(this.defaultSavepointDirectory)
                //是否允许算子链 默认true
                .setChaining(this.isChainingEnabled)
                //存储目录 集群和本地目录不同
                .setUserArtifacts(this.cacheFile)
                //时间模式 1.11在不配置时为process time，1.14默认为event time
                .setTimeCharacteristic(this.timeCharacteristic)
                //网络缓冲超时时间 默认-1 不允许
                .setDefaultBufferTimeout(this.bufferTimeout);
        }
    }
    ...
}
```

如上，在程序检测到StreamExecutionEnvironment#execute()方法后，会逐步调用，在StreamExecutionEnvironment#execute(String)时调用getStreamGraph(String)，然后getStreamGraph(String, boolean)，在getStreamGraphGenerator()中生成StreamGraphGenerator，执行StreamGraphGenerator#generate()。

传入的参数中，List<StreamTransformation<? >> transformations代表的是每次进行的操作符(算子)。

对应的StreamGraphGenerator类在org.apache.flink.streaming.api.graph中：

```java
public class StreamGraphGenerator {
    ...
    //键为不同转化种类，值为该种类转化的转化器。例如SourceTransformation.class，new SourceTransformationTranslator()
    private static final Map<Class<? extends Transformation>, TransformationTranslator<?, ? extends Transformation>> translatorMap;
    //记录算子是否被记录，value是对应的算子转换后集合
    private Map<Transformation<?>, Collection<Integer>> alreadyTransformed;
    private StreamGraph streamGraph;
	...
        
	public StreamGraph generate() {
		//字段含义见上文
        this.streamGraph = new StreamGraph(this.executionConfig, this.checkpointConfig, this.savepointRestoreSettings);
        this.shouldExecuteInBatchMode = this.shouldExecuteInBatchMode(this.runtimeExecutionMode);
        this.configureStreamGraph(this.streamGraph);
        //初始化alreadyTransformed
        this.alreadyTransformed = new HashMap();
        
        //1.11版本不使用迭代器，直接for读取
        //使用迭代器读取全部算子，执行transform(Transformation<?>)方法
        Iterator var1 = this.transformations.iterator();
        while(var1.hasNext()) {
            Transformation<?> transformation = (Transformation)var1.next();
            this.transform(transformation);
        }

        //1.11 和 1.14 在本处的语句有很多不同，此处为1.11版本代码
        final StreamGraph builtStreamGraph = streamGraph;

		alreadyTransformed.clear();
		alreadyTransformed = null;
		streamGraph = null;

		return builtStreamGraph;
    }
    
    //将transform转化为id
    //本方法实际上会连续调用，会将当前transfrom的input再次执行，input也可能有input
    //当执行到source的transform：LegacySourceTransformation时，停止调用
    private Collection<Integer> transform(Transformation<?> transform) {
        if (this.alreadyTransformed.containsKey(transform)) {
            //当存在键时(使用hashCode判断，hash对象包括id name 输出类型 并行度 缓冲超时时间)
            return (Collection)this.alreadyTransformed.get(transform);
        } else {
            //当不存在时
            LOG.debug("Transforming " + transform);
            //非法并行度检查
            if (transform.getMaxParallelism() <= 0) {
            	//当没有配置最大并行度时，采取job全局最大并行度
                int globalMaxParallelismFromConfig = this.executionConfig.getMaxParallelism();
                if (globalMaxParallelismFromConfig > 0) {
                    transform.setMaxParallelism(globalMaxParallelismFromConfig);
                }
            }
			
            //检查输出值类型，当类型异常时，会抛出InvalidTypesException
            transform.getOutputType();
            
            //在1.14版本中，获取对应的TransformationTranslator；在1.11版本中获取transformid，以下是1.11
            //在本版本中，列出全部的transform类型，获取ide
            Collection<Integer> transformedIds;
            if (transform instanceof OneInputTransformation<?, ?>) {
                transformedIds = transformOneInputTransform((OneInputTransformation<?, ?>) transform);
            } else if (transform instanceof TwoInputTransformation<?, ?, ?>) {
                transformedIds = transformTwoInputTransform((TwoInputTransformation<?, ?, ?>) transform);
            } else if (transform instanceof AbstractMultipleInputTransformation<?>) {
                transformedIds = transformMultipleInputTransform((AbstractMultipleInputTransformation<?>) transform);
            } else if (transform instanceof SourceTransformation) {
                transformedIds = transformSource((SourceTransformation<?>) transform);
            } else if (transform instanceof LegacySourceTransformation<?>) {
            	//(见后文)，理论上graph添加的第一个是source
                transformedIds = transformLegacySource((LegacySourceTransformation<?>) transform);
            } else if (transform instanceof SinkTransformation<?>) {
                transformedIds = transformSink((SinkTransformation<?>) transform);
            } else if (transform instanceof UnionTransformation<?>) {
                transformedIds = transformUnion((UnionTransformation<?>) transform);
            } else if (transform instanceof SplitTransformation<?>) {
                transformedIds = transformSplit((SplitTransformation<?>) transform);
            } else if (transform instanceof SelectTransformation<?>) {
                transformedIds = transformSelect((SelectTransformation<?>) transform);
            } else if (transform instanceof FeedbackTransformation<?>) {
                transformedIds = transformFeedback((FeedbackTransformation<?>) transform);
            } else if (transform instanceof CoFeedbackTransformation<?>) {
                transformedIds = transformCoFeedback((CoFeedbackTransformation<?>) transform);
            } else if (transform instanceof PartitionTransformation<?>) {
                //分区transform
                transformedIds = transformPartition((PartitionTransformation<?>) transform);
            } else if (transform instanceof SideOutputTransformation<?>) {
                transformedIds = transformSideOutput((SideOutputTransformation<?>) transform);
            } else {
                throw new IllegalStateException("Unknown transformation: " + transform);
            }

            //将transform转换为id后存入map，之后直接读取
            if (!alreadyTransformed.containsKey(transform)) {
                alreadyTransformed.put(transform, transformedIds);
            }

            if (transform.getBufferTimeout() >= 0) {
                streamGraph.setBufferTimeout(transform.getId(), transform.getBufferTimeout());
            } else {
                //defaultBufferTimeout -1
                streamGraph.setBufferTimeout(transform.getId(), defaultBufferTimeout);
            }
			
            //在不设置的情况下，uid == null
            if (transform.getUid() != null) {
                //通过Map<Integer, StreamNode>实现
                streamGraph.setTransformationUID(transform.getId(), transform.getUid());
            }
            
            if (transform.getUserProvidedNodeHash() != null) {
                streamGraph.setTransformationUserHash(transform.getId(), transform.getUserProvidedNodeHash());
            }
			
            //当不允许自动生成uid时
            if (!streamGraph.getExecutionConfig().hasAutoGeneratedUIDsEnabled()) {
                if (transform instanceof PhysicalTransformation &&
                        transform.getUserProvidedNodeHash() == null &&
                        transform.getUid() == null) {
                    throw new IllegalStateException("Auto generated UIDs have been disabled " +
                        "but no UID or hash has been assigned to operator " + transform.getName());
                }
            }
			
            //MinResources有初始值UNKNOWN，为全null；PreferredResources有初始值UNKNOWN，也是全null
            if (transform.getMinResources() != null && transform.getPreferredResources() != null) {
                streamGraph.setResources(transform.getId(), transform.getMinResources(), transform.getPreferredResources());
            }
			
            //对托管内存的依赖权重，默认1；仅对UNKNOWN使用
            streamGraph.setManagedMemoryWeight(transform.getId(), transform.getManagedMemoryWeight());

            return transformedIds;
        }
    }
    
    //获取共享slot组
    private String determineSlotSharingGroup(String specifiedGroup, Collection<Integer> inputIds) {
        if (specifiedGroup != null) {
        	//当source中存在用户定义的组时，直接返回
            return specifiedGroup;
        } else {
            String inputGroup = null;
            //冷知识，在1.11中，直接采取for，而不是迭代器，对于source有关的transform，inputId是空
            Iterator var4 = inputIds.iterator();

            while(var4.hasNext()) {
                int id = (Integer)var4.next();
                String inputGroupCandidate = this.streamGraph.getSlotSharingGroup(id);
                if (inputGroup == null) {
                    inputGroup = inputGroupCandidate;
                } else if (!inputGroup.equals(inputGroupCandidate)) {
                    return "default";
                }
            }
			
			//flink给一个默认的组，名为default
            return inputGroup == null ? "default" : inputGroup;
        }
    }
}
```

上文中说到了众多transform，这里举例说明：

第一个例子是source转换：

```java
	//source代表数据源的转换
	private <T> Collection<Integer> transformLegacySource(LegacySourceTransformation<T> source) {
        //获取共享slot组，默认情况都是default
		String slotSharingGroup = determineSlotSharingGroup(source.getSlotSharingGroup(), Collections.emptyList());
		
        //对source来说，不存在input
        
		streamGraph.addLegacySource(source.getId(),
				slotSharingGroup,
				source.getCoLocationGroupKey(),//不知道是什么
				source.getOperatorFactory(),//算子工厂
				null,输入类型，对于source来说输入null
				source.getOutputType(),//输入类型
				"Source: " + source.getName());//算子名
		if (source.getOperatorFactory() instanceof InputFormatOperatorFactory) {
            //对于source来说，这一步通过factory生成了一个实现InputFormat接口的对象。该对象具有生成数据源、并行拆分数据源等能力。
			streamGraph.setInputFormat(source.getId(),
					((InputFormatOperatorFactory<T>) source.getOperatorFactory()).getInputFormat());
		}
        //判断source并行度是否等于-1，若不等则使用source的并行度
		int parallelism = source.getParallelism() != ExecutionConfig.PARALLELISM_DEFAULT ?
			source.getParallelism() : executionConfig.getParallelism();
        //为每个算子赋予并行度 通过Map<Integer, StreamNode>实现
		streamGraph.setParallelism(source.getId(), parallelism);
        //为每个算子赋予最大并行度 通过Map<Integer, StreamNode>实现
		streamGraph.setMaxParallelism(source.getId(), source.getMaxParallelism());
        //返回包含唯一值的set
		return Collections.singleton(source.getId());
	}
```

第二个例子是partition：

```java
	//partition代表数据在进行分区
	private <T> Collection<Integer> transformPartition(PartitionTransformation<T> partition) {
		Transformation<T> input = partition.getInput();
		List<Integer> resultIds = new ArrayList<>();
		
        //如上文所说，会将transform中的input先进行转换
		Collection<Integer> transformedIds = transform(input);
		for (Integer transformedId: transformedIds) {
            //static的counter，初始为0，调用++
			int virtualId = Transformation.getNewNodeId();
            //第一个参数代表被链接的transform id
            //第二个参数是虚拟id
            //第三个参数是分区器，比如hash
            //第四个参数是传输类型：流/批/默认流
            //在其中通过Map<Integer, Tuple3<>>记录虚拟id，被链接transformid、分区、传输类型
			streamGraph.addVirtualPartitionNode(
					transformedId, virtualId, partition.getPartitioner(), partition.getShuffleMode());
			resultIds.add(virtualId);
		}

		return resultIds;
	}
```

第三个例子是oneInputTransform：

```java
	private <IN, OUT> Collection<Integer> transformOneInputTransform(OneInputTransformation<IN, OUT> transform) {

		Collection<Integer> inputIds = transform(transform.getInput());

		// the recursive call might have already transformed this
		if (alreadyTransformed.containsKey(transform)) {
			return alreadyTransformed.get(transform);
		}

		String slotSharingGroup = determineSlotSharingGroup(transform.getSlotSharingGroup(), inputIds);

        //在Map<Integer, StreamNode>中添加对应id
		streamGraph.addOperator(transform.getId(),
				slotSharingGroup,
				transform.getCoLocationGroupKey(),
				transform.getOperatorFactory(),
				transform.getInputType(),
				transform.getOutputType(),
				transform.getName());
		
        //在不指定分键操作时，flink默认提供一个NullByteKeySelector，实际上是为每条数据提供一个相同的键
		if (transform.getStateKeySelector() != null) {
			TypeSerializer<?> keySerializer = transform.getStateKeyType().createSerializer(executionConfig);
			streamGraph.setOneInputStateKey(transform.getId(), transform.getStateKeySelector(), keySerializer);
		}

		int parallelism = transform.getParallelism() != ExecutionConfig.PARALLELISM_DEFAULT ?
			transform.getParallelism() : executionConfig.getParallelism();
        //为每个算子赋予并行度 通过Map<Integer, StreamNode>实现
		streamGraph.setParallelism(transform.getId(), parallelism);
        //为每个算子赋予最大并行度 通过Map<Integer, StreamNode>实现
		streamGraph.setMaxParallelism(transform.getId(), transform.getMaxParallelism());

		for (Integer inputId: inputIds) {
            //在不考虑优化和断链的情况下，一条边作为链接两个顶点的连线
            //这一步会添加输入边和输出边
			streamGraph.addEdge(inputId, transform.getId(), 0);
		}

		return Collections.singleton(transform.getId());
	}
```

最后一个例子是sinkTransform：

```java
	private <T> Collection<Integer> transformSink(SinkTransformation<T> sink) {

		Collection<Integer> inputIds = transform(sink.getInput());

		String slotSharingGroup = determineSlotSharingGroup(sink.getSlotSharingGroup(), inputIds);
		
        //addSink和addOperator的区别在于，addSink最后会将sink算子的id放入名为sinks的Set中
		streamGraph.addSink(sink.getId(),
				slotSharingGroup,
				sink.getCoLocationGroupKey(),
				sink.getOperatorFactory(),
				sink.getInput().getOutputType(),
				null,
				"Sink: " + sink.getName());

		StreamOperatorFactory operatorFactory = sink.getOperatorFactory();
		if (operatorFactory instanceof OutputFormatOperatorFactory) {
            //此处设置一个OutputFormat，作用是消耗事件的输出，描述了如何存储最终记录
			streamGraph.setOutputFormat(sink.getId(), ((OutputFormatOperatorFactory) operatorFactory).getOutputFormat());
		}

		int parallelism = sink.getParallelism() != ExecutionConfig.PARALLELISM_DEFAULT ?
			sink.getParallelism() : executionConfig.getParallelism();
		streamGraph.setParallelism(sink.getId(), parallelism);
		streamGraph.setMaxParallelism(sink.getId(), sink.getMaxParallelism());

		for (Integer inputId: inputIds) {
			streamGraph.addEdge(inputId,
					sink.getId(),
					0
			);
		}

		if (sink.getStateKeySelector() != null) {
			TypeSerializer<?> keySerializer = sink.getStateKeyType().createSerializer(executionConfig);
			streamGraph.setOneInputStateKey(sink.getId(), sink.getStateKeySelector(), keySerializer);
		}

        //sink算子返回空列表
		return Collections.emptyList();
	}
```