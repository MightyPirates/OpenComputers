# CPU

![脑——子——](oredict:opencomputers:cpu1)

中央处理器是[电脑](../general/computer.md)和[服务器](server1.md)的核心，定义了[电脑](../general/computer.md)的架构，并决定了可连接组件的数量。级别越高，每 tick 可以进行的函数调用越多。一言以蔽之，级别越高跑得越快。

CPU 可控制的组件上限：
- T1：至多 8 组件。
- T2：至多 12 组件。
- T3：至多 16 组件。

对[服务器](server1.md)来说，还可以用[组件总线](componentBus1.md)来连接更多的组件。

如果连接的组件数量超过上限，会令机器无法开机，对于运行中的机器来说则是宕机。
