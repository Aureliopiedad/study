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

在某些情况下，'$a.x'和'$a.getX()'是相等的。对于短引用，有如下按顺序的映射规则：

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

最经常使用的指令应该是'#set'，set指令要求左侧必须是一个引用类型，右侧必须是以下类型之一：

- 变量引用
  ```
  #set( $a = $b )
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
  #set( $a = ['key': 'value', 'key1': 'value1'] )
  ```