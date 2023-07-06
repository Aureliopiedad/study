# flink cep 研究记录

## pattern 严格连续next和非严格连续followedby

定义输入流为：

```java
@Override
public void run(SourceContext<Integer> sourceContext) throws Exception {
    List<Integer> list = new ArrayList<>();
    list.add(-1);
    list.add(1);
    list.add(-2);
    list.add(2);

    while (i) {
        for (Integer i : list) {
            sourceContext.collect(i);
        }
        Thread.sleep(30000L);
    }
}
```

定义模式流select方式为：

```java
patternStream.select(new PatternSelectFunction<Integer, Integer>() {
            @Override
            public Integer select(Map<String, List<Integer>> pattern) throws Exception {
                for (Map.Entry<String, List<Integer>> entry : pattern.entrySet()) {
                    System.out.println("key:" + entry.getKey() + ", value" + entry.getValue());
                }
                return 0;
            }
        }).print();
```

### followedBy

定义模式序列为：

```java
Pattern<Integer, ?> pattern = Pattern.<Integer>begin("begin").where(
                new IterativeCondition<Integer>() {
                    @Override
                    public boolean filter(Integer value, Context<Integer> ctx) throws Exception {
                        System.out.println("begin 接受参数" + value);
                        return Math.abs(value) == 1;
                    }
                }
        ).followedBy("middle").where(
                new IterativeCondition<Integer>() {
                    @Override
                    public boolean filter(Integer value, Context<Integer> ctx) throws Exception {
                        System.out.println("middle 接受参数" + value);
                        return Math.abs(value) == 2;
                    }
                }
        );
```

followedBy输出为：

```json
begin 接受参数-1
middle 接受参数1
middle 接受参数1
begin 接受参数1
middle 接受参数-2
middle 接受参数-2
middle 接受参数-2
middle 接受参数-2
begin 接受参数-2
key:begin, value[-1]
key:middle, value[-2]
0
key:begin, value[1]
key:middle, value[-2]
0
begin 接受参数2
```

结论：
对于松散连续/非严格连续，输入流 {a1, a2, b1, b2}，对模式序列{a, b}来说，输出为{a1, b1}和{a2, b1}

### next

定义模式序列为：

```java
Pattern<Integer, ?> pattern = Pattern.<Integer>begin("begin").where(
                new IterativeCondition<Integer>() {
                    @Override
                    public boolean filter(Integer value, Context<Integer> ctx) throws Exception {
                        System.out.println("begin 接受参数" + value);
                        return Math.abs(value) == 1;
                    }
                }
        ).next("middle").where(
                new IterativeCondition<Integer>() {
                    @Override
                    public boolean filter(Integer value, Context<Integer> ctx) throws Exception {
                        System.out.println("middle 接受参数" + value);
                        return Math.abs(value) == 2;
                    }
                }
        );
```

next输出为：

```json
begin 接受参数-1
middle 接受参数1
begin 接受参数1
middle 接受参数-2
begin 接受参数-2
key:begin, value[1]
key:middle, value[-2]
0
begin 接受参数2
```

结论：
对于严格连续，输入流{a1, a2, b1, b2}，对于模式序列{a, b}，输出为{a2, b1}
