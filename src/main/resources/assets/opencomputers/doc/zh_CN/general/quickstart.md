# 快速入门

这篇文章叫做"如何组装第一台电脑". 为了使你的[电脑](computer.md)运作, 你要正确的设定他. 我们有多个档次的电脑, 但我们会从基础档开始.

**Disclaimer**: 这是手把手的教程, 会告诉你后面出现问题要怎么处理, 因此这教程比较长. 如果现实中没有装机经历, 并且你是萌新, 那么请耐心读完.

首先你需要一个[机箱](../block/case1.md). 这个方块将容纳你的电脑配件, 并定义你电脑的行为.

![A tier two computer case.](oredict:oc:case2)

比如你要挑一个适合你的 [显卡](../item/graphicsCard1.md)(GTX690,GTX1080,划掉), 还可能需要一个[网卡](../item/lanCard.md), 一块 [红石卡](../item/redstoneCard1.md) , 如果你是可以调创造的狗管理你还可以拿出 [调试卡](../item/debugCard.md).

打开[机箱](../block/case1.md)你会看到一系列的槽位. 上面写了槽位的数量,可以放什么样的配件进去,等 (槽位小字有写),这些数据取决于机箱档次.

![GUI of a tier two computer case.](opencomputers:doc/img/configuration_case1.png)

如果没放东西, [机箱](../block/case1.md)完全没用. 如果你试图按下 [电脑](computer.md)的开机键, 他只会立刻在你的聊天框输出一条错误, 用蜂鸣声提示你. 幸运的是他提醒了你要怎么去做: 电脑需要能源. 把你的电脑接发电机, 或者是接到[能量转换器](../block/powerConverter.md).

这次启动, 他会告诉你电脑需要 [CPU](../item/cpu1.md). CPU分不同级别,越高级的要越多的东西合成,执行速度也快.选择一款CPU,丢进你的[机箱](../block/case1.md).

好的这次他要你放[内存](../item/ram1.md)了 . 注意警报变化了: 长-短. 越高级的 [内存](../item/ram1.md)容量越大,跑的程序越多. 运行 [OpenOS](openOS.md), 这篇教程的目标, 需要至少2条T1 [内存](../item/ram1.md).

ok,做的很好,现在[机箱](../block/case1.md) 变成这样了:

![Partially configured computer.](opencomputers:doc/img/configuration_case2.png)

别急, 现在虽然不会打印错误了, 但是他也什么都做不了. 至少他还会发出两声警报. 这意味着电脑进入执行状态,但是未能成功. In other words: it technically runs! This is where a very useful tool comes into play: the [analyzer](../item/analyzer.md). This tool allows inspecting many of OpenComputers' blocks, as well as some blocks from other mods. To use it on the [computer](computer.md), use the [analyzer](../item/analyzer.md) on the case while sneaking.

你会在聊天框看到 [电脑](computer.md) 遇到的错误:

`no bios found; install configured EEPROM`

这说明:你需要一个刷写过程序的E2PROM芯片,并装入机箱

注意那个 configured,需要刷一个程序.合成E2PROM很简单,但是刷写程序需要电脑,是不是难了点,这里我们要直接合成一个预刷写LUA BIOS的E2PROM,使用E2PROM和你的手册合成,将这个ROM丢进机箱,开机！

啊哈哈,还是啥都没有,但是我们看到了一条信息

`no bootable medium found; file not found`

说明Lua BIOS运行正常,它执行了对文件系统的搜索,如根目录含有init.lua的[软盘](../item/floppy.md)或者[硬盘](../item/hdd1.md),因此我们现在需要一张带有OpenOS安装的软盘(用空磁盘+手册合成)

嗯,如果你用了T2机箱,那么你还要做一个[软盘驱动器](../block/diskDrive.md)来读盘,如果是T3及以上那么直接放到机箱就行了,如果使用驱动器,那么将驱动器放在电脑边上,插入软盘,启动

好的,他启动了.如果有什么问题的话,可以使用[分析仪](../item/analyzer.md)排查.不过我们的电脑应该跑起来了.

最难的部分已经过去了,剩下就是如何让电脑输出信息,并且让电脑接受输入

你需要将一个[屏幕](../block/screen1.md)安放在电脑上 ,并在机箱安装[显卡](../item/graphicsCard1.md)

![No, it's not a flatscreen.](oredict:oc:screen2)

之后你就可以看到电脑的输出,然后电脑就会等待你的操作了,尝试一下吧！敲入 'lua' 并按下回车,你将会得到lua交互解释器的帮助(英文),你可以测试基本lua命令,更多信息见[the Lua page](lua.md)

![It lives!](opencomputers:doc/img/configuration_done.png)

之后就可以建造更复杂的 [电脑](computer.md), [服务器](../item/server1.md) ,用 [组装机](../block/assembler.md) 组装[机器人](../block/robot.md), [无人机](../item/drone.md), [单片机](../block/microcontroller.md) 和 [平板](../item/tablet.md).

Happy coding!