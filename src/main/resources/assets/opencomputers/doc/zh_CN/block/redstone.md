# Redstone I/O

![Hi Red.](oredict:oc:redstone)

红石IO方块可以被用来远程读取和发射红石信号. 他就像1级和2级[红石卡](../item/redstoneCard1.md)的杂交品: 

可以收发简单的信号也可以收发信号群, 但是无法收发无线红石信号

When providing a side to the methods of the component exposed by this block, the directions are the global principal directions, i.e. it is recommended to use `sides.north`, `sides.east` and so on.

如[红石卡](../item/redstoneCard1.md), 当红石变化的时候，这个方块会向连接的[电脑](../general/computer.md) 发送信号，（模拟或者信号束）.也可以用来唤醒连接的[电脑](../general/computer.md)
如果达到一定的强度可以直接把电脑开机
