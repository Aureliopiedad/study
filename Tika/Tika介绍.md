# Tika介绍与简单使用

## 介绍

Tika是一种用于检测和提取不同类型的文件的元数据和文本的工具，支持的类型多达千种，包括但不限于PPT、XLS、PDF等。

## 使用

Tika主要依赖于两个接口，分别是`Parser`和`Detector`。

### Parser

Tika提供的一个通用入口，被用来解析各种文件，用于提取结构化文本内容和元数据。

最底层的对外入口是：

```java
void parse(
	InputStream stream, 
	ContentHandler handler, 
	Metadata metadata, 
	ParseContext context
)throws IOException, SAXException, TikaException;
```

其中，使用的四个参数说明如下：

- stream：必要的输入，是需要解析的文件流
- handler：接受转化后的SAX event，作为`Parser`输出的一部分
- metadata：被解析的文档的元数据，既是输入也是输出
- context：一部分上下文信息

当然在实际使用中，不需要使用这么底层的接口，Tika提供了一些默认实现。

### RTF文件

富文本格式，Rich Text Format。其实说白了就是word文件(doc\docx)的弱化(通用)版本，和word文件不同的是，RTF文件为了跨平台跨应用，使用的是纯文本格式；这点和word文件有比较大的不同，word文件是word专用的格式，文件结构更复杂，包含二进制和XML数据。

### Tika解析RTF文件

为了尽可能地解析RTF文件中不同的样式，我们通过SAX的方式解析成XHTML：

```java
    @Test
    public void testParse1() {
        try (FileInputStream fileInputStream = new FileInputStream("D:\\code\\tika-test\\src\\test\\resources\\新建 Microsoft Word 文档.rtf")) {

            AutoDetectParser parser = new AutoDetectParser();
            ToXMLContentHandler contentHandler = new ToXMLContentHandler();
            Metadata metadata = new Metadata();
            
            parser.parse(
                    // 待解析的文档，以字节流形式传入，可以避免tika占用太多内存
                    fileInputStream,
                    // 内容处理器，用来收集结果，Tika会将解析结果包装成XHTML SAX event进行分发，通过ContentHandler处理这些event就可以得到文本内容和其他有用的信息
                    contentHandler,
                    // 元信息
                    metadata
            );
            log.info(contentHandler.toString());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
```

得到这样的结果：

```shell
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta name="X-TIKA:Parsed-By" content="org.apache.tika.parser.DefaultParser" />
<meta name="X-TIKA:Parsed-By" content="org.apache.tika.parser.microsoft.rtf.RTFParser" />
<meta name="Content-Type" content="application/rtf" />
<title></title>
</head>
<body><p><b>标题1</b></p>
<p><b />标题2</p>
<p>标题3</p>
<p>标题4</p>
<p>标题5</p>
<p>正文</p>
<p />
<ol>	<li>有序列表1</li>
	<li>多级列表1.1</li>
	<li>多级列表1.1.1</li>
</ol>
<p>多级列表1.1.2</p>
<ol>	<li>多级列表1.2</li>
	<li>有序列表2</li>
</ol>
<p />
<p>l	无序列表1</p>
<p>n	多级列表1.1</p>
<p>u	多级列表1.1.1</p>
<p>u	多级列表1.1.2</p>
<p>n	多级列表2.1</p>
<p>l	无序列表2</p>
<p />
<p>两端对齐</p>
<p>左对齐</p>
<p>居中</p>
<p>右对齐</p>
<p>不同颜色</p>
<p />
<p>不同底纹</p>
<p />
<p>这</p>
<p>是</p>
<p>表</p>
<p>格</p>
<p />
<p><b>粗</b> <i>斜</i> 下划线 删除线 下标下标 上标上标</p>
<p>这是图片:</p>
<div class="package-entry"><h1>file_0.wmf</h1>
</div>
<p />
<p />
<p />
</body></html>
```

但是同样的文件内容，如果是word格式的话，解析出的结果是：

```shell
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta name="cp:revision" content="3" />
<meta name="extended-properties:AppVersion" content="16.0000" />
<meta name="meta:paragraph-count" content="1" />
<meta name="meta:word-count" content="28" />
<meta name="extended-properties:Application" content="Microsoft Office Word" />
<meta name="meta:last-author" content="DaFei Hou" />
<meta name="dc:creator" content="xxx" />
<meta name="extended-properties:Company" content="" />
<meta name="xmpTPg:NPages" content="2" />
<meta name="dcterms:created" content="2024-08-29T02:23:00Z" />
<meta name="meta:line-count" content="1" />
<meta name="dcterms:modified" content="2024-08-29T02:38:00Z" />
<meta name="meta:character-count" content="163" />
<meta name="extended-properties:Template" content="Normal.dotm" />
<meta name="meta:character-count-with-spaces" content="190" />
<meta name="X-TIKA:Parsed-By" content="org.apache.tika.parser.DefaultParser" />
<meta name="X-TIKA:Parsed-By" content="org.apache.tika.parser.microsoft.ooxml.OOXMLParser" />
<meta name="extended-properties:DocSecurityString" content="None" />
<meta name="meta:page-count" content="2" />
<meta name="Content-Type" content="application/vnd.openxmlformats-officedocument.wordprocessingml.document" />
<meta name="dc:publisher" content="" />
<title></title>
</head>
<body><p class="header">页眉(左)	页眉(中)	页眉(右)</p>
<h1>标题1</h1>
<h2>标题2</h2>
<h3>标题3</h3>
<h4>标题4</h4>
<h5>标题5</h5>
<p>正文</p>
<p />
<p class="list_Paragraph">1， 有序列表1</p>
<p class="list_Paragraph">a) 多级列表1.1</p>
<p class="list_Paragraph">i. 多级列表1.1.1</p>
<p class="list_Paragraph">ii. 多级列表1.1.2</p>
<p class="list_Paragraph">b) 多级列表1.2</p>
<p class="list_Paragraph">2， 有序列表2</p>
<p />
<p class="list_Paragraph">· 无序列表1</p>
<p class="list_Paragraph">· 多级列表1.1</p>
<p class="list_Paragraph">· 多级列表1.1.1</p>
<p class="list_Paragraph">· 多级列表1.1.2</p>
<p class="list_Paragraph">· 多级列表2.1</p>
<p class="list_Paragraph">· 无序列表2</p>
<p />
<p>两端对齐</p>
<p>左对齐</p>
<p>居中</p>
<p>右对齐</p>
<p>不同颜色</p>
<p />
<p>不同底纹</p>
<p />
<table><tbody><tr>	<td><p>这</p>
</td>	<td><p>是</p>
</td></tr>
<tr>	<td><p>表</p>
</td>	<td><p>格</p>
</td></tr>
</tbody></table>
<p />
<p><b>粗</b> <i>斜</i> <u>下划线</u> <s>删除线</s> 下标下标 上标上标</p>
<p>这是图片:</p>
<p><img src="embedded:image1.png" alt="" /></p>
<p class="footer">页脚(左)	页脚(中)	页脚(右)</p>
<div class="package-entry"><h1>image1.png</h1>
</div>
</body></html>
```

两者一对比，就能发现rtf文件比word文件缺失了很多元数据、文本数据的描述信息(xml属性)等，页眉页脚信息也丢掉了。

### RTF格式的识别：

详细的代码在`org.apache.tika.detect.MagicDetector#detect`，大概是通过判断文件流(`ByteArrayInputStream`)的前五位是否是`[123, 92, 144, 116, 102]`，如果通过，则认为该文件是`application/rtf`类型。
