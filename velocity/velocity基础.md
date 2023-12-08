# velocity

## velocity介绍

Velocity 是一个基于Java 的模板引擎。允许网页设计者引用Java代码中定义的方法。

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

