# OpenOS

OpenOS 是 OpenComputers 的基本操作系统。[电脑](computer.md)的第一次启动必须用它来引导。它可以通过用空的[软盘](../item/floppy.md) 和[手册](../item/manual.md)在工作台中合成获得。

合成出[此软盘](../item/floppy.md)后，它就可以放进与[电脑](computer.md)相连的[软驱](../block/diskDrive.md)中来启动 OpenOS。  
启动完成后，推荐将系统安装到空[硬盘](../item/hdd1.md)里面，这样就不用[软盘](../item/floppy.md)了，同时也能获得读写硬盘的能力（包括 OpenOS 安装软盘在内的所有软盘都是只读的）。最高级的[机箱](../block/case3.md)自带一个软驱，所以如果使用了这个机箱就不需要额外安装软驱。

要安装 OpenOS，只需要在启动完后的界面中输入 `install`，然后按提示操作就好。[软盘](../item/floppy.md)可以在系统重启完后取出。OpenOS 可在除了[无人机](../item/drone.md)和[单片机](../block/microcontroller.md)之外的所有设备上安装。（无人机和单片机都需要使用 [EEPROM](../item/eeprom.md) 编程方能使用，因为它们都没有文件系统。）

OpenOS 拥有大量内建命令，最有用的是 `lua`，运行后会打开 Lua 解释器。在正式开始编写 .lua 脚本前，你可以再这里测试各种命令及组件 API。注意解释器启动时的信息，它会告诉你如何显示命令结果，以及如何退出。

关于编程的信息请参考 [Lua Programming](lua.md)。要运行脚本的话，敲入文件名回车就行了（比如你的脚本叫 `script.lua`，那只需要在终端中敲 `script` 然后敲回车就可以了）。
