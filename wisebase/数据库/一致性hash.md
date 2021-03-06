# 一致性Hash原理

在解决分布式系统中负载均衡的问题，可以使用Hash算法让固定的一部分请求落在同一部服务器上，这样每台服务器固定处理一部分请求，起到负载均衡的作用。

但是普通的取余Hash算法，比如用户ID%服务器数，伸缩性很差，当新增或者下线服务器的时候，用户ID和服务器的映射关系会被破坏失效。

## 一致性Hash概述

举个例子：有4台服务器，ip1，ip2，ip3和ip4。

- 定义一个Hash环，其值为0～ 一个正整数

- 首先计算4个ip地址对应的Hash值，找到ip地址在Hash环上的位置


- 当客户在客户端进行请求的时候，根据hash值（用户ID）计算路由规则，然后计算得到的Hash值落在Hash环的哪个地方，然后以顺时针的方向找距离最近的ip作为路路由ip


![](http://p5s0bbd0l.bkt.clouddn.com/hash2.jpg)

- 观察上图可知
	- user1和user2请求会交给ip2处理
	- user3的请求会交给ip3处理
	- user4的请求会交给ip4处理
	- user5和user6的请求会交给ip1处理

- 接下来：如果ip2挂了怎么办？
根据顺时针的规则，user1和user2的请求会交给ip3处理，而其他用户的请求不受影响。

![](http://p5s0bbd0l.bkt.clouddn.com/hash3.jpg)

- 另一种情况：现在新增一个ip5服务器，会怎么样？
根据Hash值可以计算出ip5在环上对应的位置，而user5的请求就不再是交给ip1处理了，而是交给顺时针最近的ip5服务器处理。

![](http://p5s0bbd0l.bkt.clouddn.com/hash4.jpg)


## 一致性Hash的特性

- 单调性：在Hash中增加新的服务器，应该保证原先的请求被映射到新的服务器，或者是原先映射的服务器，而不会有其他选择。

- 分散性：好的Hash算法应该降低分散性。这里的分散性是针对用户来说的，应该尽量让同一个用户（ID作为标识）的请求交给同一个服务器进行处理。

- 平衡性：即负载均衡。一致性Hash可以做到每个服务器都进行处理请求，但是不能保证每个服务器处理的请求数量大致相同。如下图，负载均衡的效果很差。

![](http://p5s0bbd0l.bkt.clouddn.com/hash5.jpg)


## 虚拟节点

虚拟节点的出现是为了增强一致性Hash的平衡性。

一致性Hash倾斜的问题可以增加多台机器来解决，另一种方法是添加虚拟节点，如下图，为每个物理机器引入一个虚拟节点。

![](http://p5s0bbd0l.bkt.clouddn.com/hash6.jpg)


## 均匀一致性Hash

虚拟节点的生成算法在不好的情况下，在每个服务器引入一个虚拟节点后，情况有所改善，但还是不够均匀，可能会得到如下的环：

![](http://p5s0bbd0l.bkt.clouddn.com/hash7.jpg)


均匀的一致性Hash应该是如下图：

![](http://p5s0bbd0l.bkt.clouddn.com/hash8.jpg)
