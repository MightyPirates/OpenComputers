# OpenOS

OpenOS 是OpenComputers里面的基本操作系统. 必须用它来进行[电脑](computer.md)的第一次启动, 可以通过用空的 [软盘](../item/floppy.md) 和 [手册](../item/manual.md) 合成.

合成后, [此软盘](../item/floppy.md) 就可以放进连接电脑的 [软驱](../block/diskDrive.md)来启动电脑，OpenOS的软盘是只读的，所以推荐将系统安到硬盘里面。如果要安装到硬盘，请在启动完成后在shell敲入install，选择OpenOS的编号

OpenOS可以在除了单片机和无人机外的任何设备安装，安装好了以后，软盘可以被拔出

OpenOS拥有大量内建命令, 最有用的是lua，将打开lua解释器，这里可以测试命令, 在将命令写入lua文件脚本前试验组件api.注意解释器启动时的信息，会告诉你如何显示命令结果，如何退出。

要得到编程的信息请转到[Lua Programming](lua.md) . 运行脚本的话敲入文件名回车就行了(比如, 通过在shell敲下script，就可以运行`script.lua`).
