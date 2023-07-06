# Flink数据传递

## 内存结构

### MemorySegment

内存段，Flink中内存的最小单元。

```java
public final class MemorySegment {
    ...
    //内置了get、put、copy、swap等方法
    ...
}
```

### DataInputView(读取) & DataOutputView(输入)

内存页，数据访问视图；分为输入和输出。
视图可以自动实现跨内存段的读取和写入。

每个视图中持有多个内存段MemorySegment的引用，即MemorySegment[]，这一组内存段被视为内存页(Memory Page)。

```java
public interface DataInputView extends DataInput {
    //跳过一部分byte
    void skipBytesToRead(int numBytes) throws IOException;
    //从off开始读取len个字节，写入b中；返回实际读取的字节数量
    int read(byte[] b, int off, int len) throws IOException;
    //填充b
    int read(byte[] b) throws IOException;
}

public interface DataOutputView extends DataOutput {
    //跳过一部分byte
    void skipBytesToWrite(int numBytes) throws IOException;
    //将给定个字节复制到给定视图
    void write(DataInputView source, int numBytes) throws IOException;
}
```

除此之外，还存在DataInputViewStream、DataInputViewStreamWrapper及同样的output类。

### Buffer

Task算子处理数据后，数据在网络层面传输使用的载体即Buffer，其实现类是NetworkBuffer。
NetworkBuffer底层的实现依然是MemorySegment。

Buffer中有计数的概念，当当前Buffer被新的消费者(BufferConsumer)消费时，计数加一；消费完，计数减一；当计数清零时，释放该Buffer。

```java
public interface Buffer {
    ...
}

public class NetworkBuffer {
    ...
}
```

BufferPool用于管理全部的Buffer，包括Buffer的申请、销毁等，实现类是LocalBufferPool。为了避免性能和并发问题，每个task都有独立的LocalBufferPool，并为subtask分配buffer。

BufferPoolFactory提供了BufferPool的创建和销毁，唯一实现类是NetworkBufferPool。每个TaskManager只能有一个NetworkBufferPool，在taskmanager创建时创建。

NetworkBufferPool除了创建BufferPoolFactory外，还提供所需的MemorySegment。

```java
public interface BufferPool extends BufferProvider, BufferRecycler {
    ...
}

public interface BufferPoolFactory {
    BufferPool createBufferPool(int numRequiredBuffers, int maxUsedBuffers) throws IOException;

    BufferPool createBufferPool(
            int numRequiredBuffers,
            int maxUsedBuffers,
            int numSubpartitions,
            int maxBuffersPerChannel)
            throws IOException;

    void destroyBufferPool(BufferPool bufferPool) throws IOException;
}

public class NetworkBufferPool {
    ...
}
```