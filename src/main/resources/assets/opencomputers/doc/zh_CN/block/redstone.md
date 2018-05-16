# 红石I/O接口

![Hi Red.](oredict:oc:redstone)

红石IO接口可以被用来远程读取和发射红石信号. 他就像1级和2级[红石卡](../item/redstoneCard1.md)的杂交品: 它可以收发简单的红石信号, 也可以收发集束线缆的信号, 但是无法收发无线红石信号.

当你调用这个方块的component时你要提供方向, 这时候你要提供全局方向, 比如说`sides.north`, `sides.east`等等.

类似于[红石卡](../item/redstoneCard1.md), 当红石变化的时候，这个方块会向连接的[电脑](../general/computer.md) 发送信号（模拟红石信号或者集束红石信号都可以）. 它也可以唤醒连接的[电脑](../general/computer.md), 只要输入信号超过了自启的阈值.