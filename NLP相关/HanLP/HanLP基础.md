# HanLP: Han Language Processing

## 介绍

面向生产环境的多语种自然语言处理工具包，基于PyTorch和TensorFlow 2.x双引擎，目标是普及落地最前沿的NLP技术。HanLP具备功能完善、精度准确、性能高效、语料时新、架构清晰、可自定义的特点。

借助世界上最大的多语种语料库，HanLP2.1支持包括简繁中英日俄法德在内的130种语言上的10种联合任务以及多种单任务。HanLP预训练了十几种任务上的数十个模型并且正在持续迭代语料库与模型：

| 功能                                                         |
|------------------------------------------------------------|
| [分词](https://hanlp.hankcs.com/demos/tok.html)              |
| [词性标注](https://hanlp.hankcs.com/demos/pos.html)            |
| [命名实体识别](https://hanlp.hankcs.com/demos/ner.html)          |
| [依存句法分析](https://hanlp.hankcs.com/demos/dep.html)          |
| [成分句法分析](https://hanlp.hankcs.com/demos/con.html)          |
| [语义依存分析](https://hanlp.hankcs.com/demos/sdp.html)          |
| [语义角色标注](https://hanlp.hankcs.com/demos/srl.html)          |
| [抽象意义表示](https://hanlp.hankcs.com/demos/amr.html)          |
| [指代消解](https://hanlp.hankcs.com/demos/cor.html)            |
| [语义文本相似度](https://hanlp.hankcs.com/demos/sts.html)         |
| [文本风格转换](https://hanlp.hankcs.com/demos/tst.html)          |
| [关键词短语提取](https://hanlp.hankcs.com/demos/keyphrase.html)   |
| [抽取式自动摘要](https://hanlp.hankcs.com/demos/exsum.html)       |
| [生成式自动摘要](https://hanlp.hankcs.com/demos/absum.html)       |
| [文本语法纠错](https://hanlp.hankcs.com/demos/gec.html)          |
| [文本分类](https://hanlp.hankcs.com/demos/classification.html) |
| [情感分析](https://hanlp.hankcs.com/demos/sentiment.html)      |
| [语种检测](https://hanlp.hankcs.com/demos/classification.html) |

目前2.X不支持JAVA直接启动，所以本文暂时以1.X来说明。

## 下载与配置

### Portable版

HanLP提供了使用较为便捷的Portable版本，需要在maven中引用：

```xml
<dependency>
    <groupId>com.hankcs</groupId>
    <artifactId>hanlp</artifactId>
    <!-- Feb 25, 2023 -->
    <version>portable-1.8.4</version>
</dependency>
```

零配置，即可使用基本功能（除由字构词、依存句法分析外的全部功能）。如果用户有自定义的需求，可以参考方式二，使用hanlp.properties进行配置（Portable版同样支持hanlp.properties）。

### Full版

HanLP将数据与程序分离，给予用户自定义的自由。

#### data文件

目前官方提供一个能用的数据集，但是相对来说比较早(2019年)。下载地址：[data.zip](http://nlp.hankcs.com/download.php?file=data)。

HanLP中的数据分为词典和模型，其中词典是词法分析必需的，模型是句法分析必需的。用户可以自行增删替换，如果不需要句法分析等功能的话，随时可以删除model文件夹。

#### 依赖与配置文件

下载地址：[hanlp-release.zip](http://nlp.hankcs.com/download.php?file=jar)。

配置文件的作用是告诉HanLP数据包的位置，只需修改第一行，最后将hanlp.properties放入classpath即可。

## 简单使用

Demo详见：[Demo](https://github.com/hankcs/HanLP/tree/1.x/src/test/java/com/hankcs/demo)。

## 命名实体识别

1.X的HanLP将命名实体识别分为两类：

1. 基于HMM角色标注的命名实体识别 （速度快）
2. 基于线性模型的命名实体识别（精度高）

说的通俗一点，就是第一种是隐马模型的字典形式，通过修改字典可以修改识别结果。

### 基于HMM角色标注的命名实体识别

#### 一个简单的例子

```java
@Slf4j
public class HanLPTest {
    @Test
    public void chineseNameSegment() {
        for (String s : Arrays.asList("吴俊贤", "潘治霖")) {
            log.info(HanLP.segment(s).toString());
        }
    }
}
```

输出结果为：

```text
[INFO ] 2024-06-04 10:54:55,346 method:com.hanlp.example.HanLPTest.chineseNameSegment(HanLPTest.java:17)
[吴俊贤/nr]
[INFO ] 2024-06-04 10:54:55,354 method:com.hanlp.example.HanLPTest.chineseNameSegment(HanLPTest.java:17)
[潘治霖/nr]
```

根据[词性标注集](http://www.hankcs.com/nlp/part-of-speech-tagging.html#h2-8)，或者查看`com.hankcs.hanlp.corpus.tag.Nature`，可以得知，两个人名都被正确的识别。

#### 修改字典内容以修正结果

现在使用一个模棱两可的词汇：

```java
@Slf4j
public class HanLPTest {
    @Test
    public void chineseNameSegment() {
        for (String s : Arrays.asList("陈兵")) {
            log.info(HanLP.segment(s).toString());
        }
    }
}
```

不出意外，被识别为了：

```text
[INFO ] 2024-06-04 11:05:47,592 method:com.hanlp.example.HanLPTest.chineseNameSegment(HanLPTest.java:17)
[陈兵/v]
```

根据hanlp.properties中的配置可知，用户自定义的字典下存在人名辞典.txt，在文件中添加：

```text
陈兵 nr 1
```

再运行(如不是重启jvm，需要删除缓存)，即可得到nr结果。

HanLP提供了运行时修改用户自定义字典的API：

```java
@Slf4j
public class HanLPTest {
    @Test
    public void chineseNameSegment() {
        for (String s : Arrays.asList("陈兵")) {
            log.info(HanLP.segment(s).toString());
        }

        CustomDictionary.add("陈兵", "nr 1");

        for (String s : Arrays.asList("陈兵")) {
            log.info(HanLP.segment(s).toString());
        }
    }
}
```

通过上述方法，也可以成功将陈兵识别为nr。但是通过代码动态增删不会保存到词典文件。

#### 用户自定义词典的说明

`CustomDictionary`是一份全局的用户自定义词典，可以随时增删，影响全部分词器。另外可以在任何分词器中关闭它。通过代码动态增删不会保存到词典文件。

`CustomDictionary`主词典文本路径是`data/dictionary/custom/CustomDictionary.txt`，用户可以在此增加自己的词语（不推荐）；也可以单独新建一个文本文件，通过配置文件CustomDictionaryPath=data/dictionary/custom/CustomDictionary.txt; 我的词典.txt;来追加词典（推荐）。
始终建议将相同词性的词语放到同一个词典文件里，便于维护和分享。

每一行代表一个单词，格式遵从`[单词] [词性A] [A的频次] [词性B] [B的频次] ...` 如果不填词性则表示采用词典的默认词性。

词典的默认词性默认是名词n，可以通过配置文件修改：`全国地名大全.txt ns;`如果词典路径后面空格紧接着词性，则该词典默认是该词性。

### 基于线性模型的命名实体识别

线性模型的命名实体识别也可以分成两种：

1. 感知机命名实体识别
2. CRF命名实体识别

#### 感知机命名实体识别

感知机的使用分为两种，HanLP的官方都提供了demo：

##### 不想训练模型

```java
@Slf4j
public class HanLPTest {
    @Test
    public void perceptronLexicalAnalyzer() {
        try {
            PerceptronLexicalAnalyzer analyzer = new PerceptronLexicalAnalyzer("D:\\code\\hanlp-test\\data\\data\\model\\perceptron\\pku199801\\cws.bin",
                    "D:\\code\\hanlp-test\\data\\data\\model\\perceptron\\pku199801\\pos.bin",
                    "D:\\code\\hanlp-test\\data\\data\\model\\perceptron\\pku199801\\ner.bin");

            log.info(analyzer.analyze("吴俊贤").toString());

            // 需要保存学习后的模型
            // analyzer.getPerceptronSegmenter().getModel().save("D:\\code\\hanlp-test\\data\\data\\model\\perceptron\\pku199801\\cws1.bin");
            // analyzer.getPerceptronPOSTagger().getModel().save("D:\\code\\hanlp-test\\data\\data\\model\\perceptron\\pku199801\\pos1.bin");
            // analyzer.getPerceptionNERecognizer().getModel().save("D:\\code\\hanlp-test\\data\\data\\model\\perceptron\\pku199801\\ner1.bin");
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }
}
```

输出为：

```text
[INFO ] 2024-06-04 14:02:26,919 method:com.hanlp.example.HanLPTest.perceptronLexicalAnalyzer(HanLPTest.java:22)
吴俊贤/nr
```

##### 需要训练模型

能通过命令行和API的方式进行训练，这里方便起见，通过API的方式做一个demo。

首先准备训练用的语料，本demo准备的是宗教信仰相关：

```text
基督教/Religion
伊斯兰教/Religion
佛教/Religion
道教/Religion
印度教/Religion
犹太教/Religion
天主教/Religion
东正教/Religion
摩门教/Religion
新教/Religion
天主教/Religion
东正教/Religion
摩门教/Religion
加尔文主义/Religion
伊斯兰教什叶派/Religion
伊斯兰教逊尼派/Religion
伊斯兰教四大法律学派/Religion
苏菲派/Religion
沙特阿拉伯瓦哈比派/Religion
什叶派/Religion
逊尼派/Religion
耶稣基督后期圣徒教会/Religion
摩门教/Religion
东正教/Religion
以色列教/Religion
斯瓦米纳拉扬教/Religion
巴哈伊信仰/Religion
神道教/Religion
儒家/Religion
耆那教/Religion
锡克教/Religion
索马里亚教/Religion
毗湿奴教/Religion
弥勒教/Religion
纳粹教/Religion
```

先来看看训练前的表现：

```java
    @Test
    public void test() {
        try {
            PerceptronLexicalAnalyzer analyzer = new PerceptronLexicalAnalyzer("D:\\code\\hanlp-test\\data\\data\\model\\perceptron\\pku199801\\cws2.bin",
                    "D:\\code\\hanlp-test\\data\\data\\model\\perceptron\\pku199801\\pos2.bin",
                    "D:\\code\\hanlp-test\\data\\data\\model\\perceptron\\pku199801\\ner2.bin");

            for (String s : Arrays.asList("犹太教", "天主教", "东正教", "摩门教", "新教", "天主教", "佛教", "一个天主教的教徒皈依佛教")) {
                log.info(analyzer.analyze(s).toString());
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }
```

输出的结果为：

```text
[INFO ] 2024-06-04 16:46:32,302 method:com.hanlp.example.HanLPTest.test(HanLPTest.java:28)
犹太教/nz
[INFO ] 2024-06-04 16:46:32,307 method:com.hanlp.example.HanLPTest.test(HanLPTest.java:28)
天主教/nz
[INFO ] 2024-06-04 16:46:32,308 method:com.hanlp.example.HanLPTest.test(HanLPTest.java:28)
东正教/nz
[INFO ] 2024-06-04 16:46:32,309 method:com.hanlp.example.HanLPTest.test(HanLPTest.java:28)
摩门教/nz
[INFO ] 2024-06-04 16:46:32,310 method:com.hanlp.example.HanLPTest.test(HanLPTest.java:28)
新教/n
[INFO ] 2024-06-04 16:46:32,310 method:com.hanlp.example.HanLPTest.test(HanLPTest.java:28)
天主教/nz
[INFO ] 2024-06-04 16:46:32,311 method:com.hanlp.example.HanLPTest.test(HanLPTest.java:28)
佛教/nz
[INFO ] 2024-06-04 16:46:32,315 method:com.hanlp.example.HanLPTest.test(HanLPTest.java:28)
一个/m 天主教/nz 的/u 教徒/n 皈依/v 佛教/nz
```

给与宗教一个特定的词性，方便起见假定为`Religion`。

训练的代码如下：

```java
    @Test
    public void testTrain() {
        try {
            PerceptronTrainer trainer2 = new CWSTrainer() {
                @Override
                protected TagSet createTagSet() {
                    CWSTagSet tagSet = new CWSTagSet();
                    tagSet.add("Religion");
                    return tagSet;
                }
            };
            ;
            PerceptronTrainer.Result result = trainer2.train(
                    "D:\\code\\hanlp-test\\宗教信仰.txt",
                    "D:\\code\\hanlp-test\\data\\data\\model\\perceptron\\pku199801\\cws2.bin"
            );

            PerceptronTrainer trainer3 = new POSTrainer() {
                @Override
                protected TagSet createTagSet() {
                    POSTagSet tagSet = new POSTagSet();
                    tagSet.add("Religion");
                    return tagSet;
                }
            };
            ;
            trainer3.train(
                    "D:\\code\\hanlp-test\\宗教信仰.txt",
                    "D:\\code\\hanlp-test\\data\\data\\model\\perceptron\\pku199801\\pos2.bin"
            );

            PerceptronTrainer trainer1 = new NERTrainer() {
                @Override
                protected TagSet createTagSet() {
                    NERTagSet tagSet = new NERTagSet();
                    tagSet.nerLabels.add("Religion");
                    return tagSet;
                }
            };
            trainer1.train(
                    "D:\\code\\hanlp-test\\宗教信仰.txt",
                    "D:\\code\\hanlp-test\\data\\data\\model\\perceptron\\pku199801\\ner2.bin"
            );
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }
```

训练结束后，再次运行识别方法，得到结果：

```text
[INFO ] 2024-06-04 17:37:18,433 method:com.hanlp.example.HanLPTest.test(HanLPTest.java:29)
犹太教/Religion
[INFO ] 2024-06-04 17:37:18,437 method:com.hanlp.example.HanLPTest.test(HanLPTest.java:29)
天主教/Religion
[INFO ] 2024-06-04 17:37:18,437 method:com.hanlp.example.HanLPTest.test(HanLPTest.java:29)
东正教/Religion
[INFO ] 2024-06-04 17:37:18,438 method:com.hanlp.example.HanLPTest.test(HanLPTest.java:29)
摩门教/nz
[INFO ] 2024-06-04 17:37:18,438 method:com.hanlp.example.HanLPTest.test(HanLPTest.java:29)
新教/Religion
[INFO ] 2024-06-04 17:37:18,438 method:com.hanlp.example.HanLPTest.test(HanLPTest.java:29)
天主教/Religion
[INFO ] 2024-06-04 17:37:18,439 method:com.hanlp.example.HanLPTest.test(HanLPTest.java:29)
佛教/Religion
[INFO ] 2024-06-04 17:37:18,440 method:com.hanlp.example.HanLPTest.test(HanLPTest.java:29)
一个/m 天主教/Religion 的/u 教徒皈/n 依佛教/Religion
```

#### CRF命名实体识别

Java版本学习效率极低。和感应机基本一致的流程，不展示了。