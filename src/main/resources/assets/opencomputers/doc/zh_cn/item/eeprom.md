# EEPROM

![让我们开始吧。](oredict:opencomputers:eeprom)

EEPROM 中包含了在电脑启动后引导其完成初始化的代码。这些代码以字节数组的形式保存，在不同的 [CPU](cpu1.md) 架构上有不同的含义，比如对于 Lua BIOS 来说这可能是搜索可用文件系统的初始化代码，对于其他设备这可能是机器码。

EEPROM 可根据特殊情况专门进行烧录，比如用于[无人机](drone.md)和[微控制器](../block/microcontroller.md)的 EEPROM 就需要专门的 EEPROM。
