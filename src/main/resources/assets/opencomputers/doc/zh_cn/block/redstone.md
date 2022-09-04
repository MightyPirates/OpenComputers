# 红石 I/O 端口

![Hi Red.](oredict:opencomputers:redstone)

红石 I/O 端口可用来远程读取和发射红石信号。它就像是 T1 和 T2 [红石卡](../item/redstoneCard1.md)的合体，可以收发简单的模拟信号及捆绑信号，但是无法收发无线红石信号。

在调用红石 I/O 端口的 API 时，建议使用 `sides.north` 和 `sides.east` 这样的方向常量，因为这里的方向指的是全局的基准方向。  

和[红石卡](../item/redstoneCard1.md)一样，当红石信号变化的时候，不论是模拟信号还是捆绑信号，红石 I/O 端口会向连接的[电脑](../general/computer.md)发送信号。红石 I/O 端口也可以用来唤醒相连的[电脑](../general/computer.md)；信号达到一定的强度可以直接让电脑启动。
