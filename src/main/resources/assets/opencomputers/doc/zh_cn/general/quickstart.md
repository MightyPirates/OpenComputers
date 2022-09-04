# 快速入门

这篇文章又叫《如何组装第一台电脑》。为了让你的[电脑](computer.md)能跑起来，你首先要正确配置它。我们有多个档次的电脑，但让我们从基础的标准档开始做起。

**免责声明**：这是手把手的教程，还会告诉你后面出现问题要怎么处理，因此这教程比较长。如果现实中你没有装机经历，并且你是第一次接触这 Mod，那么请务必耐心读完。

首先你需要一个[机箱](../block/case1.md)。你所有的电脑配件都要装这里面，它将决定你电脑的行为。

![一个 T2 机箱](oredict:opencomputers:case2)

比如你要挑一个适合你的[显卡](../item/graphicsCard1.md)，还可能需要一个[网卡](../item/lanCard.md)、一块[红石卡](../item/redstoneCard1.md)、甚至是创造模式下调试时需要的[调试卡](../item/debugCard.md)。

打开[机箱](../block/case1.md)的 GUI 后你会看到右边有一系列槽位。槽位数以及可支持的配件等级（槽位小字有写）等取决于机箱档次。  
![T2 机箱的 GUI](opencomputers:doc/img/configuration_case1.png)  
空[机箱](../block/case1.md)基本没什么用。如果你在这时试图启动[电脑](computer.md)，它只会立刻在你的聊天框输出一条错误，然后用滴滴声告诉你它现在很不满。幸运的是他提醒了你正确的修复方式：电脑需要能源。只需要给你的电脑接通电源或是[能量转换器](../block/powerConverter.md)就可以了。

这个时候再尝试启动，它会告诉你电脑需要安装 [CPU](../item/cpu1.md)。CPU 分不同级别，级别越高，能搭配使用的配件就越多，执行速度也越快。此后你会注意到 OpenComputer 中的很多东西都分级别。就现在来说，你现在应该选一块合适的 CPU 装进你的[机箱](../block/case1.md)里。

好的，然后它要你装[内存条](../item/ram1.md)了。你会注意到警报音变了：长-短。越高级的[内存](../item/ram1.md)容量越大，在上面跑的程序也就能越复杂。为达到这篇教程的目标——运行 [OpenOS](openOS.md)——需要至少 2 条 T1 [内存](../item/ram1.md)。

干得不错，现在你的[机箱](../block/case1.md)应该长这样：  
![这是台配置好了一半的电脑。](opencomputers:doc/img/configuration_case2.png)  
看，现在虽然不会打印错误提示了，但它还是什么都做不了。除了两声警报。这意味着电脑未能成功启动。但实际上，从技术角度来看：电脑能启动了！下面有请超级好用的工具：[调试器](../item/analyzer.md)。它可用来调试各种 OpenComputers 的方块及一部分其他 Mod 的方块。只需潜行时对着[电脑](computer.md)的机箱使用[调试器](../item/analyzer.md)就好。

你会在聊天框看到[电脑](computer.md)遇到的错误：  
`no bios found; install configured EEPROM`

注意那个“configured”。合成 [EEPROM](../item/eeprom.md) 很简单，但要配置 EEPROM 则需要[电脑](computer.md)。是不是难了点？"Lua BIOS" [EEPROM](../item/eeprom.md)，使用 EEPROM 和你的[手册](../item/manual.md)合成，然后丢进机箱，开机！

——还是啥都没有。但是我们有[调试器](../item/analyzer.md)，调试器告诉我们：  
`no bootable medium found; file not found`

这说明 BIOS 运行正常，但并没有任何可启动的东西，比如[软盘](../item/floppy.md)或[硬盘](../item/hdd1.md)什么的。对于 Lua BIOS 来说，它还要在文件系统的根目录中寻找名为 `init.lua` 的文件。你或许已经猜到了：要合成系统安装盘了。用空[软盘](../item/floppy.md)和[手册](../item/manual.md)在工作台中合成即可得到 [OpenOS](openOS.md) 安装盘。

如果你用了 [T2 机箱](../block/case2.md)，那么你还要做一个[软盘驱动器](../block/diskDrive.md)来放软盘，软驱可以直接连在机箱一侧，或是通过[线缆](../block/cable.md)相连。如果是 T3 或更高级的机箱，那么直接放到机箱就行了。现在，插入软盘，启动。

好的，它启动了。如果还有什么问题的话，可以使用[分析仪](../item/analyzer.md)排查。不过我们的电脑应该跑起来了。最难的部分已经过去了，剩下就是如何让电脑输出信息，并且让电脑接受输入。

你需要给电脑配[屏幕](../block/screen1.md)和[显卡](../item/graphicsCard1.md)。
![不是平板屏幕哦](oredict:opencomputers:screen2)

[屏幕](../block/screen1.md)可以直接放在机箱一侧，或是通过[线缆](../block/cable.md)相连。[显卡](../item/graphicsCard1.md)自然是要装机箱里。现在你应该能看到[屏幕](../block/screen1.md)上闪烁的光标了。最后，[键盘](../block/keyboard.md)应安装在[屏幕](../block/screen1.md)上，或直接冲着[屏幕](../block/screen1.md)放置。

就是这样了。此时[电脑](computer.md)应已经启动，并等待你的操作。尝试一下吧！敲入 `lua` 并按下回车，你会得到 Lua 解释器的使用说明（英文的），在这里你可以测试基本 Lua 指令。更多信息见[这里](lua.md)。

![它活了！](opencomputers:doc/img/configuration_done.png)

之后你还可以建造更复杂的[电脑](computer.md)和[服务器](../item/server1.md)，用[组装机](../block/assembler.md)组装[机器人](../block/robot.md)、[无人机](../item/drone.md)、[单片机](../block/microcontroller.md)和[平板电脑](../item/tablet.md)。

最后：Happy coding!
