# Siddhi 基础

> 以下说明基于Siddhi 5.1版本：
> [Siddhi 5.1](https://siddhi.io/en/v5.1/docs/#!)

## Siddhi 介绍

Siddhi是一个流处理(Stream processing)和复杂事件处理(CEP)平台。支持Java、Python应用，能部署到docker和k8s平台。

## Siddhi 必要组件

- 资源：最低内存128M，推荐内存500M，根据处理的数据需要更高的内存；推荐2核
- JDK 8 or 11
- Maven 3.0.4+

## Siddhi SQL

Siddhi允许将事件消费、处理、集成和发布的逻辑编写为SQL脚本，即[Siddhi Streaming SQL](https://siddhi.io/en/v5.1/docs/query-guide/)。

一个简单的Siddhi SQL demo如下：

```shell
@app:name('Temperature-Analytics')

define stream TempStream (deviceID long, roomNo int, temp double);

@info(name = '5minAvgQuery')
from TempStream#window.time(5 min)
select roomNo, avg(temp) as avgTemp
  group by roomNo
insert into OutputStream;
```

一个Siddhi App通常以'@App:name()'开头，这个注解命名了这个Siddhi App，如果某个App没有填写，那么将会是某个随机的UUID。

下面说明一些常见的Siddhi SQL关键字：

| Elements              | Description                                                                            |
| --------------------- | -------------------------------------------------------------------------------------- |
| Stream                | 一系列按时间排序的逻辑事件，具有唯一识别名称和定义好的一组类型化属性。                                                    |
| Event                 | 事件是和Stream关联的单个事件对象，Stream的所有事件都包含一个时间戳和一组与Stream相同的类型化属性。                             |
| Table                 | 定义好的数据的存储，可以是in-memory的，也可以借助外部存储。可以在运行时访问和操作这些Table。                                  |
| Name-Window           | 定义好的数据和拥有逐出策略的存储，窗口数据存储在内存中，并由窗口自动清理。其他Siddhi elements只能查询，无法改变这些数据。                   |
| Named-Aggregation     | 一种数据的结构化表示，以定义好的数据结构和聚合粒度递增聚合和存储。能存储在内存或外部存储中。其他Siddhi elements只能查询，无法改变这些数据。          |
| Query                 | 通过使用来自一个或多个Stream、Table、Window或Aggregation的数据，以流的方式处理事件，并将输出事件发送到Stream、Table或Window中。 |
| Source                | 从外部源获取不同格式的事件，并将其转化为Siddhi event，并传递到流中进行处理。                                           |
| Sink                  | 将事件映射到预定义的数据格式并将它们发送到外部。                                                               |
| Input Handler         | 允许用户将事件注入某个流。                                                                          |
| Stream/Query Callback | 使用来自流或查询的输出事件的机制。                                                                      |
| Partition             | 根据从事件派生的分区键隔离查询。                                                                       |
| Inner Stream          | 用于将分区内的分区查询连接起来。                                                                       |

### Stream

流(Stream)是按事件排序的一系列事件(Event)。一个流的定义包括：流名称和一组属性，这些属性具有特定类型和流内唯一名称，与流关联的所有事件都具有相同的形式。

一个流定义的模板为：

```shell
# 如果stream name或attribute name不符合[a-zA-Z_][a-zA-Z_0-9]*，需要为其添加'`'，例如`$test(0)`
define stream <stream name> (<attribute name> <attribute type>, <attribute name> <attribute type>, ...);
```

模板说明如下：

| parameter      | description                                           |
| -------------- | ----------------------------------------------------- |
| stream name    | 流名称，推荐使用驼峰命名。                                         |
| attribute name | 流内属性的唯一标识符，推荐使用驼峰命名。                                  |
| attribute type | 属性的类型，大致可分为：STRING、INT、LONG、DOUBLE、FLOAT、BOOL、OBJECT。 |

如果想让流以异步或多线程的方式运行，需要添加注解：[@async](https://siddhi.io/en/v5.1/docs/query-guide/#threading-and-synchronization)。

### Value

| type   | description                                            |
| ------ | ------------------------------------------------------ |
| int    | 类似13，-75等。                                             |
| long   | 类似123L，-75L等。                                          |
| float  | 类似123.0f，-75.0e-10F等。                                  |
| double | 类似123.0,123.0D,等。                                      |
| bool   | true, false, TRUE, FALSE。                              |
| string | 有三种形式：' aaa ', " aaa, 'bb' ", """ aaa, 'bb', "cc" """。 |
| time   | Time是一种特殊的long，以数字+单位的形式存在，被执行时会被转化为毫秒的long。           |

| time unit    | description                 |
| ------------ | --------------------------- |
| Year         | year or years               |
| Month        | month or months             |
| Week         | week or weeks               |
| Day          | day or days                 |
| Hour         | hour or hours               |
| Minutes      | minute or minutes or min    |
| Seconds      | second or seconds or sec    |
| Milliseconds | millisecond or milliseconds |

举例，一小时二十五分钟可以写为：

```shell
1 hour and 25 minutes
```

在执行时，会被转化为5100000。

### Source

源通过各种传输组件和各种数据格式接受事件，并将事件定向到流中做处理。源的配置允许定义映射，以便将各种格式的数据转化为Siddhi事件。默认状态下，Siddhi假定到达的事件基于流的预定义格式。

要配置通过源使用事件的流，需要添加注解：@source：

```shell
@source(type='<source type>', <static.key>='<value>', <static.key>='<value>',
    @map(type='<map type>', <static.key>='<value>', <static.key>='<value>',
        @attributes( <attribute1>='<attribute mapping>', <attributeN>='<attribute mapping>')
    )
)
define stream <stream name> (<attribute1> <type>, <attributeN> <type>);
```

说明如下：

| parameter     | description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| ------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| source type   | 定义了接受数据的类型，[In-memory](https://siddhi.io/en/v5.1/docs/api/latest/#inmemory-source)，[HTTP](https://siddhi-io.github.io/siddhi-io-http/)，[Kafka](https://siddhi-io.github.io/siddhi-io-kafka/)，[TCP](https://siddhi-io.github.io/siddhi-io-tcp/)，[Email](https://siddhi-io.github.io/siddhi-io-jms/)，[JMS](https://siddhi-io.github.io/siddhi-io-jms/)，[File](https://siddhi-io.github.io/siddhi-io-file/)，[CDC](https://siddhi-io.github.io/siddhi-io-cdc/)，[Prometheus](https://siddhi-io.github.io/siddhi-io-prometheus/)。                                                                        |
| source mapper | 每个source配置都可以有一个@map表示的映射，定义了如何将传入的数据转变为Siddhi事件。@attributes可以和@map一起使用，可以有选择的从传入的数据中提取数据并分配给某个属性。Siddhi目前支持的映射关系有：[PassThrough](https://siddhi.io/en/v5.1/docs/api/latest/#passthrough-source-mapper)，[JSON](https://siddhi-io.github.io/siddhi-map-json/)，[XML](https://siddhi-io.github.io/siddhi-map-xml/)，[TEXT](https://siddhi-io.github.io/siddhi-map-text/)，[Avro](https://siddhi-io.github.io/siddhi-map-avro/)，[Binary](https://siddhi-io.github.io/siddhi-map-binary/)，[Key Value](https://siddhi-io.github.io/siddhi-map-keyvalue/)，[CSV](https://siddhi-io.github.io/siddhi-map-csv/) |

这里提供两个例子：

#### 第一个例子

现在假定'http://0.0.0.0:8080/foo'将返回如下格式的返回值：

```json
{
   "event":{
       "name":"Paul",
       "age":20,
       "country":"UK"
   }
}
```

将这个http请求作为source，并传入某个stream：

```shell
@source(type='http', receiver.url='http://0.0.0.0:8080/foo',
  @map(type='json'))
define stream InputStream (name string, age int, country string);
```

下面开始解释这个例子：

首先介绍以下Http这个source，[Http(source)](https://siddhi-io.github.io/siddhi-io-http/api/2.3.6/#http-source)通过http或https请求某个POST接口，接收的数据格式支持text、json和xml，支持基本的认证。这里的receiver.url指的是发送出事件的接口，如果想开启SSL，需要使用https协议。

观察这个source定义的stream，并没有在@map下添加@attributes，那么Siddhi默认http请求得到的json数据能够映射到定义的stream上。

#### 第二个例子

对比第一个例子而言，http请求的返回值会显得比较复杂：

```json
{
  "portfolio":{
    "stock":{
      "volume":100,
      "company":{
        "symbol":"FB"
      },
      "price":55.6
    }
  }
}
```

将这个http请求作为source，并传入某个stream：

```shell
@source(type='http', receiver.url='http://0.0.0.0:8080/foo',
  @map(type='json', enclosing.element="$.portfolio",
    @attributes("stock.company.symbol", "stock.price", "stock.volume")))
define stream StockStream (symbol string, price float, volume long);
```

在[json](https://siddhi-io.github.io/siddhi-map-json/api/latest/#json-source-mapper)的映射中，有一个属性：enclosing.element，这个属性代表在嵌套json中选择封闭元素，封闭元素下的所有子元素被视为事件。

### sink

和source相反，从流中消费事件并以各种格式发送到外部。和@source相对，也存在一个@sink：

```shell
@sink(type='<sink type>', <static.key>='<value>', <dynamic.key>='{{<value>}}',
    @map(type='<map type>', <static.key>='<value>', <dynamic.key>='{{<value>}}',
        @payload('<payload mapping>')
    )
)
define stream <stream name> (<attribute1> <type>, <attributeN> <type>);
```

和@source不同的是，在@sink中，可以使用'{{...}}'使用stream的属性值。

| parameter   | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| ----------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| sink type   | [in-memory](https://siddhi.io/en/v5.1/docs/api/latest/#inmemory-sink)，[log](https://siddhi.io/en/v5.1/docs/api/latest/#log-sink)，[http](https://siddhi-io.github.io/siddhi-io-http/)，[kafka](https://siddhi-io.github.io/siddhi-io-kafka/)，[tcp](https://siddhi-io.github.io/siddhi-io-tcp/)，[email](https://siddhi-io.github.io/siddhi-io-email/)，[jms](https://siddhi-io.github.io/siddhi-io-jms/)，[file](https://siddhi-io.github.io/siddhi-io-file/)，[Prometheus](https://siddhi-io.github.io/siddhi-io-prometheus/)    |
| sink mapper | @map和@payload一起使用，达到将事件映射的目的。Siddhi目前支持的映射关系有：[PassThrough](https://siddhi.io/en/v5.1/docs/api/latest/#passthrough-sink-mapper)，[JSON](https://siddhi-io.github.io/siddhi-map-json/)，[XML](https://siddhi-io.github.io/siddhi-map-xml/)，[TEXT](https://siddhi-io.github.io/siddhi-map-text/)，[Avro](https://siddhi-io.github.io/siddhi-map-avro/)，[Binary](https://siddhi-io.github.io/siddhi-map-binary/)，[Key Value](https://siddhi-io.github.io/siddhi-map-keyvalue/)，[CSV](https://siddhi-io.github.io/siddhi-map-csv/) |

和@source不同的是，@sink提供了一种分布式输出，允许使用负载均衡或分区策略将事件发送到多个端点，任何sink都能作为分布式sink来使用。一般使用比较多的有roundRobin和partitioned：

#### roundRobin

```shell
@sink(type='<sink type>', <common.static.key>='<value>', <common.dynamic.key>='{{<value>}}',
    @map(type='<map type>', <static.key>='<value>', <dynamic.key>='{{<value>}}',
        @payload('<payload mapping>')
    )
    @distribution(strategy='roundRobin',
        @destination(<destination.specific.key>='<value>'),
        @destination(<destination.specific.key>='<value>')))
define stream <stream name> (<attribute1> <type>, <attributeN> <type>);
```

#### partitioned

```shell
@sink(type='<sink type>', <common.static.key>='<value>', <common.dynamic.key>='{{<value>}}',
    @map(type='<map type>', <static.key>='<value>', <dynamic.key>='{{<value>}}',
        @payload('<payload mapping>')
    )
    @distribution(strategy='partitioned', partitionKey='<partition key>',
        @destination(<destination.specific.key>='<value>'),
        @destination(<destination.specific.key>='<value>')))
define stream <stream name> (<attribute1> <type>, <attributeN> <type>);
```

这里给出三个例子：

#### 第一个例子

```shell
@sink(type='http', publisher.url='http://localhost:8005/endpoint',
      method='POST', headers='Accept-Date:20/02/2017',
      basic.auth.enabled='true', basic.auth.username='admin',
      basic.auth.password='admin',
      @map(type='json'))
define stream OutputStream (name string, age int, country string);
```

在这种情况下，推送到接口的数据将是：

```json
{
  "event":{
    "name":"Paul",
    "age":20,
    "country":"UK"
  }
}
```

这个例子中出现的http(sink)参数：

| parameter           | description                           |
| ------------------- | ------------------------------------- |
| publisher.url       | 被推送的接口。                               |
| basic.auth.username | 受基本身份验证的接口的用户名。                       |
| basic.auth.password | 受基本身份验证的接口的密码。                        |
| headers             | 格式为："'<key>:<value>','<key>:<value>'" |
| method              | 接口使用的http method                      |

#### 第二个例子

```shell
@sink(type='http', publisher.url='http://localhost:8005/stocks',
      @map(type='json', validate.json='true', enclosing.element='$.Portfolio',
           @payload("""{"StockData":{ "Symbol":"{{symbol}}", "Price":{{price}} }}""")))
define stream StockStream (symbol string, price float, volume long);
```

在这种情况下，推送到接口的数据将是：

```json
{
  "Portfolio":{
    "StockData":{
      "Symbol":"GOOG",
      "Price":55.6
    }
  }
}
```

| parameter     | description             |
| ------------- | ----------------------- |
| validate.json | 验证json格式，不符合格式的事件不会被发送。 |

#### 第三个例子

```shell
@sink(type='http', method='POST', basic.auth.enabled='true',
      basic.auth.username='admin', basic.auth.password='admin',
      @map(type='json'),
      @distribution(strategy='partitioned', partitionKey='country',
        @destination(publisher.url='http://localhost:8005/endpoint1'),
        @destination(publisher.url='http://localhost:8006/endpoint2')))
define stream OutputStream (name string, age int, country string);
```

这会根据country对事件进行分区，将相同country的事件发送到同一个接口上，同时，输出的事件为：

```json
{
  "event":{
    "name":"Paul",
    "age":20,
    "country":"UK"
  }
}
```

### Error handling

> Errors in Siddhi can be handled at the Streams and in Sinks.
> 意思好像是source不行，待验证

#### stream上的异常处理

按照官方的说法，当出现异常情况时，会记录异常并将异常数据丢弃。在启动@OnError后，可以将异常数据发送到其他地方。

```shell
@OnError(action='action type')
define stream <stream name> (<attribute name> <attribute type>,
                             <attribute name> <attribute type>, ... );
```

action是发生异常时会执行的动作，包括：

| action type | description                                  |
| ----------- | -------------------------------------------- |
| LOG         | 仅记录，删除错误事件。                                  |
| STREAM      | 创建一个故障流并将事件重定向到这个流。创建的故障流将具有基本流定义的属性，以及错误信息。 |

举一个例子：

```shell
@OnError(action='STREAM')
define stream TempStream (deviceID long, roomNo int, temp double);

# 在流名称前加'!'以引用故障流
define stream !TempStream (deviceID long, roomNo int, temp double, _error object);
```

#### sink上的异常处理

按官网的说法，当外部系统不可用或错误的情况时，默认情况下，会记录并丢弃失败的事件。

```shell
@sink(type='<sink type>', on.error='<action type>', <key>='<value>', ...)
define stream <stream name> (<attribute name> <attribute type>,
                             <attribute name> <attribute type>, ... );
```

| action type | description                                  |
| ----------- | -------------------------------------------- |
| LOG         | 仅记录，删除错误事件。                                  |
| STREAM      | 创建一个故障流并将事件重定向到这个流。创建的故障流将具有基本流定义的属性，以及错误信息。 |
| WAIT        | 一直等待到下游成功。                                   |

### Query

使让来自一个或多个流、窗口、表、聚合的事件经过流处理，发送到流、窗口、表、聚合中。

```shell
@info(name = '<query name>')
from <input>
<projection>
<output action>
```

| parameter     | description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| ------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| query name    | 给这个query命名，可选，若无则随机分配。                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| input         | 通过流、窗口、表、聚合定义事件的产出方，并能使用[filter](https://siddhi.io/en/v5.1/docs/query-guide/#filter)，[window](https://siddhi.io/en/v5.1/docs/query-guide/#window)，[stream-functions](https://siddhi.io/en/v5.1/docs/query-guide/#stream-function)，[joins](https://siddhi.io/en/v5.1/docs/query-guide/#join)，[patterns](https://siddhi.io/en/v5.1/docs/query-guide/#pattern)，[sequences](https://siddhi.io/en/v5.1/docs/query-guide/#sequence)                                                                                                                                                                                                      |
| projection    | 通过[select](https://siddhi.io/en/v5.1/docs/query-guide/#select)，[functions](https://siddhi.io/en/v5.1/docs/query-guide/#function)，[aggregate-function](https://siddhi.io/en/v5.1/docs/query-guide/#aggregation-function)，[group by](https://siddhi.io/en/v5.1/docs/query-guide/#group-by)等方法产生输出事件的属性，并通过[having](https://siddhi.io/en/v5.1/docs/query-guide/#having)，[limit & offset](https://siddhi.io/en/v5.1/docs/query-guide/#limit-offset)，[order by](https://siddhi.io/en/v5.1/docs/query-guide/#order-by)，[output rate limiting](https://siddhi.io/en/v5.1/docs/query-guide/#output-rate-limiting)等方法限制输出事件。选填，不填写时即认为全部输出。 |
| output action | 定义需要由流、窗口、表上生成事件执行的输入操作，例如：insert into、update、delete。                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |

### From

所有Query都需要至少一个流或窗口作为输入，只有join query才能通过表、聚合作为第二个输入。流、窗口、表、聚合需要在查询前定义。

```shell
from ((<stream>|<named-window>)<handler>*) ((join (<stream>|<named-window>|<table>|<named-aggregation>)<handler>*)|((,|->)(<stream>|<named-window>)<handler>*)+)?
<projection>
insert into (<stream>|<named-window>|<table>)
```

| parameter | description                                                                                                                                                                                                       |
| --------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| handler   | 处理逻辑，包括[filter](https://siddhi.io/en/v5.1/docs/query-guide/#filter)，[windows](https://siddhi.io/en/v5.1/docs/query-guide/#window)，[stream-functions](https://siddhi.io/en/v5.1/docs/query-guide/#stream-function) |
| join      | [joins](https://siddhi.io/en/v5.1/docs/query-guide/#join)                                                                                                                                                         |
| ,         | [sequences](https://siddhi.io/en/v5.1/docs/query-guide/#sequence)                                                                                                                                                 |
| ->        | [patterns](https://siddhi.io/en/v5.1/docs/query-guide/#pattern)                                                                                                                                                   |

### Insert

允许事件插入流、窗口、表。当插入的流尚未定义时，会自动推断流属性并生成该流。

```shell
from <input>
<projection>
insert into (<stream>|<named-window>|<table>)
```

### Select

在查询中，可以使用select定义输出事件的属性。select可以适配多种场景：

1. 查询特定的一部分属性
   
   ```shell
   from <input stream>
   select <param1>, <param2>
   insert into <output stream>
   ```
2. 查询全部属性：不写select则默认全选
   
   ```shell
   from <input stream>
   select *
   insert into <output stream>
   ```
3. 重命名某个属性
   
   ```shell
   from <input stream>
   select <param1> as <name1>, <param2> as <name2>
   insert into <output stream>
   ```
4. 使用常量作为属性值
   
   ```shell
   from <input stream>
   select <param1> as <name1>, <param2> as <name2>, 1 as constantNum
   insert into <output stream>
   ```
5. 使用逻辑表达式
   
   | operator  | example                         |
   | --------- | ------------------------------- |
   | ()        | (cost + tax) * 0.05             |
   | IS NULL   | deviceID is null                |
   | NOT       | not (price > 10)                |
   | +-*/%     | temp * 9/5 + 32                 |
   | <,<=,>,>= | totalCost >= price * quantity   |
   | ==,!=     | totalCost != price * quantity   |
   | IN        | $param in $tableName            |
   | AND       | temp < 40 and humidity < 40     |
   | OR        | humidity < 40 or humidity >= 60 |

### function

Siddhi中内置了一部分函数，可以使用零个或多个参数，生成单个值作为结果，能作用在任何使用属性的地方。

```shell
(<namespace>:)?<function name>( (<parameter>(, <parameter>)*)? )
```

namespace和function name共同唯一标识一个函数，通常情况下，namespace被省略。输入的参数可以是属性、常量值、其他函数的结果、数学或逻辑表达式的结果或时间值。

这里列举一部分内置函数：

| Inbuilt function                                                                                                            | description                       |
| --------------------------------------------------------------------------------------------------------------------------- | --------------------------------- |
| [eventTimestamp](https://siddhi.io/en/v5.1/docs/api/latest/#eventtimestamp-function)                                        | 返回某个事件的时间戳。                       |
| [currentTimeMillis](https://siddhi.io/en/v5.1/docs/api/latest/#currenttimemillis-function)                                  | 返回当前时间。                           |
| [default](https://siddhi.io/en/v5.1/docs/api/latest/#default-function)                                                      | 返回某个值的默认值，更类似于getOrDefault        |
| [ifThenElse](https://siddhi.io/en/v5.1/docs/api/latest/#ifthenelse-function)                                                | 返回某个条件的结果，类似于三元表达式。               |
| [UUID](https://siddhi.io/en/v5.1/docs/api/latest/#uuid-function)                                                            | 生成一个UUID。                         |
| [cast](https://siddhi.io/en/v5.1/docs/api/latest/#cast-function)                                                            | 强转属性类型，会报错。                       |
| [convert](https://siddhi.io/en/v5.1/docs/api/latest/#convert-function)                                                      | 转化属性类型，convert(45.9, 'int')结果是46。 |
| [coalesce](https://siddhi.io/en/v5.1/docs/api/latest/#coalesce-function)                                                    | 返回第一个非空的输入参数。                     |
| [maximum](https://siddhi.io/en/v5.1/docs/api/latest/#maximum-function)                                                      | 取最大值。                             |
| [minimum](https://siddhi.io/en/v5.1/docs/api/latest/#minimum-function)                                                      | 取最小值。                             |
| [instanceOfBoolean/Double/Float/Integer/Long/String](https://siddhi.io/en/v5.1/docs/api/latest/#instanceofboolean-function) | 检查属性是否属于该类型。                      |
| [createSet](https://siddhi.io/en/v5.1/docs/api/latest/#createset-function)                                                  | 根据输入的参数生成一个HashSet。               |
| [sizeOfSet](https://siddhi.io/en/v5.1/docs/api/latest/#minimum-function)                                                    | 返回HashSet的长度。                     |

其他的函数可以在[这里](https://siddhi.io/en/v5.1/docs/extensions/)找到，一部分扩展的函数见[这里](https://siddhi.io/en/v5.1/docs/extensions/)。

### Filter

过滤器能够过滤到达输入流的事件，能够接受属性、常量、函数和其他产生bool的方法，当过滤器的结果为false时，丢弃该事件。

```shell
from <input stream>[<filter condition>]
select <attribute name>, <attribute name>, ...
insert into <output stream>
```

### Stream function

流函数处理通过输入流或窗口的事件，以生成零个或多个新的事件，这些事件具有一个或多个附加属性。

```shell
from <input stream>#(<namespace>:)?<stream function name>(<parameter>, <parameter>, ... )
select <attribute name>, <attribute name>, ...
insert into <output stream>
```

namespace和stream function name共同标识一个唯一的流函数，在使用内置函数时，namespace可省略。

举一个例子，[pol2Cart](https://siddhi.io/en/v5.1/docs/api/latest/#pol2cart-stream-function)可以通过给定的两个参数计算笛卡尔坐标：

```shell
from PolarStream#pol2Cart(theta, rho)
select x, y 
insert into outputStream ;
```

### window

窗口可以从输入流中捕获事件的一个子集，并根据标准在窗口中保持一段时间。该标准定义了什么时间和什么方式将事件从窗口中抛出。

在一个查询中，一个输入流最多只有一个窗口。

```shell
from <input stream>#window.(<namespace>:)?<window name>(<parameter>, <parameter>, ... )
select <attribute name>, <attribute name>, ...
insert <output event type>? into <output stream>
```

同理，namespace和window name共同标识一个唯一窗口。

这里给出一些内置的窗口：

| Inbuilt function                                                                         | description             |
| ---------------------------------------------------------------------------------------- | ----------------------- |
| [time](https://siddhi.io/en/v5.1/docs/api/latest/#time-window)                           | 滑动时间窗口                  |
| [timeBatch](https://siddhi.io/en/v5.1/docs/api/latest/#timebatch-window)                 | 翻滚时间窗口                  |
| [length](https://siddhi.io/en/v5.1/docs/api/latest/#length-window)                       | 滑动计数窗口                  |
| [lengthBatch](https://siddhi.io/en/v5.1/docs/api/latest/#lengthbatch-window)             | 滚动计数窗口                  |
| [timeLength](https://siddhi.io/en/v5.1/docs/api/latest/#timelength-window)               | 滑动时间计数窗口                |
| [session](https://siddhi.io/en/v5.1/docs/api/latest/#session-window)                     | 根据每次会话保留事件              |
| [batch](https://siddhi.io/en/v5.1/docs/api/latest/#batch-window)                         | 保留最后到达的事件块的事件           |
| [sort](https://siddhi.io/en/v5.1/docs/api/latest/#sort-window)                           | 根据参数保留top-k或bottom-k个事件 |
| [cron](https://siddhi.io/en/v5.1/docs/api/latest/#cron-window)                           | 滚动cron窗口                |
| [externalTime](https://siddhi.io/en/v5.1/docs/api/latest/#externaltime-window)           | 以滑动的方式保留一段时间的事件。        |
| [externalTimeBatch](https://siddhi.io/en/v5.1/docs/api/latest/#externaltimebatch-window) | 以滚动的方式保留一段时间的事件。        |
| [delay](https://siddhi.io/en/v5.1/docs/api/latest/#delay-window)                         | 以滑动的方式以给定的延迟时间输出事件。     |
| [other](https://siddhi.io/en/v5.1/docs/extensions/)                                      | 。。                      |

解释滑动和滚动的区别：

| type                             | description             |
| -------------------------------- | ----------------------- |
| 滑动(sliding)计数：length             | 最近的x个，对每个输入的元素都计算一次并输出  |
| 滚动(tumbling/batch)计数：lengthBatch | 每x个，每x个元素输出一次           |
| 滑动(sliding)时间：time               | 最近的x时间，对每个输入的元素都计算一次并输出 |
| 滚动(tumbling/batch)时间：timeBatch   | 每x时间，每x时间后输出一次          |

使用length和lengthBatch做个实验：

```java
//Siddhi Application
String siddhiApp =
        "" +
                "@App:name('Test') \n" +
                "define stream InputStream(age long);\n" +
                "@info(name = 'query1')\n" +
                "from InputStream#window.length(20)\n" +
                "select max(age) as ageMax, min(age) as ageMin\n" +
                "insert into OutputStream;\n";

//Sending events to Siddhi
for (int i = 0; i < 100; i ++) {
    inputHandler.send(new Object[]{(long) i});
    Thread.sleep(15L);
}
Thread.sleep(1000L);
```

输出为：

```shell
[main] INFO siddhi.test.App - stream result: [Event{timestamp=1686125937585, data=[0, 0], isExpired=false}]
[main] INFO siddhi.test.App - stream result: [Event{timestamp=1686125937607, data=[1, 0], isExpired=false}]
# 中间省略[2, 0] - [28, 9]
[main] INFO siddhi.test.App - stream result: [Event{timestamp=1686125938053, data=[29, 10], isExpired=false}]
# 中间省略[30, 11] - [98, 79]
[main] INFO siddhi.test.App - stream result: [Event{timestamp=1686125939154, data=[99, 80], isExpired=false}]
```

如果使用lengthBatch：

```java
//Siddhi Application
String siddhiApp =
        "" +
                "@App:name('Test') \n" +
                "define stream InputStream(age long);\n" +
                "@info(name = 'query1')\n" +
                "from InputStream#window.lengthBatch(20)\n" +
                "select max(age) as ageMax, min(age) as ageMin\n" +
                "insert into OutputStream;\n";
```

输出为：

```shell
[main] INFO siddhi.test.App - stream result: [Event{timestamp=1686126148419, data=[19, 0], isExpired=false}]
[main] INFO siddhi.test.App - stream result: [Event{timestamp=1686126148735, data=[39, 20], isExpired=false}]
[main] INFO siddhi.test.App - stream result: [Event{timestamp=1686126149045, data=[59, 40], isExpired=false}]
[main] INFO siddhi.test.App - stream result: [Event{timestamp=1686126149356, data=[79, 60], isExpired=false}]
[main] INFO siddhi.test.App - stream result: [Event{timestamp=1686126149666, data=[99, 80], isExpired=false}]
```

### event type

```shell
from <input stream>#window.<window name>(<parameter>, <parameter>, ... )
select <attribute name>, <attribute name>, ...
insert <event type> into <output stream>
```

一般来说，查询出的事件类型分几种：

| event type     | description                    |
| -------------- | ------------------------------ |
| current events | 默认。仅在新事件达到查询时，才输出已存在的事件。       |
| expired events | 仅在事件到达时，才输出已存在的事件。             |
| all events     | 当新事件到达查询时，以及事件从窗口过期时，输出已存在的事件。 |

举一个例子直观的感受event type的区别：

```java
//Siddhi Application
String siddhiApp =
        "" +
                "@App:name('Test') \n" +
                "define stream InputStream(age long);\n" +
                "@info(name = 'query1')\n" +
                "from InputStream#window.lengthBatch(20)\n" +
                "select *\n" +
                "insert current events into OutputStream;\n";

//Sending events to Siddhi
for (int i = 0; i < 100; i ++) {
    inputHandler.send(new Object[]{(long) i});
    Thread.sleep(10L);
}
Thread.sleep(1000L);
```

输出为：

```shell
# 这里省略了，大致就是5个列表
# 0 - 19
# 20 - 39
# 40 - 59
# 60 - 79
# 80 - 99
```

换成expired event：

```java
//Siddhi Application
String siddhiApp =
        "" +
                "@App:name('Test') \n" +
                "define stream InputStream(age long);\n" +
                "@info(name = 'query1')\n" +
                "from InputStream#window.lengthBatch(20)\n" +
                "select *\n" +
                "insert expired events into OutputStream;\n";

//Sending events to Siddhi
for (int i = 0; i < 100; i ++) {
    inputHandler.send(new Object[]{(long) i});
    Thread.sleep(10L);
}
Thread.sleep(1000L);
```

输出为：

```shell
# 这里省略了，大致就是4个列表
# 0 - 19
# 20 - 39
# 40 - 59
# 60 - 79
```

这里可以发现，expired events比current events少一行，因为80 - 99的事件没有过期，无法输出。解决方案如下：

```java
//Sending events to Siddhi
for (int i = 0; i < 120; i ++) {
    inputHandler.send(new Object[]{(long) i});
    Thread.sleep(10L);
}
Thread.sleep(1000L);
```

需要在计数窗口中再添加20(窗口大小)个事件，才能让窗口中累积的事件输出。按照Siddhi的逻辑，当新的缓存队列大于等于计数窗口大小时，才能让旧的队列输出。

### Aggregate function

聚合函数是预配置的聚合操作，可以从多个事件中生成单个结果。聚合函数只能作为select子语句的一部分，当查询包括窗口时，将被约束在一个窗口内的事件；当没有窗口时，将从接收到的第一个事件开始。

```shell
from <input stream>#window.<window name>(<parameter>, <parameter>, ... )
select (<namespace>:)?<aggregate function name>(<parameter>, <parameter>, ... ) as <attribute name>, <attribute2 name>, ...
insert into <output stream>;
```

比较常见的内置函数如下：

| Inbuilt function                                                                             | description    |
| -------------------------------------------------------------------------------------------- | -------------- |
| [sum](https://siddhi.io/en/v5.1/docs/api/latest/#sum-aggregate-function)                     | 计算一组事件的某个值的总和  |
| [count](https://siddhi.io/en/v5.1/docs/api/latest/#count-aggregate-function)                 | 计算一组事件的计数总数    |
| [distinctCount](https://siddhi.io/en/v5.1/docs/api/latest/#distinctcount-aggregate-function) | 计算一组事件的非重复计数总数 |
| [avg](https://siddhi.io/en/v5.1/docs/api/latest/#avg-aggregate-function)                     | 计算平均值          |
| [max](https://siddhi.io/en/v5.1/docs/api/latest/#max-aggregate-function)                     | 查询最大值          |
| [min](https://siddhi.io/en/v5.1/docs/api/latest/#min-aggregate-function)                     | 查询最小值          |
| [maxForever](https://siddhi.io/en/v5.1/docs/api/latest/#maxforever-aggregate-function)       | 不限于单个窗口，查询最大值  |
| [minForever](https://siddhi.io/en/v5.1/docs/api/latest/#minforever-aggregate-function)       | 不限于单个窗口，查询最小值  |
| [stdDev](https://siddhi.io/en/v5.1/docs/api/latest/#stddev-aggregate-function)               | 计算一组值的标准偏差     |
| [and](https://siddhi.io/en/v5.1/docs/api/latest/#and-aggregate-function)                     | 计算一组布尔值的逻辑和    |
| [or](https://siddhi.io/en/v5.1/docs/api/latest/#or-aggregate-function)                       | 计算一组布尔值的逻辑或    |
| [unionSet](https://siddhi.io/en/v5.1/docs/api/latest/#unionset-aggregate-function)           | 通过一组值构造一个Set   |
| [其他](https://siddhi.io/en/v5.1/docs/extensions/)                                             | 。。             |

### Group by

根据一个或多个指定的属性对事件进行分类以执行聚合操作。

```shell
from <input stream>#window.<window name>(...)
select <aggregate function>( <parameter>, <parameter>, ...) as <attribute1 name>, <attribute2 name>, ...
group by <attribute1 name>, <attribute2 name>, ...
insert into <output stream>;
```

group by影响的是聚合函数之前的数据，举例说明：

```java
//Siddhi Application
String siddhiApp =
        "" +
                "@App:name('Test') \n" +
                "define stream InputStream(age long);\n" +
                "@info(name = 'query1')\n" +
                "from InputStream#window.lengthBatch(50)\n" +
                "select age, min(age) as min1, max(age) as max1\n" +
                "insert current events into OutputStream;\n";
//Sending events to Siddhi
for (int i = 0; i < 119; i ++) {
    inputHandler.send(new Object[]{(long) i});
    Thread.sleep(10L);
}
```

在不使用group by语句的时候，输出结果应该是：

```shell
[Event{timestamp=1686204173321, data=[49, 0, 49], isExpired=false}]
[Event{timestamp=1686204174115, data=[99, 50, 99], isExpired=false}]
```

修改Siddhi sql，添加group by操作：

```java
//Siddhi Application
String siddhiApp =
        "" +
                "@App:name('Test') \n" +
                "define stream InputStream(age long);\n" +
                "@info(name = 'query1')\n" +
                "from InputStream#window.lengthBatch(50)\n" +
                "select age, min(age) as min1, max(age) as max1\n" +
                "group by age\n" +
                "insert current events into OutputStream;\n";
```

输出修改为：

```shell
[Event{timestamp=1686204806580, data=[0, 0, 0], isExpired=false},..., Event{timestamp=1686204807354, data=[49, 49, 49], isExpired=false}]
[Event{timestamp=1686204807385, data=[50, 50, 50], isExpired=false},..., Event{timestamp=1686204808162, data=[99, 99, 99], isExpired=false}]
```

可以看出来，进入聚合函数max和min的事件每次只有一个了，因为都按照age区分开了。

为了验证猜想，还有一个例子：

```java
//Siddhi Application
String siddhiApp =
        "" +
                "@App:name('Test') \n" +
                "define stream InputStream(age long);\n" +
                "@info(name = 'query1')\n" +
                "from InputStream#window.lengthBatch(119)\n" +
                "select age, min(age) as min1, max(age) as max1, count(age) as count1\n" +
                "group by age\n" +
                "insert current events into OutputStream;\n";

//Sending events to Siddhi
for (int i = 0; i < 119; i ++) {
    inputHandler.send(new Object[]{(long) Math.abs(60 - i)});
    Thread.sleep(10L);
}
```

如上所示，输入的元素应该是60一个，0一个，1-59两个，但是由于窗口大小119，所有OutputStream接收到的数据应该是60、0、59一个，1-58两个。输出如下：

```shell
[
  Event{timestamp=1686205084725, data=[60, 60, 60, 1], isExpired=false}, 
  Event{timestamp=1686205084754, data=[59, 59, 59, 1], isExpired=false},
  Event{timestamp=1686205086614, data=[58, 58, 58, 2], isExpired=false},
  ...
  Event{timestamp=1686205085714, data=[1, 1, 1, 2], isExpired=false},
  Event{timestamp=1686205085699, data=[0, 0, 0, 1], isExpired=false}
]
```

基本能确定，对窗口输出的元素，先经过group by，然后再到聚合函数。

### Having

使用查询语句输出事件时过滤输出，如果条件为真，则事件输出。

```shell
from <input stream>#window.<window name>( ... )
select <aggregate function>( <parameter>, <parameter>, ...) as <attribute1 name>, <attribute2 name>, ...
group by <attribute1 name>, <attribute2 name> ...
having <condition>
insert into <output stream>;
```

### Order by

针对查询结果做排序。

```shell
from <input stream>#window.<window name>( ... )
select <aggregate function>( <parameter>, <parameter>, ...) as <attribute1 name>, <attribute2 name>, ...
group by <attribute1 name>, <attribute2 name> ...
having <condition>
order by <attribute1 name> (asc|desc)?, <attribute2 name> (asc|desc)?, ...
insert into <output stream>;
```

### Limit & Offsets

通过索引量获取有限数量的事件。

```shell
from <input stream>#window.<window name>( ... )
select <aggregate function>( <parameter>, <parameter>, ...) as <attribute1 name>, <attribute2 name>, ...
group by <attribute1 name>, <attribute2 name> ...
having <condition>
order by <attribute1 name> (asc | desc)?, <attribute2 name> (<ascend/descend>)?, ...
limit <positive integer>?
offset <positive integer>?
insert into <output stream>;
```

- limit：不填则默认选取全部事件。
- offset：默认偏移量0，选取的事件不包括offset指定的事件。

Siddhi的limit和offset可能会导致一些误解，这里举个例子说明一下：

```java
//Siddhi Application
String siddhiApp =
        "" +
                "@App:name('Test') \n" +
                "define stream InputStream(age long, num int);\n" +
                "@info(name = 'query1')\n" +
                "from InputStream#window.lengthBatch(50)\n" +
                "select age, num, min(age) as min1, max(age) as max1\n" +
                "group by num\n" +
                "insert current events into OutputStream;\n";

//Sending events to Siddhi
for (int i = 0; i < 119; i ++) {
    inputHandler.send(new Object[]{(long) i, i % 2});
    Thread.sleep(10L);
}
```

输出为：

```shell
[Event{timestamp=1686207613353, data=[48, 0, 0, 48], isExpired=false}, Event{timestamp=1686207613369, data=[49, 1, 1, 49], isExpired=false}]
[Event{timestamp=1686207614150, data=[98, 0, 50, 98], isExpired=false}, Event{timestamp=1686207614166, data=[99, 1, 51, 99], isExpired=false}]
```

将sql修改为：

```java
//Siddhi Application
String siddhiApp =
        "" +
                "@App:name('Test') \n" +
                "define stream InputStream(age long, num int);\n" +
                "@info(name = 'query1')\n" +
                "from InputStream#window.lengthBatch(50)\n" +
                "select age, num, min(age) as min1, max(age) as max1\n" +
                "group by num\n" +
                "limit 1" +
                "insert current events into OutputStream;\n";
```

输出为：

```shell
[Event{timestamp=1686207683405, data=[48, 0, 0, 48], isExpired=false}]
[Event{timestamp=1686207684202, data=[98, 0, 50, 98], isExpired=false}]
```

将sql修改为：

```java
//Siddhi Application
String siddhiApp =
        "" +
                "@App:name('Test') \n" +
                "define stream InputStream(age long, num int);\n" +
                "@info(name = 'query1')\n" +
                "from InputStream#window.lengthBatch(50)\n" +
                "select age, num, min(age) as min1, max(age) as max1\n" +
                "group by num\n" +
                "limit 1" +
                "offset 1" +
                "insert current events into OutputStream;\n";
```

输出为：

```shell
[Event{timestamp=1686207733748, data=[49, 1, 1, 49], isExpired=false}]
[Event{timestamp=1686207734554, data=[99, 1, 51, 99], isExpired=false}]
```

当offset置为2时，无输出。可见，在窗口中，limit限制的是每个窗口，而不是最终窗口的集合。

### stream processor

流处理器是流函数和窗口函数的组合。直接作用于输入流上，以生成具有零个或多个附加属性的零个或多个事件，同时具有保留和发出事件的能力。

一个内置的流处理器如下：

- [LOG](https://siddhi.io/en/v5.1/docs/api/latest/#log-stream-processor)：记录日志
- [其他](https://siddhi.io/en/v5.1/docs/extensions/)

### Join

根据指定条件实时合并来自两个流的事件。Join可以根据定义的窗口指定两个流的关联方式：流是不能直接连接的，因此，每个流都需要与一个窗口关联，因为窗口可以保存事件。在关联过程中，每个流的每个事件都会根据指定条件和其他窗口中的所有事件进行匹配，并为所有匹配的事件对输出事件。

```shell
from <input stream>(<non window handler>)*(#window.<window name>(<parameter>, ... ))? (as <reference>)? (unidirectional)?
         <join type> <input stream>(<non window handler>)*(#window.<window name>(<parameter>,  ... ))? (as <reference>)? (unidirectional)? 
    (on <join condition>)?
select <reference>.<attribute name>, <reference>.<attribute name>, ...
insert into <output stream>
```

| join type        | description                                                                |
| ---------------- | -------------------------------------------------------------------------- |
| inner join       | 默认。仅当任一流触发连接且两个窗口都存在匹配时间时才生成输出。                                            |
| left outer join  | 将左侧流事件生成到输出。当右侧流触发连接并在左窗口中找到匹配事件时，以及左侧窗口触发连接时，产生输出。若左窗口在右窗口找不到匹配事件时，会以空值代替 |
| right outer join | 基本等同left                                                                   |
| fill outer join  | left和right的结合                                                              |

在正常条件下，任一流的事件到达后都会触发连接操作。但是，可以通过单向关键字来控制连接行为：unidirectional。只有具有单向性的流才能触发连接和生成事件。

### pattern

模式是一种状态机实现，用于检测从一个或多个流到达的事件发生。可以重复匹配模式、计算事件发生次数和使用逻辑排序。类似Flink中的CEP使用方式。

```shell
from (
      (every)? (<event reference>=)?<input stream>[<filter condition>](<<min count>:<max count>>)? |
      (every)? (<event reference>=)?<input stream>[<filter condition>] (and|or) (<event reference>=)?<input stream>[<filter condition>] |
      (every)? not input stream>[<filter condition>] (and <event reference>=<input stream>[<filter condition>] | for <time gap>)
    ) -> ...
    (within <time gap>)?     
select <event reference>.<attribute name>, <event reference>.<attribute name>, ...
insert into <output stream>
```

| item                           | description                                                               |
| ------------------------------ | ------------------------------------------------------------------------- |
| ->                             | 类似于Flink中的follow，指定一个事件将在某个事件之后，不一定紧跟着发生。->左侧是前一个事件满足的条件，->右侧是后一个事件满足的条件。 |
| every                          | 一个可选的关键字，定义何时启动新的模式匹配来重复匹配。不使用时，只会匹配一次。                                   |
| within time gap                | 一个可选的within语句，定义了模式的持续时间。                                                 |
| min count，max count            | 确定在给定条件下应匹配的最小和最大事件数。例如，匹配1-4次<1:4>、小于4次<:4>、大于等于4次<4:>、4次<4>。            |
| and                            | 允许两个条件以任何顺序被两个不同的事件匹配。                                                    |
| or                             | 只需要一个条件被事件满足即可。此时不匹配的事件将被置为空值。                                            |
| not condition1 and condition2  | 在任何匹配condition1的事件之前检测匹配condition2。                                       |
| not condition1 for time period | 在指定的事件跨度上未检测到匹配condition1的事件。                                             |
| event reference                | 访问事件的可选引用。                                                                |

这个说明起来有点抽象，可以看几个具体的例子：

关于event reference，一般以'e[n]'作为第n个事件的引用，定义'e[n]'的过程为：

```shell
e1=$streamName[ condition ]
```

这里的意思是，流里面满足条件的事件被称为e1。当使用min count，max count时，定义如下：

```shell
e2=$streamName[ condition ]<1:>
```

这种情况下，可以使用以下方法调用这些事件：

```shell
# 匹配的第一个事件
e2[0]
# 匹配的第二个事件
e2[1]
...
# 匹配的倒数第二个事件
e2[last - 1]
# 匹配的最后一个事件
e2[last]
```

这里举一个简单的例子：

```java
//Siddhi Application
String siddhiApp =
        "" +
                "@App:name('Test') \n" +
                "define stream InputStream(age long, num int);\n" +
                "from e1=InputStream -> e2=InputStream[ e1.num == num ] within 100 milliseconds\n" +
                "select e1.age as age1, e1.num as num1, e2.age as age2, e2.num as num2\n" +
                "insert into OutputStream;\n";

//Sending events to Siddhi
for (int i = 0; i < 119; i ++) {
    inputHandler.send(new Object[]{(long) i, i % 2});
    Thread.sleep(10L);
}
Thread.sleep(1000L);
```

输出为：

```shell
# 第一个输入为[0,0]，这个值成为e1
# 第二个输入为[1,1]，这个值不满足e2的条件表达式e1.num == num，丢弃
# 第三个输入为[2,0]，这个值满足e2的条件表达式e1.num == num，成为e2
# 因为没有添加every选项，所以只输出一行
[Event{timestamp=1686276449054, data=[0, 0, 2, 0], isExpired=false}]
```

修改Siddhi sql如下：

```java
//Siddhi Application
String siddhiApp =
        "" +
                "@App:name('Test') \n" +
                "define stream InputStream(age long, num int);\n" +
                // every( e1=InputStream ) 和 every e1=InputStream结果一致
                "from every( e1=InputStream ) -> e2=InputStream[ e1.num == num ] within 100 milliseconds\n" +
                "select e1.age as age1, e1.num as num1, e2.age as age2, e2.num as num2\n" +
                "insert into OutputStream;\n";
```

输出为：

```shell
# 逻辑同上一个例子，但是由于every前缀的关系，持续输出到[116, 0, 118, 0]
# 这里其实e1=[117,1]和[118,0]已经在CEP的缓冲池中了，但是没有119的输入，所以没有下一步的输出了
[Event{timestamp=1686276745315, data=[0, 0, 2, 0], isExpired=false}]
[Event{timestamp=1686276745343, data=[1, 1, 3, 1], isExpired=false}]
...
[Event{timestamp=1686276747159, data=[115, 1, 117, 1], isExpired=false}]
[Event{timestamp=1686276747174, data=[116, 0, 118, 0], isExpired=false}]
```

修改Siddhi sql如下：

```java
//Siddhi Application
String siddhiApp =
        "" +
                "@App:name('Test') \n" +
                "define stream InputStream(age long, num int);\n" +
                "from every( e1=InputStream[ age > 115] ) -> e2=InputStream[ e1.num == num and num == 0 ] within 100 milliseconds\n" +
                "select e1.age as age1, e1.num as num1, e2.age as age2, e2.num as num2\n" +
                "insert into OutputStream;\n";
```

输出为：

```shell
[Event{timestamp=1686277133335, data=[116, 0, 118, 0], isExpired=false}]
```

下面测试以下关于[minCount, maxCount]的部分：

```java
//Siddhi Application
String siddhiApp =
        "" +
                "@App:name('Test') \n" +
                "define stream InputStream(age long, num int);\n" +
                "from every e1=InputStream -> e2=InputStream[ e1.num == num ]<1:3>\n" +
                "select e1.age as age1, e1.num as num1, e2.age as age2, e2.num as num2\n" +
                "insert into OutputStream;\n";
```

输出为：

```shell
[Event{timestamp=1686278539281, data=[0, 0, [2], [0]], isExpired=false}]
[Event{timestamp=1686278539296, data=[1, 1, [3], [1]], isExpired=false}]
...
[Event{timestamp=1686278541106, data=[115, 1, [117], [1]], isExpired=false}]
[Event{timestamp=1686278541122, data=[116, 0, [118], [0]], isExpired=false}]
```

替换Siddhi sql如下：

```java
//Siddhi Application
String siddhiApp =
        "" +
                "@App:name('Test') \n" +
                "define stream InputStream(age long, num int);\n" +
                "from every e1=InputStream -> e2=InputStream[ e1.num == num ]<0:3>\n" +
                "select e1.age as age1, e1.num as num1, e2.age as age2, e2.num as num2\n" +
                "insert into OutputStream;\n";
```

输出：

```shell
[Event{timestamp=1686278720424, data=[0, 0, [], []], isExpired=false}]
...
[Event{timestamp=1686278722335, data=[118, 0, [], []], isExpired=false}]
```

可以看的出来，对于[minCount, maxCount]，在后续没有新的模式匹配下，会优先达成最低要求直接输出。明明可以匹配多次，但是由于限定是[0:3]，所以直接是空(只是猜测，需要看源码)。

在后续还有模式匹配的情况下，结果又会不一样：

```java
//Siddhi Application
String siddhiApp =
        "" +
                "@App:name('Test') \n" +
                "define stream InputStream(age long, num int);\n" +
                "from every e1=InputStream[ age > 110 ] -> e2=InputStream[ e1.num == num ]<0:3> -> e3=InputStream[ e1.num == num ]\n" +
                "select e1.age as age1, e1.num as num1, e2.age as age2, e2.num as num2, e3.age as age3, e3.num as num3\n" +
                "insert into OutputStream;\n";
```

如上，在e2后面新加了一个事件匹配，输出的结果是：

```shell
[Event{timestamp=1686279262374, data=[111, 1, [], [], 113, 1], isExpired=false}]
...
[Event{timestamp=1686279262451, data=[116, 0, [], [], 118, 0], isExpired=false}]
```

那这不是和上面的结果一样吗，继续修改：

```java
//Siddhi Application
String siddhiApp =
        "" +
                "@App:name('Test') \n" +
                "define stream InputStream(age long, num int);\n" +
                "from every e1=InputStream[ age > 110 ] -> e2=InputStream[ e1.num == num ]<0:3> -> e3=InputStream[ e2[0].num == num ]\n" +
                "select e1.age as age1, e1.num as num1, e2.age as age2, e2.num as num2, e3.age as age3, e3.num as num3\n" +
                "insert into OutputStream;\n";
```

这里的修改逻辑是把e3的条件依赖于e2数组的第一个结果，这里的输出为：

```shell
[Event{timestamp=1686279494793, data=[111, 1, [113], [1], 115, 1], isExpired=false}]
...
[Event{timestamp=1686279494841, data=[114, 0, [116], [0], 118, 0], isExpired=false}]
```

然后就发现e2有值了，数组长度是1，再修改：

```java
//Siddhi Application
String siddhiApp =
        "" +
                "@App:name('Test') \n" +
                "define stream InputStream(age long, num int);\n" +
                "from every e1=InputStream[ age > 110 ] -> e2=InputStream[ e1.num == num ]<0:3> -> e3=InputStream[ e2[1].num == num ]\n" +
                "select e1.age as age1, e1.num as num1, e2.age as age2, e2.num as num2, e3.age as age3, e3.num as num3\n" +
                "insert into OutputStream;\n";
```

将e2[0]修改为e2[1]，输出结果如下：

```shell
[Event{timestamp=1686279644914, data=[111, 1, [113, 115], [1, 1], 117, 1], isExpired=false}]
[Event{timestamp=1686279644929, data=[112, 0, [114, 116], [0, 0], 118, 0], isExpired=false}]
```

我们把e1的限制条件调低点，多发一些数据，同时在e3也做一些修改：

```java
//Siddhi Application
String siddhiApp =
        "" +
                "@App:name('Test') \n" +
                "define stream InputStream(age long, num int);\n" +
                "from every e1=InputStream[ age > 100 ] -> e2=InputStream[ e1.num == num ]<0:5> -> e3=InputStream[ e2[0].num == num ]\n" +
                "select e1.age as age1, e1.num as num1, e2.age as age2, e2.num as num2, e3.age as age3, e3.num as num3\n" +
                "insert into OutputStream;\n";
```

输出为：

```shell
[Event{timestamp=1686279960409, data=[101, 1, [103], [1], 105, 1], isExpired=false}]
[Event{timestamp=1686279960425, data=[102, 0, [104], [0], 106, 0], isExpired=false}]
...
[Event{timestamp=1686279960600, data=[113, 1, [115], [1], 117, 1], isExpired=false}]
[Event{timestamp=1686279960616, data=[114, 0, [116], [0], 118, 0], isExpired=false}]
```

改进：

```java
//Siddhi Application
String siddhiApp =
        "" +
                "@App:name('Test') \n" +
                "define stream InputStream(age long, num int);\n" +
                "from every e1=InputStream[ age > 100 ] -> e2=InputStream[ e1.num == num ]<0:5> -> e3=InputStream[ e2[4].num == num ]\n" +
                "select e1.age as age1, e1.num as num1, e2.age as age2, e2.num as num2, e3.age as age3, e3.num as num3\n" +
                "insert into OutputStream;\n";
```

输出为：

```shell
[Event{timestamp=1686280114292, data=[101, 1, [103, 105, 107, 109, 111], [1, 1, 1, 1, 1], 113, 1], isExpired=false}]
[Event{timestamp=1686280114308, data=[102, 0, [104, 106, 108, 110, 112], [0, 0, 0, 0, 0], 114, 0], isExpired=false}]
...
[Event{timestamp=1686280114355, data=[105, 1, [107, 109, 111, 113, 115], [1, 1, 1, 1, 1], 117, 1], isExpired=false}]
[Event{timestamp=1686280114371, data=[106, 0, [108, 110, 112, 114, 116], [0, 0, 0, 0, 0], 118, 0], isExpired=false}]
```

到这一步，能看的出来，在后续模式中对e2数组下标的指定会影响到从e2发出来的数据。还有最后一组对比：

```java
//Siddhi Application
String siddhiApp =
        "" +
                "@App:name('Test') \n" +
                "define stream InputStream(age long, num int);\n" +
                "from every e1=InputStream[ age > 100 ] -> e2=InputStream[ e1.num == num ]<0:5> -> e3=InputStream[ e2[1].age >= 110 ]\n" +
                "select e1.age as age1, e1.num as num1, e2.age as age2, e2.num as num2, e3.age as age3, e3.num as num3\n" +
                "insert into OutputStream;\n"; 
```

经过上文的说明，这里的e2输出应该是长度为2的数组，而且也确实如此：

```shell
[Event{timestamp=1686280352401, data=[106, 0, [108, 110], [0, 0], 111, 1], isExpired=false}]
[Event{timestamp=1686280352415, data=[107, 1, [109, 111], [1, 1], 112, 0], isExpired=false}]
...
[Event{timestamp=1686280352494, data=[112, 0, [114, 116], [0, 0], 117, 1], isExpired=false}]
[Event{timestamp=1686280352509, data=[113, 1, [115, 117], [1, 1], 118, 0], isExpired=false}]
```

这时候我们把Siddhi sql修改如下：

```java
//Siddhi Application
String siddhiApp =
        "" +
                "@App:name('Test') \n" +
                "define stream InputStream(age long, num int);\n" +
                "from every e1=InputStream[ age > 100 ] -> e2=InputStream[ e1.num == num ]<0:5> -> e3=InputStream[ e2[last].age >= 110 ]\n" +
                "select e1.age as age1, e1.num as num1, e2.age as age2, e2.num as num2, e3.age as age3, e3.num as num3\n" +
                "insert into OutputStream;\n";
```

不妨先推测以下这个Siddhi sql的结果：

由于给定的min maxCount值是0-5，所以最终输出的每个事件中e2的长度最多为5(不超过5次)，由于最终要求e2[last].age >= 110，所以存在以下这些匹配事件：

```
101, 1, [103, 105, 107, 109, 111], [1, 1, 1, 1, 1], 112, 0
# 如果将e1的条件换成age >= 100，这里就是100, 0, [102, 104, 106, 108, 110], [0, 0, 0, 0, 0], 111, 1
102, 0, [104, 106, 108, 110], [0, 0, 0, 0], 113, 1
```

来看一下最终的输出：

```
[Event{timestamp=1686289340749, data=[102, 0, [104, 106, 108, 110], [0, 0, 0, 0], 111, 1], isExpired=false}, Event{timestamp=1686289340749, data=[104, 0, [106, 108, 110], [0, 0, 0], 111, 1], isExpired=false}, Event{timestamp=1686289340749, data=[106, 0, [108, 110], [0, 0], 111, 1], isExpired=false}, Event{timestamp=1686289340749, data=[108, 0, [110], [0], 111, 1], isExpired=false}]

[Event{timestamp=1686289340779, data=[101, 1, [103, 105, 107, 109, 111], [1, 1, 1, 1, 1], 112, 0], isExpired=false}, Event{timestamp=1686289340779, data=[103, 1, [105, 107, 109, 111], [1, 1, 1, 1], 112, 0], isExpired=false}, Event{timestamp=1686289340779, data=[105, 1, [107, 109, 111], [1, 1, 1], 112, 0], isExpired=false}, Event{timestamp=1686289340779, data=[107, 1, [109, 111], [1, 1], 112, 0], isExpired=false}, Event{timestamp=1686289340779, data=[109, 1, [111], [1], 112, 0], isExpired=false}]

[Event{timestamp=1686289340795, data=[110, 0, [112], [0], 113, 1], isExpired=false}]

[Event{timestamp=1686289340810, data=[111, 1, [113], [1], 114, 0], isExpired=false}]

[Event{timestamp=1686289340826, data=[112, 0, [114], [0], 115, 1], isExpired=false}]

[Event{timestamp=1686289340841, data=[113, 1, [115], [1], 116, 0], isExpired=false}]

[Event{timestamp=1686289340857, data=[114, 0, [116], [0], 117, 1], isExpired=false}]

[Event{timestamp=1686289340873, data=[115, 1, [117], [1], 118, 0], isExpired=false}]
```

说一下我的理解：

1. 由于last并不是一个常量，所以根据Siddhi sql中限制的0-5，last的取值是0-4
2. 当last取值为0时，e2的取值是110、111、112、113、114、115、116、117
3. 当last取值为1时，e2的取值是[108, 110], [109, 111], ..., [114, 116], [115, 117]
4. 当last取值为2时，e2的取值是[106, 108, 110], [107, 109, 111], ..., [112, 114, 116], [113, 115, 117]
5. 当last取值为3时，e2的取值是[104, 106, 108, 110], [105, 107, 109, 111], ..., [110, 112, 114, 116], [111, 113, 115, 117]
6. 当last取值为4时，e2的取值是[103, 105, 107, 109, 111], [104, 106, 108, 110, 112], ..., [108, 110, 112, 114, 116], [109, 111, 113, 115, 117]

但是根据这个逻辑，上面的输出是缺少了很多数据的，所以肯定还有一个逻辑在里面：

1. 按照我在上文猜测的逻辑：以最低满足为优先
2. 当e1=101进入CEP时，last=0、1、2、3都不能输出任何事件，只有last=4时，才能输出e2=[103, 105, 107, 109, 111]
3. 当e1=102进入CEP时，last=0、1、2都不能输出任何事件，last=3时，就能输出e2=[104, 106, 108, 110]；所以last=4就不执行了。
4. 同理，e1=103进入CEP时，也只执行last=3；e1=104，只执行last=2;
5. ...
6. 同理，e1=108时，只执行last=0；当e1>=108时，都只执行last=0。
7. 所以真实输出和推测输出有数量的差距。
8. 至于为什么前两行是列表输出，未知。

为了验证猜想，我们把Siddhi sql修改为：

```java
//Siddhi Application
String siddhiApp =
        "" +
                "@App:name('Test') \n" +
                "define stream InputStream(age long, num int);\n" +
                "from every e1=InputStream[ age > 100 ] -> e2=InputStream[ e1.num == num ]<0:5> -> e3=InputStream[ e2[last].age >= 110 or e2[last].age == 104 ]\n" +
                "select e1.age as age1, e1.num as num1, e2.age as age2, e2.num as num2, e3.age as age3, e3.num as num3\n" +
                "insert into OutputStream;\n";
```

这个修改后的sql，主要是针对e1=102的，只有e1=102才能让e2的列表包含104，输出结果(只看不同点)：

```
# 这是不加or条件的
[Event{timestamp=1686289340749, data=[102, 0, [104, 106, 108, 110], [0, 0, 0, 0], 111, 1], isExpired=false}, Event{timestamp=1686289340749, data=[104, 0, [106, 108, 110], [0, 0, 0], 111, 1], isExpired=false}, Event{timestamp=1686289340749, data=[106, 0, [108, 110], [0, 0], 111, 1], isExpired=false}, Event{timestamp=1686289340749, data=[108, 0, [110], [0], 111, 1], isExpired=false}]

# 这是加了or条件的
[Event{timestamp=1686291947007, data=[102, 0, [104], [0], 105, 1], isExpired=false}]
[Event{timestamp=1686291947102, data=[104, 0, [106, 108, 110], [0, 0, 0], 111, 1], isExpired=false}, Event{timestamp=1686291947102, data=[106, 0, [108, 110], [0, 0], 111, 1], isExpired=false}, Event{timestamp=1686291947102, data=[108, 0, [110], [0], 111, 1], isExpired=false}]
```

由于e1=102且last=0时，e2=[104]就满足了条件，所以直接输出了，last=1、2、3、4都被丢弃。

当然这只是我的理解，真正的逻辑需要看源码。

### Sequence

和Pattern不同的是，需要连续到达的事件才能触发。类似于Flink next的概念。

和Pattern的区别应该是用的不是'->'而是','。

```
from (
      (every)? (<event reference>=)?<input stream>[<filter condition>] (+|*|?)? |
               (<event reference>=)?<input stream>[<filter condition>] (and|or) (<event reference>=)?<input stream>[<filter condition>] |
               not input stream>[<filter condition>] (and <event reference>=<input stream>[<filter condition>] | for <time gap>)
    ), ...
    (within <time gap>)?     
select <event reference>.<attribute name>, <event reference>.<attribute name>, ...
insert into <output stream>
```

| items           | description       |
| --------------- | ----------------- |
| ,               | 前后事件需要连续到达。       |
| every           | 重复触发。             |
| within          | 时间段内              |
| +               | 一个或多个             |
| *               | 零个或多个             |
| ?               | 零个或一个             |
| and             | 逻辑与               |
| or              | 逻辑或               |
| not 1 and 2     | 在任何匹配1的事件之前检查2    |
| not 1 for       | 在指定时间段上未检测到匹配1的事件 |
| event reference | 事件引用              |

一个简单的例子说明一下Pattern和Sequence的区别：

```java
//Siddhi Application
String siddhiApp =
        "" +
                "@App:name('Test') \n" +
                "define stream InputStream(age long, num int);\n" +
                "from e1=InputStream[ age == 0 ] -> e2=InputStream[ e1.num == num ]\n" +
                "select e1.age as age1, e2.age as age2\n" +
                "insert into OutputStream;\n";

//Sending events to Siddhi
for (int i = 0; i < 119; i ++) {
    inputHandler.send(new Object[]{(long) i, i % 2});
    Thread.sleep(10L);
}
Thread.sleep(1000L);
```

输出结果是：

```
[Event{timestamp=1686294584312, data=[0, 2], isExpired=false}]
```

切换为Sequence后：

```java
//Siddhi Application
String siddhiApp =
        "" +
                "@App:name('Test') \n" +
                "define stream InputStream(age long, num int);\n" +
                "from e1=InputStream[ age == 0 ], e2=InputStream[ e1.num == num ]\n" +
                "select e1.age as age1, e2.age as age2\n" +
                "insert into OutputStream;\n";
```

无输出，将[ e1.num == num ]删除后，输出为：

```
[Event{timestamp=1686295117099, data=[0, 1], isExpired=false}]
```

这里举几个例子：

```java
//Siddhi Application
String siddhiApp =
        "" +
                "@App:name('Test') \n" +
                "define stream InputStream(age long, num int);\n" +
                "@info(name = 'test')" +
                "from every e1=InputStream, e2=InputStream[ifThenElse(e2[last].age is null, age < 100, e2[last].age < 100)]+, e3=InputStream[ age > 90 ]\n" +
                "select e1.age as age1, e2.age as age2, e3.age as age3\n" +
                "insert into OutputStream;\n";

//Sending events to Siddhi
for (int i = 0; i < 119; i ++) {
    inputHandler.send(new Object[]{(long) i, i % 2});
    Thread.sleep(10L);
}
Thread.sleep(1000L);
```

输出为：

```
[Event{timestamp=1686298507196, data=[0, [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90], 91], isExpired=false}]

[Event{timestamp=1686298507228, data=[91, [92], 93], isExpired=false}]
[Event{timestamp=1686298507259, data=[93, [94], 95], isExpired=false}]
[Event{timestamp=1686298507290, data=[95, [96], 97], isExpired=false}]
[Event{timestamp=1686298507320, data=[97, [98], 99], isExpired=false}]
```

修改为：

```java
//Siddhi Application
String siddhiApp =
        "" +
                "@App:name('Test') \n" +
                "define stream InputStream(age long, num int);\n" +
                "@info(name = 'test')" +
                "from every e1=InputStream, e2=InputStream[ifThenElse(e2[last].age is null, age < 100, e2[last].age < 100)]*, e3=InputStream[ age > 90 ]\n" +
                "select e1.age as age1, e2.age as age2, e3.age as age3\n" +
                "insert into OutputStream;\n";
```

输出为：

```
[Event{timestamp=1686298864521, data=[0, [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90], 91], isExpired=false}]

[Event{timestamp=1686298864551, data=[91, [], 92], isExpired=false}]
[Event{timestamp=1686298864566, data=[92, [], 93], isExpired=false}]
...
[Event{timestamp=1686298864938, data=[116, [], 117], isExpired=false}]
[Event{timestamp=1686298864953, data=[117, [], 118], isExpired=false}]
```

### Output Rate Limiting

可以限制指定的查询输出的事件数。

```
from <input stream> ...
select <attribute name>, <attribute name>, ...
output <rate limiting configuration>
insert into <output stream>
```

| rate limiting configuration | Syntax                                         | Description                           |
| --------------------------- | ---------------------------------------------- | ------------------------------------- |
| 基于时间                        | (output event selection)? every time interval  | 每隔一段时间输出一部分                           |
| 基于事件数量                      | (output event selection)? every event interval | 每隔一些数量输出一部分                           |
| 基于快照                        | snapshot every time interval                   | 每隔一段时间，输出当前查询窗口的的所有事件，如果没有窗口，只输出最后一个。 |

| output event selection | description                    |
| ---------------------- | ------------------------------ |
| first                  | 第一个查询结果生成后立刻发送，后续的事件被删除。直到下一次。 |
| last                   | 仅发出间隔内的最后一个事件。                 |
| all                    | 默认，所有的事件。                      |

举例子：

```java
//Siddhi Application
String siddhiApp =
        "" +
                "@App:name('Test') \n" +
                "define stream InputStream(age long, num int);\n" +
                "@info(name = 'test')" +
                "from every e1=InputStream -> e2=InputStream[ num == e2.num ]\n" +
                "select e1.age as age1, e2.age as age2\n" +
                "insert into OutputStream;\n";
```

这是一个基础的Siddhi sql，它的输出应该是：

```
[Event{timestamp=1686300966182, data=[0, 1], isExpired=false}]
[Event{timestamp=1686300966199, data=[1, 2], isExpired=false}]
...
[Event{timestamp=1686300968009, data=[116, 117], isExpired=false}]
[Event{timestamp=1686300968023, data=[117, 118], isExpired=false}]
```

修改如下：

```java
//Siddhi Application
String siddhiApp =
        "" +
                "@App:name('Test') \n" +
                "define stream InputStream(age long, num int);\n" +
                "@info(name = 'test')" +
                "from every e1=InputStream -> e2=InputStream[ num == e2.num ]\n" +
                "select e1.age as age1, e2.age as age2\n" +
                "output first every 20 events\n" +
                "insert into OutputStream;\n";
```

输出为：

```
[Event{timestamp=1686301127159, data=[0, 1], isExpired=false}]
[Event{timestamp=1686301127473, data=[20, 21], isExpired=false}]
...
[Event{timestamp=1686301128415, data=[80, 81], isExpired=false}]
[Event{timestamp=1686301128730, data=[100, 101], isExpired=false}]
```

修改如下：

```java
[Event{timestamp=1686301191514, data=[19, 20], isExpired=false}]
[Event{timestamp=1686301191827, data=[39, 40], isExpired=false}]
...
[Event{timestamp=1686301192454, data=[79, 80], isExpired=false}]
[Event{timestamp=1686301192767, data=[99, 100], isExpired=false}]
```

### Partition

根据事件的属性值将事件分类到各种隔离的分区实例并通过隔离处理分区实例来提供并行性。每隔分区都被分区键标记，只处理与分区键匹配的数据。将为每隔唯一的分区键动态创建一个分区实例，这些分区将永远存在内存中，除非@purge定义了删除策略。

```
@purge(enable='true', interval='<purge interval>', idle.period='<idle period of partition instance>')
partition with ( <key selection> of <stream name>,
                 <key selection> of <stream name>, ... )
begin
    from <stream name> ...
    select <attribute name>, <attribute name>, ...
    insert into (#)?<stream name>

    from (#)?<stream name> ...
    select <attribute name>, <attribute name>, ...
    insert into <stream name>

    ...
end;
```

| key selection      | Syntax                                                              | description                                                               |
| ------------------ | ------------------------------------------------------------------- | ------------------------------------------------------------------------- |
| Partition by value | attribute name                                                      | 根据属性值分区                                                                   |
| Partition by range | compare condition as 'value' or compare condition as 'value' or ... | 针对所有的condition执行event，并将所有与条件相关的属性值用于分区，当事件和多个条件匹配时，会在与这些条件相关的所有分区实例上做处理。 |

当一个分区块内有多个查询时，可以使用'#'来将查询连接起来而不离开分区实例的隔离，这种流被称为内部流。通过这种方式，无需对流进行再次分区，查询实例的输出也可以用作同一个分区实例中另一个查询实例的输入。

清理策略会定时清除给定时间段内未使用的分区，默认情况下，当一个分区实例被创建后，状态信息将永远存在并且可能会大量存在分区实例，造成内存紧张。分区实例可以根据@purge配置清除：

| configuration | description                                            |
| ------------- | ------------------------------------------------------ |
| enable        | 开启分区清除                                                 |
| internal      | 清除分区实例的周期性间隔，清除标记为清除的分区                                |
| idle.period   | 空闲期间，特定分区实例需要在可清除之前变为空闲状态；当该值指定时间段内没有数据到达分区时，才会被标记为清除。 |

举几个例子：

```java
//Siddhi Application
String siddhiApp =
        "" +
                "@App:name('Test') \n" +
                "define stream InputStream(age long, num int);\n" +
                "@info(name = 'test')\n" +
                "from InputStream#window.lengthBatch(20)\n" +
                "select min(age) as age1, max(age) as age2\n" +
                "insert into OutputStream;\n";

//Sending events to Siddhi
for (int i = 0; i < 119; i ++) {
        inputHandler.send(new Object[]{(long) i, i % 2});
        Thread.sleep(10L);
}
```

这个例子就是每20的元素输出一次最大值和最小值，在这种状态下，输出为：

```
[Event{timestamp=1686535765587, data=[0, 19], isExpired=false}]
[Event{timestamp=1686535765905, data=[20, 39], isExpired=false}]
[Event{timestamp=1686535766223, data=[40, 59], isExpired=false}]
[Event{timestamp=1686535766538, data=[60, 79], isExpired=false}]
[Event{timestamp=1686535766854, data=[80, 99], isExpired=false}]
# 因为没有119的输入，所以没有下一个输出
```

下面加上Partition by value的声明：

```java
//Siddhi Application
String siddhiApp =
        "" +
                "@App:name('Test') \n" +
                "define stream InputStream(age long, num int);\n" +
                "partition with ( num of InputStream )\n" +
                "begin\n" +
                "@info(name = 'test')\n" +
                "from InputStream#window.lengthBatch(20)\n" +
                "select min(age) as age1, max(age) as age2\n" +
                "insert into OutputStream;\n" +
                "end;\n";

//Sending events to Siddhi
for (int i = 0; i < 119; i ++) {
        inputHandler.send(new Object[]{(long) i, i % 2});
        Thread.sleep(10L);
}
```

如上，将输出流分成两个分区：

```
[Event{timestamp=1686535851493, data=[0, 38], isExpired=false}]
[Event{timestamp=1686535851508, data=[1, 39], isExpired=false}]
[Event{timestamp=1686535852124, data=[40, 78], isExpired=false}]
[Event{timestamp=1686535852140, data=[41, 79], isExpired=false}]
[Event{timestamp=1686535852752, data=[80, 118], isExpired=false}]
# 因为没有119的输入，所以没有下一个输出
```

说明如下：

按照num分区后，进入窗口的数据应该是：

```
[0, 2, 4, 6, 8, ..., 38, 40, ... 78, 80, ..., 118]
[1, 3, 5, 7, ..., 39, 41, ..., 117]
```

其中，0-38为20个事件，1-39为20个事件，类推得到结果。

下面增加一些分区实例，并替换为Partition by range：

```java
//Siddhi Application
String siddhiApp =
        "" +
                "@App:name('Test') \n" +
                "define stream InputStream(age long, num int);\n" +
                "partition with ( num <= 10 as 'lessTen' or\n" +
                "num <= 20 and num > 10 as 'less2Ten' or\n" +
                "num > 20 as 'more2Ten' of InputStream)\n" +
                "begin\n" +
                "@info(name = 'test')\n" +
                "from InputStream#window.lengthBatch(30)\n" +
                "select min(age) as age1, max(age) as age2\n" +
                "insert into OutputStream;\n" +
                "end;\n";

//Sending events to Siddhi
for (int i = 0; i < 119; i ++) {
        inputHandler.send(new Object[]{(long) i, i % 30});
        Thread.sleep(10L);
}
```

先来梳理逻辑，首先，输入的事件是：

```
[0, 0], [1, 1], ..., [10, 10] # 共11个
[11, 11], ..., [20, 20] # 共10个
[21, 21], ..., [29, 29] # 共9个
[30, 0], ..., [40, 10] # 共11个
[41, 11], ..., [50, 20] # 共10个
[51, 21], ..., [59, 29] # 共9个
[60, 0], ..., [70, 10] # 共11个
[71, 11], ..., [80, 20] # 共10个
[81, 21], ..., [89, 29] # 共9个
[90, 0], ..., [100, 10] # 共11个
[101, 11], ..., [110, 20] # 共10个
[111, 21], ..., [118, 28] # 共8个
```

根据分区策略：num <= 10、num <= 20 and num > 10、num > 20分区后，按照每30个划分：

```
[0, 0], [1, 1], ..., [10, 10], [30, 0], ..., [40, 10], [60, 0], ..., [67, 7]
[11, 11], ..., [20, 20], [41, 11], ..., [50, 20], [71, 11], ..., [80, 20]
[21, 21], ..., [29, 29], [51, 21], ..., [59, 29], [81, 21], ..., [89, 29], [111, 21], ..., [113, 23]


[68, 8], ..., [70, 10], [90, 0], ..., [100, 10] # 不足30
[101, 11], ..., [110, 20] # 不足30
[114, 24], ..., [118, 28] # 不足30
```

因此，推测输出为：

```
[Event{timestamp=1686536900681, data=[0, 67], isExpired=false}]
[Event{timestamp=1686536900886, data=[11, 80], isExpired=false}]
[Event{timestamp=1686536901411, data=[21, 113], isExpired=false}]
```

实际输出也等同。

下面用一个对比例子说明内部流(#)的使用：

```java
//Siddhi Application
String siddhiApp =
        "" +
                "@App:name('Test') \n" +
                "define stream InputStream(age long, num int);\n" +
                "partition with ( num <= 10 as 'lessTen' or\n" +
                "num <= 20 and num > 10 as 'less2Ten' or\n" +
                "num > 20 as 'more2Ten' of InputStream)\n" +
                "begin\n" +
                "@info(name = 'test')\n" +
                "from InputStream#window.lengthBatch(30)\n" +
                "select min(age) as age1, max(age) as age2\n" +
                "insert into OutputStream1;\n" +
                "from every e1=OutputStream1 -> e2=OutputStream1\n" +
                "select e1.age1 as age11, e1.age2 as age12, e2.age1 as age21, e2.age2 as age22\n" +
                "insert into OutputStream;\n" +
                "end;\n";

//Sending events to Siddhi
for (int i = 0; i < 119; i ++) {
        inputHandler.send(new Object[]{(long) i, i % 30});
        Thread.sleep(10L);
}
```

这个例子展示的是在一个分区块内，使用了CEP的场景，它的返回值是：

```
# 因为我们分了三个分区，所以会重复三次
[Event{timestamp=1686552013740, data=[0, 67, 11, 80], isExpired=false}]
[Event{timestamp=1686552013740, data=[0, 67, 11, 80], isExpired=false}]
[Event{timestamp=1686552013740, data=[0, 67, 11, 80], isExpired=false}]
[Event{timestamp=1686552014255, data=[11, 80, 21, 113], isExpired=false}]
[Event{timestamp=1686552014255, data=[11, 80, 21, 113], isExpired=false}]
[Event{timestamp=1686552014255, data=[11, 80, 21, 113], isExpired=false}]
```

当我们把OutputStream1声明为内部流时：

```java
//Siddhi Application
String siddhiApp =
        "" +
                "@App:name('Test') \n" +
                "define stream InputStream(age long, num int);\n" +
                "partition with ( num <= 10 as 'lessTen' or\n" +
                "num <= 20 and num > 10 as 'less2Ten' or\n" +
                "num > 20 as 'more2Ten' of InputStream)\n" +
                "begin\n" +
                "@info(name = 'test')\n" +
                "from InputStream#window.lengthBatch(30)\n" +
                "select min(age) as age1, max(age) as age2\n" +
                "insert into #OutputStream1;\n" +
                "from every e1=#OutputStream1 -> e2=#OutputStream1\n" +
                "select e1.age1 as age11, e1.age2 as age12, e2.age1 as age21, e2.age2 as age22\n" +
                "insert into OutputStream;\n" +
                "end;\n";
```

这样的话是没有返回值的，因为不存在e2，我们修改一下窗口大小：

```java
//Siddhi Application
String siddhiApp =
        "" +
                "@App:name('Test') \n" +
                "define stream InputStream(age long, num int);\n" +
                "partition with ( num <= 10 as 'lessTen' or\n" +
                "num <= 20 and num > 10 as 'less2Ten' or\n" +
                "num > 20 as 'more2Ten' of InputStream)\n" +
                "begin\n" +
                "@info(name = 'test')\n" +
                "from InputStream#window.lengthBatch(10)\n" +
                "select min(age) as age1, max(age) as age2\n" +
                "insert into #OutputStream1;\n" +
                "from every e1=#OutputStream1 -> e2=#OutputStream1\n" +
                "select e1.age1 as age11, e1.age2 as age12, e2.age1 as age21, e2.age2 as age22\n" +
                "insert into OutputStream;\n" +
                "end;\n";
```

输出为：

```
[Event{timestamp=1686552525646, data=[0, 9, 10, 38], isExpired=false}]
[Event{timestamp=1686552525849, data=[11, 20, 41, 50], isExpired=false}]
[Event{timestamp=1686552526113, data=[10, 38, 39, 67], isExpired=false}]
...
[Event{timestamp=1686552526573, data=[39, 67, 68, 96], isExpired=false}]
[Event{timestamp=1686552526793, data=[71, 80, 101, 110], isExpired=false}]
[Event{timestamp=1686552526841, data=[52, 82, 83, 113], isExpired=false}]
```

到此为止，内部流(#)的作用已经比较明显了：当分区块内输入到某个非内部流时，这个流会全分区广播，导致重复输出；当输入到内部流时，只在当前块内存在。

### table

表是事件的存储集合。表的定义包含表名和一组特定类型和表内唯一的属性名称。表的事件存储在内存中，也可以扩展到外部数据库。表支持主键、索引。

```
@primaryKey( <key>, <key>, ... )
@index( <key>)
define table <table name> (<attribute name> <attribute type>, <attribute name> <attribute type>, ... );
```

主键的用法和mysql基本一致，一张表只能由一个@primaryKey注解，支持联合主键。当插入相同主键的事件时，后一个事件会替换表中已存在的事件。

Siddhi不支持联合索引，但是一张表可以有很多个@index注解。

当需要使用外部存储的时候，可以使用@store注解：

```
@store(type='<store type>', <common.static.key>='<value>', <common.static.key>='<value>'
       @cache(size='<cache size>', cache.policy='<cache policy>', retention.period='<retention period>', purge.interval="<purge interval>"))
@primaryKey( <key>, <key>, ... )
@index( <key>, <key>, ...)
define table <table name> (<attribute name> <attribute type>, <attribute name> <attribute type>, ... );
```
目前支持的外部存储：[RDBMS](https://siddhi-io.github.io/siddhi-store-rdbms)，[MongoDB](https://siddhi-io.github.io/siddhi-store-mongodb)，[Redis](https://siddhi-io.github.io/siddhi-store-redis)，[Elasticsearch](https://siddhi-io.github.io/siddhi-store-elasticsearch)。

在使用外部存储的时候，根据外部存储的类型支持联合索引。同时，外部存储支持缓存@cache。

| parameter | description |
|-----------|-------------|
| size | 必填，缓存中最大存储量 |
| cache.policy | 选填，当超过最大存储量时，事件的删除策略。FIFO：默认，先进先出；LRU：最近最少使用；LFU：最不常用。 |
| retention.period | 选填，无论是否超过最大存储量，都会在一定时间后移除。 |
| purge.interval | 选填，标记为删除的事件的删除周期。 |

### Named-Aggregation

```
@store(type='<store type>', ...)
@purge(enable="<enable purging>", interval='<purging interval>',
       @retentionPeriod(<granularity> = '<retention period>', ...))
@PartitionById(enable="<enable distributed aggregation>")
define aggregation <aggregation name>
from (<stream>|<named-window>)
select <attribute name>, <aggregate function>(<attribute name>) as <attribute name>, ...
    group by <attribute name>
    aggregate by <timestamp attribute> every <time granularities> ;
```

| parameter | description |
|-----------|-------------|
| @store | 外部存储 |           
| @purge | 定时清理策略，不写默认策略 |
| @PartitionById | 使多个命名聚合以分布式的方式处理 |
| aggregation name | 推荐驼峰 |
| timestamp attribute | 配置属性以用作聚合中的事件时间戳 |
| time granularities | 定义执行聚合的粒度范围 |

当多个Siddhi App定义了多个命名聚合并且以同样的聚合名称指向同一个物理存储的时候，将会报错；除非使用@PartitionById。允许通过各个Siddhi App独立地聚合一部分数据，这部分数据将在数据检索期间被组合。