# OpenOS

OpenOS是OC模组的基本操作系统。首次启动[电脑](computer.md)时必须有OpenOS软盘，它可以通过空[软盘](../item/floppy.md)和开放式电脑[手册](../item/manual.md)在工作台中合成获得。

完成了合成后，可将此[软盘](../item/floppy.md)插入[软盘驱动器](../block/diskDrive.md)，[软盘驱动器](../block/diskDrive.md)需要连接到[正确配置的](quickstart.md)[电脑](computer.md)系统上。这样[电脑](computer.md)就能启动OpenOS了。 
启动后，推荐将系统安装到空[硬盘](../item/hdd1.md)中，以摆脱对[软盘](../item/floppy.md)的依赖，并获得可读写的文件系统（包括OpenOS软盘在内的所有“战利品”[软盘](../item/floppy.md)都是只读的）。T3的[机箱](../block/case3.md)无需外置[软盘驱动器](../block/diskDrive.md)，因为它内置了一个[软盘](../item/floppy.md)槽位。

OpenOS的安装很简单：运行`install`命令，并按照屏幕上的提示语完成安装即可。[软盘](../item/floppy.md)在系统完成了重启后即可取出。OpenOS可安装于所有设备，除了[无人机](../item/drone.md)与[微控制器](../block/microcontroller.md)（二者均需要手动在[EEPROM](../item/eeprom.md)中编写程序才能实现功能，因为它们没有内置文件系统）。

OpenOS有大量内置功能，其中最有用的是`lua`命令，运行后会打开一个Lua解释器。这里很适合在向.lua文件写入命令前测试各种命令，以及进行组件API实验。注意解释器启动时显示的信息，它会告诉你如何显示命令结果，以及如何退出。

要获取更多有关编程的信息，请参考[Lua编程](lua.md)页面。要运行Lua脚本的话，只需输入文件名并按下回车即可（例如，在终端中输入`script`并按回车键即可运行名为`script.lua`的脚本）。
