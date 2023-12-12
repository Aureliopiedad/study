# velocity

## velocity介绍

Velocity 是一个基于Java 的模板引擎。允许网页设计者引用Java代码中定义的方法。

看这个东西的原因是因为maven archetype生成项目时，使用到了这个模板。

Velocity使用的是VTL模板语言，一个最简单的例子是：

```
#set( $a = 'Velocity' )
#set( $b = "Velocity $a" )
```

在这个示例中，指令以'#'开头，分别是两个赋值语句，单引号将明确引用的值给到左边的引用对象，双引号将允许使用指令。

## 简单语法介绍

### 一个hello world例子

```html
<html>
  <body>
  #set( $foo = "Velocity" )
  Hello $foo World!
  </body>
</html>
```

### 注释

在Velocity中，注释有如下几种写法：

```
## This is a single line comment.

#*
  Thus begins a multi-line comment. Online visitors won't
  see this text because the Velocity Templating Engine will
  ignore it.
*#

#**
  This is a VTL comment block and
  may be used to store such information
  as the document author and versioning
  information:
    @author John Doe
    @version 5
*#
```

### 引用

变量的引用由符号'$'和后续标识符组成，标识符以字符开头，其余字符仅限[a-zA-Z0-9-_]。例如：

```
## variables
$foo
$mudSlinger
$mud-slinger
$mud_slinger
$mudSlinger1

## properties
$customer.Address

## methods
$customer.getAddress()
$page.setTitle('title')

## index notation

$array[0] ## 等同于 '$array.get(0)'
$array[$i]
$map['bar'] ## map是键值对

## 在Velocity 1.6版本后，Velocity支持可变长度的参数，即：public void setPlanets(String... planets)
$sun.setPlanets('Earth', 'Mars', 'Neptune')
$sun.setPlanets('Mercury')
$sun.setPlanets()

## 在Velocity 1.6版本后，所有的数组类型都被认为是长度固定的列表，可以在Velocity中对数组调用java.util.List方法，例如：
$myarray.isEmpty()
$myarray.size()
$myarray.get(2)
$myarray.set(1, 'test')
```

对于短引用，有如下按顺序的映射规则：

- 对于'$a.x'：
  1. $a.getx()
  2. $a.getX()
  3. $a.get('address')
  4. $a.isX()
- 对于'$a.X':
  1. $a.getX()
  2. $a.getx()
  3. $a.get('address')
  4. $a.isX()

上文展示的是引用类型的简写方法，还存在一种正式的引用符号：'${...}'：

```
Jack is a $vicemaniac.
Jack is a ${vice}maniac.
```

每个引用产生的最终值在呈现到最终输出的时候都会转化为String对象，Velocity 将调用其 .toString() 方法将该对象解析为 String。

当某个值不存在时：

- $a：展示为'$a'
- $!a：展示空字符串
- $!{a}：展示空字符串

当然还可以选择引用不存在的值时报错，需要配置runtime.references.strict为true。

### 指令

通常情况下，指令通常以'#'开头，也可以使用'#{}'的形式：

```
#if($a==1)true enough#{else}no way!#end
```

#### Set

最经常使用的指令应该是'#set'，set指令要求左侧必须是一个引用类型，右侧必须是以下类型之一：

- 变量引用
  ```
  #set( $a = $b )
  #set( $a = $b - 1 )
  #set( $a = $b * 1 )
  ```
- 字符串常量
  ```
  #set( $a = '132' )
  ```
- 属性引用
  ```
  #set( $a = $a.b )
  ```
- 方法引用
  ```
  #set( $a = $b.get($c) )
  ```
- 数值引用
  ```
  #set( $a = 321 )
  ```
- 列表
  ```
  #set( $a = ['12', $b, '123'] )
  ```
- 键值对
  ```
  ## $a.key 和 $a.get('key')是相等的
  #set( $a = {'key': 'value', 'key1': 'value1'} )
  ```

一般来说，使用单引号不会执行解析文本的操作，例如：'${value}1'和"${value}1"是不一样的，可以通过stringliterals.interpolate配置来修改。

经常会有大段大段的不解析的文本，这种情况需要使用类似以下的语法：

```
hello

#[[
#set( $result =  $test1.query('key1'))
hello $result

#set( $result =  $test1.query('key3'))
hello $result
]]#
```

这样输出的就是单纯的字符串，不会执行任何操作。

#### If ElseIf Else

```
#set($list = [1,2,3,4])
#set($result = '1')

#foreach($i in $list)
    #if($i == 1)
        1
    #elseif($i == 2)
        2
    #elseif($i == 3)
        3
    #else
        4
    #end
#end

#if($result)
    t
#else
    f
#end

#if($a)
    a
#else
    b
#end
```

输出为：

```shell
1
2
3
4

t

b
```

1. 在Velocity中，if能判断的不止是布尔类型，在传递非布尔值的情况下，if的作用是判空，满足非空即返回true。
2. 在Velocity中，使用'=='判断对象是否相等时，判断逻辑是调用对象的toString()进行比较
3. 在Velocity中，也可以使用AND(&&)、OR(||)、NOT(!)
4. 在Velocity中，逻辑运算符也有字符版本，eq, ne, and, or, not, gt, ge, lt, le。

#### Foreach

```
#set( $map = {'key1': 'value1', 'key2': 'value2'} )
#foreach($key in $map.keySet())
Index:$foreach.index, Key: $key, Value: $map.get($key) #if($foreach.hasNext); #{else}。#set($count = $foreach.count) #{end}
#end
List count: $count
```

输出为：

```shell
Index:0, Key: key1, Value: value1 ; 
Index:1, Key: key2, Value: value2 。 
List count: 2
```

除此之外，#foreach还存在类似$foreach.first、$foreach.last、$foreach.parent、$foreach.topmost等方法。和java一样，可以使用break跳出循环。可以通过directive.foreach.maxloops配置最大循环次数。

#### Include

[将本地文件插入模板](https://velocity.apache.org/engine/1.7/user-guide.html#include)

#### Parse

[将本地VM文件插入模板](https://velocity.apache.org/engine/1.7/user-guide.html#parse)

#### Break

跳出当前渲染。

#### Stop

停止所有渲染。

#### Evaluate

类似eval，相当于重新渲染字符串。例如：

```
#set($source1 = "abc")
#set($select = "1")
#set($dynamicsource = "$source$select")
## $dynamicsource is now the string '$source1'
#evaluate($dynamicsource)
## 真实输出为abc
```

#### Define

允许把一段代码分配给某个引用。