# Stanford CoreNLP

## 介绍

Stanford CoreNLP提供了一套Java编写的自然语言分析工具。

## 应用于Maven

本文使用Stanford CoreNLP的4.5.7版本。

需要使用如下几个依赖：

```xml
    <dependencies>
        <!-- 必要 -->
        <dependency>
            <groupId>edu.stanford.nlp</groupId>
            <artifactId>stanford-corenlp</artifactId>
            <version>4.5.7</version>
        </dependency>

        <!-- 若需要使用官方的模型，必要 -->
        <dependency>
            <groupId>edu.stanford.nlp</groupId>
            <artifactId>stanford-corenlp</artifactId>
            <version>4.5.7</version>
            <classifier>models</classifier>
        </dependency>

        <!-- 若使用其他语言，自行添加，例如models-english -->
        <dependency>
            <groupId>edu.stanford.nlp</groupId>
            <artifactId>stanford-corenlp</artifactId>
            <version>4.5.7</version>
            <classifier>models-chinese</classifier>
        </dependency>
    </dependencies>
```

注意，由于一些依赖包非常大，可以手动从[Maven repo](https://repo1.maven.org/maven2/edu/stanford/nlp/stanford-corenlp/4.5.7/)下载。

## 快速开始

Stanford CoreNLP提供了两套访问API，分别是3.9.0之前使用的`void annotate(Annotation annotation)`和最新推出的`annotate(CoreDocument document)`。

其实还有一种简易API，但是好像没办法直接跑中文。

```java
@Slf4j
public class NLPTest {
    @Test
    public void oldAPI() {
        StanfordCoreNLP pipeline = new StanfordCoreNLP("StanfordCoreNLP-chinese");
        Annotation annotation = new Annotation("巴拉克·奥巴马是美国总统。他在2008年当选");
        pipeline.annotate(annotation);

        List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
        for (CoreMap sentence : sentences) {
            // traversing the words in the current sentence
            // a CoreLabel is a CoreMap with additional token-specific methods
            // CoreLabel可以近似理解成一个键值对，需要每次从里面get数据
            for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                // this is the text of the token
                String word = token.get(CoreAnnotations.TextAnnotation.class);
                // this is the POS tag of the token
                String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                // this is the NER label of the token
                String ne = token.get(CoreAnnotations.NamedEntityTagAnnotation.class);
                log.info("word: {}, pos: {}, ner: {}", word, pos, ne);
            }
        }

        Throwable ex = annotation.get(CoreAnnotations.ExceptionAnnotation.class);
        if (ex != null) {
            log.error(ex.getMessage());
        }
    }

    @Test
    public void newAPI() {
        StanfordCoreNLP pipeline = new StanfordCoreNLP("StanfordCoreNLP-chinese");
        CoreDocument document = new CoreDocument("巴拉克·奥巴马是美国总统。他在2008年当选");
        pipeline.annotate(document);

        for (CoreSentence sentence : document.sentences()) {
            for (int i = 0; i < sentence.tokens().size(); i++) {
                log.info("word: {}, pos: {}, ner: {}", sentence.tokens().get(i).originalText(), sentence.posTags().get(i), sentence.nerTags().get(i));
            }
        }
    }
}
```

两种API的返回结果是一致的：

```text
[INFO ] 2024-06-07 14:44:23,910 method:com.test.NLPTest.newAPI(NLPTest.java:61)
word: 巴拉克·奥巴马, pos: NR, ner: PERSON
[INFO ] 2024-06-07 14:44:23,911 method:com.test.NLPTest.newAPI(NLPTest.java:61)
word: 是, pos: VC, ner: O
[INFO ] 2024-06-07 14:44:23,911 method:com.test.NLPTest.newAPI(NLPTest.java:61)
word: 美国, pos: NR, ner: COUNTRY
[INFO ] 2024-06-07 14:44:23,911 method:com.test.NLPTest.newAPI(NLPTest.java:61)
word: 总统, pos: NN, ner: TITLE
[INFO ] 2024-06-07 14:44:23,911 method:com.test.NLPTest.newAPI(NLPTest.java:61)
word: 。, pos: PU, ner: O
[INFO ] 2024-06-07 14:44:23,912 method:com.test.NLPTest.newAPI(NLPTest.java:61)
word: 他, pos: PN, ner: O
[INFO ] 2024-06-07 14:44:23,912 method:com.test.NLPTest.newAPI(NLPTest.java:61)
word: 在, pos: P, ner: O
[INFO ] 2024-06-07 14:44:23,912 method:com.test.NLPTest.newAPI(NLPTest.java:61)
word: 2008年, pos: NT, ner: DATE
[INFO ] 2024-06-07 14:44:23,912 method:com.test.NLPTest.newAPI(NLPTest.java:61)
word: 当选, pos: VV, ner: O

[INFO ] 2024-06-07 14:44:23,979 method:com.test.NLPTest.oldAPI(NLPTest.java:44)
word: 巴拉克·奥巴马, pos: NR, ner: PERSON
[INFO ] 2024-06-07 14:44:23,979 method:com.test.NLPTest.oldAPI(NLPTest.java:44)
word: 是, pos: VC, ner: O
[INFO ] 2024-06-07 14:44:23,979 method:com.test.NLPTest.oldAPI(NLPTest.java:44)
word: 美国, pos: NR, ner: COUNTRY
[INFO ] 2024-06-07 14:44:23,980 method:com.test.NLPTest.oldAPI(NLPTest.java:44)
word: 总统, pos: NN, ner: TITLE
[INFO ] 2024-06-07 14:44:23,980 method:com.test.NLPTest.oldAPI(NLPTest.java:44)
word: 。, pos: PU, ner: O
[INFO ] 2024-06-07 14:44:23,980 method:com.test.NLPTest.oldAPI(NLPTest.java:44)
word: 他, pos: PN, ner: O
[INFO ] 2024-06-07 14:44:23,980 method:com.test.NLPTest.oldAPI(NLPTest.java:44)
word: 在, pos: P, ner: O
[INFO ] 2024-06-07 14:44:23,980 method:com.test.NLPTest.oldAPI(NLPTest.java:44)
word: 2008年, pos: NT, ner: DATE
[INFO ] 2024-06-07 14:44:23,981 method:com.test.NLPTest.oldAPI(NLPTest.java:44)
word: 当选, pos: VV, ner: O
```

### 代码说明

CoreNLP包主要由两个部分组成，这两部分又包含在`AnnotationPipeline`(就是例子中使用的`StanfordCoreNLP`)中：

1. Annotation：保存Annotator生成的结果，包括词性标签、命名实体识别等
2. Annotator：类似函数概念，目的是执行分词、解析、命名实体识别等流程。目前支持的全Annotator如下：[Full list of Annotators](https://stanfordnlp.github.io/CoreNLP/annotators.html)

在上面的例子中，没有显示地指定Annotator，而是通过配置文件的形式将配置项传入`AnnotationPipeline`。

Pipeline也是CoreNLP中非常重要的概念：

![Pipeline](https://stanfordnlp.github.io/CoreNLP/assets/images/pipeline.png)

## How can I train my own NER model?

[参考地址1](https://nlp.stanford.edu/software/crf-faq.html)
[参考地址2](https://medium.com/swlh/stanford-corenlp-training-your-own-custom-ner-tagger-8119cc7dfc06)

先准备好训练集，执行如下命令：

```shell
# austen.prop的文件内容可以参考上面两个网址
D:\Local_repository\edu\stanford\nlp\stanford-corenlp\4.5.7>java -cp stanford-corenlp-4.5.7.jar edu.stanford.nlp.ie.crf.CRFClassifier -prop austen.prop
```

训练结束后会在指定目录下生成一个模型文件，通过如下代码进行测试：

```java
    @Test
    public void testModel() {
        CRFClassifier<CoreMap> userDefinedModel = CRFClassifier.getClassifierNoExceptions("D:\\code\\nlp-for-stanfordnlp\\ner-model.ser.gz");
        for (String str : Arrays.asList(
                "犹太教", "天主教", "东正教", "摩门教", "新教", "天主教", "佛教", // 宗教
                "吴俊贤", "潘治霖", "王健", "方耀民", // 人名
                "dafeihou@hillstone.com", "dafeihou@163.com", "hs-china@hillstonenet.com", // 邮箱
                "1.1.1.1", "10.182.3.1", "127.0.0.1", "123.222.33.1", // IPv4
                "3ffe:1900:4545:3:200:f8ff:fe21:67cf", "fe80:0000:0000:0000:0204:61ff:fe9d:f156", "2001:db8::ff00:42:8329", //IPv6
                "京AF0236", "粤F0A943", "苏UL5616", "苏UP8826", // 车牌号
                "男", "女", "Female", "man", // 性别
                "汉族", "维吾尔族", "白族", // 民族
                "2020/01/01" // 日期
        )) {
            log.info(userDefinedModel.classifyToString(str));
        }
    }
```

运行结果就不放了，说实话，这个正确率有点感人，也可能是训练集比较小或者某些训练的配置不对。