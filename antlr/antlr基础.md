# Antlr4 学习

先说一下为什么看这个东西：最近需要学习siddhi，而siddhiQL是基于[antlr4](https://github.com/antlr/antlr4/blob/4.6/doc/getting-started.md)解析的。

## Antlr4 介绍

> ANTLR (ANother Tool for Language Recognition) is a powerful parser generator for reading, processing, executing, or translating structured text or binary files. It's widely used to build languages, tools, and frameworks. From a grammar, ANTLR generates a parser that can build and walk parse trees.

ANTLR(ANother Tool for Language Recognition)是一个语法解析器生成器，用于读取、处理、执行或翻译结构化文本或二进制文件，被用于构建语言、工具和框架。根据语法，ANTLR生成了一个可以构建和遍历解析树的解析器。

ANTLR的应用案例非常多，最常见的场景是实现一套类sql的查询语言或者读取配置文件，其他的类似Hive、Presto、SparkSQL等应用也是基于ANTLR研发的。由于很多类似编译原理的机制，所以ANTLR也可以作为某种自定义语言的编译器的实现基础。

> ANTLR v4 is a powerful parser generator that you can use to read, process, execute, or translate structured text or binary files. It’s widely used in  academia and industry to build all sorts of languages, tools, and frameworks. Twitter search uses ANTLR for query parsing, with more than 2 billion queries  a day. The languages for Hive and Pig and the data warehouse and analysis systems for Hadoop all use ANTLR. Lex Machina 1 uses ANTLR for information  extraction from legal texts. Oracle uses ANTLR within the SQL Developer IDE and its migration tools. The NetBeans IDE parses C++ with ANTLR. The HQL language in the Hibernate object-relational mapping framework is built with ANTLR.

## 前置需求

pom依赖引入：

```xml
<dependency>
    <groupId>org.antlr</groupId>
    <artifactId>antlr4-runtime</artifactId>
    <version>4.13.0</version>
</dependency>
```

由于需要对文件做编译，需要再安装一个antlr4的[执行工具](https://github.com/antlr/antlr4-tools)，这里不推荐安装，推荐idea的[antlr4的插件](https://plugins.jetbrains.com/plugin/7358-antlr-v4)。

还有一个可选的[Maven插件](https://github.com/antlr/antlr4/tree/master/antlr4-maven-plugin)，默认状态下，会读取\${basedir}/src/main/antlr4下的语法文件，并在\${basedir}/src/man/antlr4/imports搜索Token文件，编译结果的默认输出路径为\${project.build.directory}/generated-sources/antlr4。

目前，这个Maven插件最大的问题是，高版本的插件需要高版本的JDK版本。

当然，默认的读取路径和输出路径是可以更换的：

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.antlr</groupId>
            <artifactId>antlr4-maven-plugin</artifactId>
            <version>4.13.0</version>
            <executions>
                <execution>
                    <goals>
                        <goal>antlr4</goal>
                    </goals>
                    <configuration>
                        <listener>true</listener>
                        <visitor>true</visitor>
                        <sourceDirectory>${project.basedir}/src/main/resources/antlr4</sourceDirectory>
                        <libDirectory>${project.basedir}/src/main/resources/antlr/imports</libDirectory>
                        <outputDirectory>${project.basedir}/src/main/java/antlr/generated</outputDirectory>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

## DemoV1

先定义一个很简单的语法规则文件：Hello.g4

```
/**  
* This is a hello test  
*/  
grammar Hello;  
@header { package antlr.generated; }  
  
/*  
This is a hello test  
*/  
prog: expr EOF;  
  
expr: HELLO NAME  
    ;  
  
/** This is a hello test */  
HELLO  
    : H E L L O // This is a hello test  
    ;  
  
NAME  
    : [a-zA-Z]+  
    ;  
  
fragment H: [hH];  
fragment E: [eE];  
fragment L: [lL];  
fragment O: [oO];  
  
SPACE  
    : ' ' -> skip;
```

如上文，这个简单的语法规则表达的是：将传入的字符串解析成hello和其他字符串，忽略空格。

```java
@Log4j  
public class HelloApp {  
    public static void main(String[] args) {  
        HelloLexer helloLexer = new HelloLexer(CharStreams.fromString("hello tEst"));  
        CommonTokenStream commonTokenStream = new CommonTokenStream(helloLexer);  
        HelloParser helloParser = new HelloParser(commonTokenStream);  
        HelloParser.ProgContext progContext = helloParser.prog();  
  
        log.info(progContext.toStringTree());  
    }  
}
```

例如，当输入为：'hello tEst'时，输出为：“([] ([4] hello tEst) <EOF>)”。

## 概念介绍

### 基本概念

为了能够识别一段文字，需要做的步骤:

1. 解析一段文字，把文字分解为不同的组件(Tokens)，对应词法分析(lexical analysis)。
2. 在Antlr4中，通过parser把第一步解析的组件(Tokens)构建成一个语法树结构，对应语法解析。

这样的说法可能有些抽象，举一个例子来描述以下这段流程：

#### 声明规则文件

```
grammar TokensTest;  
@header {  package antlr.generated;}  
  
prog: expr EOF;  
  
/**  
* 一个简单的赋值语句  
*/  
expr: VALUE '=' VALUE;  
  
VALUE  
    : [a-zA-Z]+  
    ;  
  
SPACE  
    : ' ' -> skip  
    ;
```

#### 准备一段需要解析的文字

本例中，将传入以下字符串：

```
name = abc
```

#### 按照上文的流程解析字符串

```java
@Log4j  
public class TokensTestApp {  
    public static void main(String[] args) {  
        // 对应Lexer，从字符串解析成tokens  
		TokensTestLexer tokensTestLexer = new TokensTestLexer(CharStreams.fromString("name = abc"));  
		CommonTokenStream commonTokenStream = new CommonTokenStream(tokensTestLexer);  
		  
		// 将tokens转化为树  
		TokensTestParser tokensTestParser = new TokensTestParser(commonTokenStream);  
		TokensTestParser.ExprContext expr = tokensTestParser.expr();  
		log.info(expr.children);
    }  
}
```

输出为：

```shell
[name, =, abc]
```

### 访问树

在生成解析树之后，使用者就可以解析树(ParseTree)来访问各个节点。当然，Antlr4本身也提供了两种默认的访问方式，分别是监听者和访问者。

[访问者](https://www.runoob.com/design-pattern/visitor-pattern.html)和监听者都属于设计模式。

这里讲一下访问者和监听者模式在树中的应用，访问者模式会在顶层抽象接口中定义全部parser rule的访问方式，例如TokensTest.g4的顶层访问接口如下：

```java
public interface TokensTestVisitor<T> extends ParseTreeVisitor<T> {
	T visitProg(TokensTestParser.ProgContext ctx);
	T visitExpr(TokensTestParser.ExprContext ctx);
}
```

监听者的顶层抽象如下:

```java
public interface TokensTestListener extends ParseTreeListener {
	void enterProg(TokensTestParser.ProgContext ctx);
	void exitProg(TokensTestParser.ProgContext ctx);
	void enterExpr(TokensTestParser.ExprContext ctx);
	void exitExpr(TokensTestParser.ExprContext ctx);
}
```

关于访问者和监听者如何在antlr中实现，根据使用者选择方式的不同，antlr会调用不同的方法：

```java
public static class ProgContext extends ParserRuleContext {  
	// 略
	
   @Override  
   public void enterRule(ParseTreeListener listener) {  
      if ( listener instanceof TokensTestListener ) ((TokensTestListener)listener).enterProg(this);  
   }  
   @Override  
   public void exitRule(ParseTreeListener listener) {  
      if ( listener instanceof TokensTestListener ) ((TokensTestListener)listener).exitProg(this);  
   }  
   @Override  
   public <T> T accept(ParseTreeVisitor visitor) {  
      if ( visitor instanceof TokensTestVisitor ) return ((TokensTestVisitor)visitor).visitProg(this);  
      else return visitor.visitChildren(this);  
   }  
}
```

### 语法词典

#### 注释

在g4文件中，注释的写法和java一致，详见Hello.g4。

#### 标识符

Token命名时一般是以大写字母开头，一般全大写，举例：

```shell
ID, LPAREN, RIGHT_CURLY // token names/lexer rules
```

Parser Rule命名时一般以小写字母开头，一般全小写，举例：

```
expr, simpleDeclarator, d2, header_file // parser rule names
```

#### 保留关键字

如同许多语言，Antlr4存在许多关键字，这些关键字不允许重写：

- import
- fragment
- lexer
- parser
- grammar
- returns
- locals
- throws
- catch
- finally
- mode
- options
- tokens

### 语法结构

g4文件的语法本质上是一个声明，包含一系列的规则，大体上，一般的g4文件结构如下：

```
/** Optional javadoc style comment */  
grammar Name;   
options {...}  
import ... ;  
   
tokens {...}  
channels {...} // lexer only  
@actionName {...}  
     
rule1 // parser and lexer rules, possibly intermingled  
...  
ruleN
```

1. 在g4文件中，注释的写法和java一致。
2. grammar 后面的字符串必须是文件名，例如上文的grammar Hello，文件名就是Hello.g4。
3. 在一个g4文件中，除了grammar和最少一个rule之外，其他内容均为可选。
4. 一般情况下，rule名称以小写字母开头，lexer规则以大写字母开头。
5. 本次暂不说明比较复杂的语法结构，以基础使用为主。

## DemoV2

下面进行一个比较复杂的语句逻辑定义CalTest.g4：

```
grammar CalTest;  
@header { package antlr.generated; }  
  
cal  
    : (expr)* EOF  
    ;  
  
expr  
    : '(' + expr + ')' # exprWithBr  
    | expr OP=('*'|'/'|'%') expr # exprWithMultiplication  
    | expr OP=('+'|'-') expr # exprWithAddition  
    | INT # exprWithConstantValue  
    ;  
  
INT  
    : ('-'|'+')? [0-9]+  
    ;  
  
SPACE  
    : ' ' -> skip;  
  
NEW_LINE  
    : [\n\t\r] -> skip  
    ;
```

如上，这个规则文件展示了基本的四则运算，包括加减乘除、括号和数字。例如，输入：'(1 + 2) * 2 + 1'，那么解析出来的树结构为：

```
calc:
    expr:
        expr:
            expr:
                BR_OPEN:(
                expr:
                    expr:
                        NUMBER:1
                    PLUS:+
                    expr:
                        NUMBER:2
                BR_CLOSE:)
            TIMES:*
            expr:
                NUMBER:2
        PLUS:+
        expr:
            NUMBER:1
    <EOF>
```

顺便一提，因为四则运算是有基本顺序的，所以规则中需要体现这个顺序。

到目前为止，只是把传入的运算解析成一个一个的token，该怎么计算出真正的值呢？

新建一个类：

```java
@Log4j  
public class CalTestVisitorImpl extends CalTestBaseVisitor<Integer> {  
    @Override  
    public Integer visitCal(CalTestParser.CalContext ctx) {  
        return super.visitCal(ctx);  
    }  
  
    @Override  
    public Integer visitExprWithBr(CalTestParser.ExprWithBrContext ctx) {  
        return super.visitExprWithBr(ctx);  
    }  
  
    @Override  
    public Integer visitExprWithAddition(CalTestParser.ExprWithAdditionContext ctx) {  
        Integer leftInteger = visit(ctx.expr(0));  
        Integer rightInteger = visit(ctx.expr(1));  
  
        return switch (ctx.OP.getText()) {  
            case "+" -> leftInteger + rightInteger;  
            case "-" -> leftInteger - rightInteger;  
            default -> throw new RuntimeException();  
        };  
    }  
  
    @Override  
    public Integer visitExprWithMultiplication(CalTestParser.ExprWithMultiplicationContext ctx) {  
        Integer leftInteger = visit(ctx.expr(0));  
        Integer rightInteger = visit(ctx.expr(1));  
  
        return switch (ctx.OP.getText()) {  
            case "*" -> leftInteger * rightInteger;  
            case "/" -> leftInteger / rightInteger;  
            case "%" -> leftInteger % rightInteger;  
            default -> throw new RuntimeException();  
        };  
    }  
  
    @Override  
    public Integer visitExprWithConstantValue(CalTestParser.ExprWithConstantValueContext ctx) {  
        return Integer.parseInt(ctx.getText());  
    }  
  
    @Override  
    protected Integer aggregateResult(Integer aggregate, Integer nextResult) {  
        return aggregate == null ? nextResult : aggregate;  
    }  
}
```

这样，当输入为：'(1 + 2) * (2 + 1)'时，输出为：'9'。

## DemoV2 进阶

```
grammar ActionsTest;  
@header {  
package antlr.generated;  
}  
  
@parser::members {  
    private int eval(int left, int op, int right) {  
        switch ( op ) {  
            case MUL : return left * right;  
            case DIV : return left / right;  
            case ADD : return left + right;  
            case SUB : return left - right;  
            case MOD : return left % right;  
        }  
        return 0;  
    }  
}  
  
cal  
    : expr EOF  
    ;  
  
expr returns [int v]  
    : '(' value=expr ')' {$v = $value.v;}  
    | value1=expr OP=(MUL|DIV|MOD) value2=expr {$v = eval($value1.v, $OP.type, $value2.v);}  
    | value1=expr OP=(ADD|SUB) value2=expr {$v = eval($value1.v, $OP.type, $value2.v);}  
    | INT {$v = $INT.int;}  
    ;  
  
MUL: '*';  
DIV: '/';  
MOD: '%';  
ADD: '+';  
SUB: '-';  
  
INT  
    : ('-'|'+')? [0-9]+  
    ;  
  
SPACE  
    : ' ' -> skip;  
  
NEW_LINE  
    : [\n\t\r] -> skip  
    ;
```

这样子在g4规则文件中集成函数后，不需要单独实现访问者或者监听者，使用默认的parser就可以实现基础的逻辑转换。在这个过程中，lexer过程不变，针对的是parser的过程。

相比于CalTest.g4的Parser，ActionsTestParser中新增了调用eval的代码。

## 源码分析

我们创建一个新的g4文件，大概讲一下实现：

```
grammar LexerTest;  
@header { package antlr.generated; }  
  
prog: expr EOF;  
  
expr  
    : attribute_name '=' constant_value  
    ;  
  
attribute_name  
    : NAME  
    ;  
  
constant_value  
    : NUMBER # constant_number  
    | BOOLEAN # constant_boolean  
    | STRING # constant_string  
    ;  
  
NAME: '$.' [a-zA-Z_] ([a-zA-Z_0-9])*;  
NUMBER: [1-9] ([0-9])*;  
BOOLEAN: TRUE | FALSE;  
STRING: ('"'(.*?)'"')  {setText(getText().substring(1, getText().length()-1));};  
  
TRUE: T R U E;  
FALSE: F A L S E;  
  
fragment A : [aA];  
fragment B : [bB];  
fragment C : [cC];  
fragment D : [dD];  
fragment E : [eE];  
fragment F : [fF];  
fragment G : [gG];  
fragment H : [hH];  
fragment I : [iI];  
fragment J : [jJ];  
fragment K : [kK];  
fragment L : [lL];  
fragment M : [mM];  
fragment N : [nN];  
fragment O : [oO];  
fragment P : [pP];  
fragment Q : [qQ];  
fragment R : [rR];  
fragment S : [sS];  
fragment T : [tT];  
fragment U : [uU];  
fragment V : [vV];  
fragment W : [wW];  
fragment X : [xX];  
fragment Y : [yY];  
fragment Z : [zZ];  
  
SPACE: ' ' -> skip;
```

### 构造Lexer，创建Token

首先，需要将传入的字符串通过Lexer解析成Tokens，这一步的入口在：

```java
LexerTestLexer lexerTestLexer = new LexerTestLexer(CharStreams.fromString("$._name_09 = true"));
```

通过构造方法，将字符串流传入词法解析。

整个的词法解析流程使用了ATN(Augmented transition network)、DFA(Deterministic finite automaton)和Adaptive LL(*) parsing，感兴趣的自行了解，这里只大概介绍以下如何实现的。

整个DFA的源码就不放了，这里展示一下一个比较好的理解的，但是百分百有问题的，在输入完全正确的情况下能跑的简易DFA：

```java
@Log4j  
public class LexerTestLexerImpl extends LexerTestLexer {  
    char c;  
    int line = 0;  
    int charPositionInLine = 0;  
    int charStartPosition = 0;  
    int charEndPosition = 0;  
    int dfaState = 0;  
  
    public LexerTestLexerImpl(CharStream input) {  
        super(input);  
        c = (char) _input.LA(1);  
    }  
  
    @Override  
    public Token nextToken() {  
        while (true) {  
            if (c == (char) CharStream.EOF) {  
                charStartPosition = _input.index();  
                return createToken(CharStream.EOF);  
            }  
  
            while (Character.isWhitespace(c)) {  
                consume();  
            }  
  
            charStartPosition = _input.index();  
  
            switch (dfaState) {  
                case 0:  
                    if (c == '$') {  
                        consume();  
                    }  
  
                    if (c == '.') {  
                        consume();  
                    }  
  
                    if (c >= 'a' && c <= 'z' || c == '_')  {  
                        while ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || ((c >= '0' && c <= '9')) || (c == '_')) {  
                            consume();  
                        }  
  
                        dfaState = 1;  
                        charEndPosition = _input.index() - 1;  
                        return createToken(NAME);  
                    }  
  
                    consume();  
                    break;  
                case 1:  
                    if (c == '=') {  
                        consume();  
  
                        dfaState = 2;  
                        charEndPosition = _input.index() - 1;  
                        return createToken(T__0);  
                    }  
  
                    consume();  
                    break;  
                case 2:  
                    if (c >= '1' && c <= '9') {  
                        while (c >= '0' && c <= '9') {  
                            consume();  
                        }  
  
                        dfaState = 3;  
                        charEndPosition = _input.index() - 1;  
                        return createToken(NUMBER);  
                    }  
  
                    if (c == '\"') {  
                        consume();  
                        while (c != '\"') {  
                            consume();  
                        }  
                        consume();  
  
                        dfaState = 3;  
                        charStartPosition++;  
                        charEndPosition = _input.index() - 2;  
                        return createToken(STRING);  
                    }  
  
                    if (c == 't' || c == 'T') {  
                        char c1;  
                        if (((c1 = (char) _input.LA(2)) == 'r' || c1 == 'R')  
                                && ((c1 = (char) _input.LA(3)) == 'u' || c1 == 'U')  
                                && ((c1 = (char) _input.LA(4)) == 'e' || c1 == 'E')) {  
                            consume();  
                            consume();  
                            consume();  
                            consume();  
  
                            dfaState = 3;  
                            charEndPosition = _input.index() - 1;  
                            return createToken(BOOLEAN);  
                        }  
                    }  
  
                    if (c == 'f' || c == 'F') {  
                        char c1;  
                        if (((c1 = (char) _input.LA(2)) == 'a' || c1 == 'A')  
                                && ((c1 = (char) _input.LA(3)) == 'l' || c1 == 'L')  
                                && ((c1 = (char) _input.LA(4)) == 's' || c1 == 'S')  
                                && ((c1 = (char) _input.LA(4)) == 'e' || c1 == 'E')) {  
                            consume();  
                            consume();  
                            consume();  
                            consume();  
                            consume();  
  
                            dfaState = 3;  
                            charEndPosition = _input.index() - 1;  
                            return createToken(BOOLEAN);  
                        }  
                    }  
  
                    consume();  
                    break;  
                default:  
                    consume();  
                    break;  
            }  
  
        }  
    }  
  
    private Token createToken(int ttype) {  
        String text = null;  
        return _factory.create(_tokenFactorySourcePair, ttype, text,  
                Token.DEFAULT_CHANNEL, charStartPosition, charEndPosition,  
                line, charPositionInLine);  
    }  
  
    private void consume() {  
        if (c == '\n') {  
            line++;  
            charPositionInLine = 0;  
        }  
        if (c != (char) CharStream.EOF) {  
            _input.consume();  
        }  
        c = (char) _input.LA(1);  
        charPositionInLine++;  
    }  
}
```


经过解析后，字符串"\$._name_09 = true"实际上被解析成：[0, 9], [10, 10], [11, 11], [12, 12], [13, 16], [17, 16]，分别对应：[$._name_09], [ ], [=], [ ], [true]。因为在g4文件中配置了空格的skip设置，所以[10, 10]和[12, 12]会在前进过程中被丢弃。

### 构造Parser，创建树结构

在执行：

```shell
LexerTestParser.ExprContext expr = lexerTestParser.expr();
```

后，会进入parser的树转化环节。

prog的解析流程大致为(伪代码)：

```shell
final ProgContext prog() {
	expr();
	match(EOF);
}

// 在有备选条件的情况下，会存在switch case的逻辑，这里不展示
final ExprContext expr() {
	attribute_name();
	match(=);
	constant_value();
}

final ExprContext attribute_name() {
	match(NAME);
}

final ExprContext constant_value() {
	switch(this token) {
		case NUMBER:
			match(NUMBER);
		case BOOLEAN:
			match(BOOLEAN);
		case STRING:
			match(STRING);
	}
}
```

每进行一个match，都会把Token的index前进一次(org.antlr.v4.runtime.Lexer#nextToken)，例如，第一次进行match(VALUE)时，对应的token就是[name]。

在match过程中，每个match都会生成对应的子节点，最终在match(EOF)后生成解析树。

## 附录：规则文件复杂结构

### 基础结构

一个最基本的Parser rule由三部分组成，包括规则名称、规则内容和分号的结尾符，例如：

```
test_rule  
    : 'end'   
    ;
```

规则内容可以被分割：

```
test  
    : 'a'  
    | 'b'  
    ;
```

规则内容如果包括空，证明该规则是可选的：

```
test  
    : 'a'  
    | // 这里是空
    ;
```

### 替代标签

在parser规则定义中，可以使用#标记各个定义项，方便访问者和监听者识别，例如：

```
expr  
    : VALUE '=' VALUE # test  
    | # test1  
    ;
```

注意：不可重复。

### 规则元素

- Tokens：首字母大写
- Parser rule：首字母小写
- 待补充

### 子规则

一个parser rule可以包含子规则，例如：

```
ex  
    : VALUE ('!=' | '=') VALUE  
    ;
```

在这段声明中，('!=' | '=')就是一个子规则。子规则没有单独的名称，是用括号围起来的，有一个或多个替代项。大体上，存在四种子规则：

1. 匹配子规则中的任意某个备选选项一次: (x|y|z)
2. 不匹配或匹配子规则中的任意某个备选选项一次: (x|y|z)?
3. 不匹配或匹配子规则中的任意备选选项多次: (x|y|z)*
4. 匹配子规则中的任意备选选项多次: (x|y|z)+

具体的表述可以访问：[Subrules](https://github.com/antlr/antlr4/blob/master/doc/parser-rules.md#subrules)。

### 异常捕获

待补充
[catching-exceptions](https://github.com/antlr/antlr4/blob/master/doc/parser-rules.md#catching-exceptions)

### 规则引用

待补充
[ule-attribute-definitions](https://github.com/antlr/antlr4/blob/master/doc/parser-rules.md#rule-attribute-definitions)

### Actions and Attributes

一般来说，Actions指的是使用大括号括起来的文本块，在识别到对应Tokens的时候触发，例如：

```
decl: type ID ';' {System.out.println("found a decl");} ;
```

## 附录：左递归规则

常见于加减乘除的顺序问题。

## siddhiQL.g4的说明

单纯看siddhiQL文件，显得有些太多了，我们以一个简单的siddhi的例子来实际的看一下：

```shell
@App:name('Test')
define stream inputStreamA (id int, content string, enterpriseId string, timestamp long);
define stream inputStreamB (id int, content string, enterpriseId string, timestamp long);
define stream outputStream (id int, content string, enterpriseId string, timestamp long);
from every eventA=inputStreamA[ id == 1] -> eventB=inputStreamB[ eventA.enterpriseId == eventB.enterpriseId and eventB.id == 2 ] within 50 sec
select eventA.id, eventA.content, eventA.enterpriseId, eventA.timestamp insert into outputStream;
```

根据siddhiQL.g4对siddhiQL整体的定义来看，最开始需要解析的是：

```shell
parse
    : siddhi_app EOF
    ;

siddhi_app
    : (app_annotation|error)*
      (definition_stream|definition_table|definition_trigger|definition_function|definition_window|definition_aggregation|error) 
      (';' (definition_stream|definition_table|definition_trigger|definition_function|definition_window|definition_aggregation|error))*
      (';' (execution_element|error))* ';'?
    ;
```

所以，在进入siddhiQL解析流程的第一步，是解析成ParseContext。针对ParseContext的处理，可以看io.siddhi.query.compiler.internal.SiddhiQLBaseVisitorImpl#visitParse方法，提取出siddhi_app部分，解析Siddhi_appContent，再执行io.siddhi.query.compiler.internal.SiddhiQLBaseVisitorImpl#visitSiddhi_app。

先看第一行，根据siddhiQL.g4的定义：

```shell
app_annotation
    : '@' APP ':' name ('(' annotation_element (',' annotation_element )* ')' )?
    ;

APP:      A P P;

fragment A : [aA];
fragment P : [pP];

# 在@APP中，name会被解析为id，所以keyword在这里不展示
name
    :id|keyword
    ;

# 在@APP中，id会被解析为ID，所以ID_QUOTES在这里不展示
id: ID_QUOTES|ID ;

ID : [a-zA-Z_] [a-zA-Z_0-9]* ;

# 在@APP中，不存在property_name
annotation_element
    :(property_name '=')? property_value
    ;

property_value
    :string_value
    ;

string_value: STRING_LITERAL;

# 这段代码的意思是，匹配引号中的字符串，最后会把段首段尾的引号清除。支持单引号、双引号、三引号
STRING_LITERAL
    :(
        '\'' ( ~('\u0000'..'\u001f' | '\''| '"' ) )* '\''
        |'"' ( ~('\u0000'..'\u001f'  |'"') )* '"'
     )  {setText(getText().substring(1, getText().length()-1));}
     |('"""'(.*?)'"""')  {setText(getText().substring(3, getText().length()-3));}
    ;
```

这一步的解析可见io.siddhi.query.compiler.internal.SiddhiQLBaseVisitorImpl#visitApp_annotation，在@APP中，最终会走到io.siddhi.query.compiler.internal.SiddhiQLBaseVisitorImpl#visitId，读取id的文字作为annotation填充到siddhiApp中。

第一步解析完后，会填充siddhiApp的annotations。

第二步开始解析stream，先看看stream的定义：

```shell
# 在这个例子中不包括annotation
definition_stream
    : annotation* DEFINE STREAM source '(' attribute_name attribute_type (',' attribute_name attribute_type )* ')'
    ;

DEFINE:   D E F I N E;
STREAM:   S T R E A M;

# 在这个例子中，不包括内部流(#)和故障流(!)
source
    :(inner='#' | fault='!')? stream_id
    ;

stream_id
    :name
    ;

attribute_name
    :name
    ;

attribute_type
    :STRING     
    |INT        
    |LONG       
    |FLOAT      
    |DOUBLE     
    |BOOL      
    |OBJECT     
    ;
```

解析stream没什么可说的，这个例子最终生成了三个stream，填充进siddhiApp。

最后一步指的是execution_element，包括from within select，这个比较复杂，得分开看：

在本例中，适配execution_element的语句是：

```shell
from every eventA=inputStreamA[ id == 1] -> eventB=inputStreamB[ eventA.enterpriseId == eventB.enterpriseId and eventB.id == 2 ] within 50 sec
select eventA.id, eventA.content, eventA.enterpriseId, eventA.timestamp insert into outputStream;
```

直接说结论，在这个例子中，上述的siddhiQL被分解为：

```shell
# FROM
from
# query_input
# pattern_stream
    # every_pattern_source_chain
        # every_pattern_source_chain
            # every just string
            every 
            # pattern_source
            # standard_stateful_source
                # event
                # id
                # just string
                eventA
                # =, just string
                =
                # basic_source
                    # source
                    # stream_id
                    # name
                    # id, just string
                    inputStreamA
                    # basic_source_stream_handlers
                    # basic_source_stream_handler
                    # filter
                        # [, just string
                        [ 
                        # expression
                        # equality_math_operation
                            # basic_math_operation
                            # attribute_reference
                            # attribute_name
                            # name
                            # id, just string
                            id 
                            # just string
                            == 
                            # basic_math_operation
                            # constant_value
                            # signed_int_value
                            # just value
                            1
                        # ], just string
                        ] 
        # -> just string
        -> 
        # every_pattern_source_chain
        # pattern_source_chain
        # pattern_source
        # standard_stateful_source
            # event
            # id, just string
            eventB
            # just string
            =
            # basic_source
                # source
                # stream_id
                # name
                # id
                inputStreamB
                # basic_source_stream_handlers
                # basic_source_stream_handler
                # filter
                    # just string
                    [ 
                    # expression
                    # and_math_operation
                        # equality_math_operation
                            # basic_math_operation
                            # attribute_reference
                                # name
                                eventA
                                .
                                # attribute_name
                                # name
                                enterpriseId 
                            # just string
                            == 
                            # basic_math_operation
                            # attribute_reference
                                # name
                                eventB
                                .
                                # attribute_name
                                # name
                                enterpriseId 
                        # just string
                        and 
                        # equality_math_operation
                            # basic_math_operation
                            # attribute_reference
                                # name
                                eventB
                                .
                                # attribute_name
                                id 
                            == 
                            # basic_math_operation
                                # constant_value
                                # singed_int_value
                                2 
                    # just string
                    ] 
    # within_time
        # within just string
        within 
        # time_value
        50 sec
# query_section
    # select just string
    select 
    # output_attribute
    # attribute_reference
        # name
        eventA
        .
        # attribute_name
        # name
        id
    # , 
    , 
    # output_attribute
    # attribute_reference
        eventA
        .
        content
    # ,
    , 
    # output_attribute
    # attribute_reference
        eventA
        .
        enterpriseId
    # ,
    , 
    # output_attribute
    # attribute_reference
        eventA
        .
        timestamp
# query_output
    # just string
    insert 
    # just string
    into 
    # target
    # source
    # stream_id
    # name
    outputStream
```

```shell
# 这个例子中不存在partition，本次不说明
execution_element
    :query|partition
    ;

# 这个例子中不存在annotation，本次不说明
query
    : annotation* FROM query_input query_section? output_rate? query_output
    ;

FROM:     F R O M;

# 这个例子中只存在pattern_stream，其他不说明
query_input
    : (standard_stream|join_stream|pattern_stream|sequence_stream|anonymous_stream)
    ;

# 这个例子中不存在absent_pattern_source_chain，不说明
pattern_stream
    : every_pattern_source_chain within_time?
    | absent_pattern_source_chain within_time?
    ;

within_time
    :WITHIN time_value
    ;

every_pattern_source_chain
    : '('every_pattern_source_chain')' 
    | EVERY '('pattern_source_chain ')'   
    | every_pattern_source_chain  '->' every_pattern_source_chain
    | pattern_source_chain
    | EVERY pattern_source 
    ;

pattern_source_chain
    : '('pattern_source_chain')'
    | pattern_source_chain  '->' pattern_source_chain
    | pattern_source
    ;

pattern_source
    :logical_stateful_source|pattern_collection_stateful_source|standard_stateful_source|logical_absent_stateful_source
    ;

standard_stateful_source
    : (event '=')? basic_source
    ;

event
    :name
    ;

basic_source
    : source basic_source_stream_handlers?
    ;

basic_source_stream_handlers
    :(basic_source_stream_handler)+ 
    ;

basic_source_stream_handler
    : filter | stream_function
    ;

filter
    :'#'? '['expression']'
    ;

expression
    :math_operation
    ;
```