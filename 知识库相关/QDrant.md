# QDrant

QDrant（Quantum-Drant）是一款开源的 向量数据库（Vector Database），专门用于高效存储、管理和搜索高维向量数据。

## 基本概念

### Collection 集合

集合类似MYSQL的表，用于存储同一类的向量数据，集合中的每一条数据被称为点(points)。

同一个集合中的点的向量必须具有相同的维度，并通过单一度量标准进行比较。

### Points 点

创建集合后，可以向集合中添加向量数据，QDrant中的向量数据都用points表示。

一个点数据包含id、payload(可选)、vector三部分。

#### payload 负载

一个点上的负载可以存储额外的信息，如标签或其他属性，用于搜索时用于过滤或排序结果。