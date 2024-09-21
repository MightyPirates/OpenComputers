# EEPROM

![派对，启动！](oredict:oc:eeprom)

EEPROM中包含了用于在电脑引导启动过程中进行初始化的代码。其中的数据以简单字节数组的形式存储，且不同[CPU](cpu1.md)架构的代码内容可能不同。例如，对Lua而言这段代码通常是一个简单脚本，能查找带有init（初始化）脚本的文件系统，而对其他架构而言可能是实际的机器码。

EEPROM可被编程用于特殊用途，例如[无人机](drone.md)与[微控制器](../block/microcontroller.md)。
