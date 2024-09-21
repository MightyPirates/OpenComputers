# 入门

此教程又名《如何组装你的第一台电脑》。为了让你的[电脑](computer.md)开始运行，你首先需要将它正确搭建起来。OC模组中有很多种电脑，但我们从基础的开始：标准电脑。

**声明**：这是手把手的教程，还会告诉你后续如何查找问题，因此教程比较长。如果现实中你没有装机经历，并且/或者你是第一次接触此模组，那么推荐你通读全文。

首先，你需要一个[电脑机箱](../block/case1.md)。这个方块会容纳所有组件，也决定了你所搭建电脑的行为。

![一个T2机箱](oredict:oc:case2)

举例而言，你还需要决定使用什么等级的[显卡](../item/graphicsCard1.md)，是否要安装[网卡](../item/lanCard.md)、[红石卡](../item/redstoneCard1.md)，或者你只是想在创造模式中随便游玩的话，可能还要装一张[调试卡](../item/debugCard.md)。

打开[机箱](../block/case1.md)的GUI后你会看到右边若干槽位。槽位数以及支持的组件等级（在槽位中用小号罗马数字标出）取决于机箱等级。  
![T2机箱的GUI](opencomputers:doc/img/configuration_case1.png)  
空[机箱](../block/case1.md)基本没什么用。你可以尝试现在启动[电脑](computer.md)，但它会立刻向你的聊天框输出一条报错信息，然后用滴声表达它的不满。好消息是报错信息告诉了你修复方式：电脑需要能量。只需要给你的电脑接通电源，无论直接连接或是通过[能量转换器](../block/powerConverter.md)连接均可。

这个时候再尝试启动，它会告诉你电脑需要安装[CPU](../item/cpu1.md)。CPU分不同等级，你会注意到等级这一概念在OC模组中普遍存在。对[CPU](../item/cpu1.md)而言，等级越高，同时连接的组件就越多，执行速度也越快。所以请挑一个等级，然后把它装进你的[电脑机箱](../block/case1.md)里。

接下来会要求你安装一些[内存条（RAM）](../item/ram1.md)。你会发现报警音发生了变化：变成了长-短。[内存条（RAM）](../item/ram1.md)等级越高，能提供给你[computer](computer.md)上所运行程序的内存就越大。要运行[OpenOS](openOS.md)，即本教程的目标，你需要至少两根1级[内存条（RAM）](../item/ram1.md)。

我们已经取得很大进展了。现在你的[电脑机箱](../block/case1.md)看上去应该类似这样：  
![部分完工的电脑。](opencomputers:doc/img/configuration_case2.png)  
看啊，现在将它打开已经不会输出报错信息了！但是，哎呀，它还是干不了什么事情。至少它现在会滴两次了。这代表[电脑](computer.md)的实际运行失败了。换句话说：理论上它已经运行了！此时一个非常实用的工具该上场表演了：[分析器](../item/analyzer.md)。它可用来检查OC模组的很多方块，也支持其他模组的一些方块。要对[电脑](computer.md)进行使用，只需手持[分析器](../item/analyzer.md)潜行与机箱交互。

你会看到导致[电脑](computer.md)发生崩溃的错误：
`no bios found; install configured EEPROM`
（未找到BIOS；请安装配置过的EEPROM）

注意其中的**configured（配置过的）**。合成 [EEPROM](../item/eeprom.md)很简单，但配置它通常需要[电脑](computer.md)，这就现在而言略有困难，因此我们要用合成配方来合成一个配置过的"Lua BIOS" [EEPROM](../item/eeprom.md)。基础配方是一个[EEPROM](../item/eeprom.md)加上一本[手册](../item/manual.md)。把配置过的[EEPROM](../item/eeprom.md)放进你的[电脑](computer.md)，然后——

不行。还是啥也没有。但我们知道要干什么：玩家可以使用[调试器](../item/analyzer.md)，太高效了！现在我们得到了一条不一样的报错信息：  
`no bootable medium found; file not found`
（未找到引导媒介；未找到文件）

好吧，这说明BIOS开始工作了。但它没找到用来启动的文件系统，例如[软盘](../item/floppy.md)或[硬盘](../item/hdd1.md)。具体而言，Lua BIOS需要一个这样的文件系统，其根目录还需有一个名为`init.lua`的文件。你可能猜到了：我们现在需要合成操作系统软盘。取一张空[软盘](../item/floppy.md)和一本[手册](../item/manual.md)，将它们合成在一起，然后你就得到了[OpenOS](openOS.md)软盘。

现在的话，如果你用的是和上面截图一样的T2[电脑机箱](../block/case2.md)，那么你还没有放入软盘的地方。如果你有T3或创造模式[电脑机箱](../block/case2.md)，那么你可以直接将软盘放进[机箱](../block/case2.md)里。不然你需要在机箱旁边放置一个[软盘驱动器](../block/diskDrive.md)（或通过[线缆](../block/cable.md)连接）。放入软盘后，你知道该干什么。按下电源键吧。

它活了！或者说应该是。如果它没能启动的话，代表有什么东西出错了，你可以用[分析器](../item/analyzer.md)排查。不过我们先假设它现在已经开始运行了，你已经接近完工了，最困难的部分已经结束了。剩下要做的就是让它接收输入并显示输出。

要让[电脑](computer.md)显示输出，你需要取一块[显示屏](../block/screen1.md)和一张[显卡](../item/graphicsCard1.md)。
![这不是纯平显示器。](oredict:oc:screen2)

请将[显示屏](../block/screen1.md)放置于直接相邻机箱的位置，或者再次通过[线缆](../block/cable.md)连接。然后将你选好的[显卡](../item/graphicsCard1.md)装进[电脑机箱](../block/case2.md)里。你应该会在[显示屏](../block/screen1.md)上看到闪烁的光标。最后，将[键盘](../block/keyboard.md)放置在[显示屏](../block/screen1.md)身上，或将其面对[显示屏](../block/screen1.md)放置，以启用[键盘](../block/keyboard.md)输入。

做完这一步，你就完工了。此时[电脑](computer.md)已经启动，正在运行且准备好执行操作了。现在试试用电脑吧！在shell中输入`lua`并按回车键，然后你会看到欢迎界面，其中有一些关于如何使用Lua解释器的信息。在这里你可以尝试基础的Lua命令。有关这方面的更多信息见[Lua页面](lua.md)。

![它活了！](opencomputers:doc/img/configuration_done.png)

请享受搭建更复杂的[电脑](computer.md)，折腾[服务器](../item/server1.md)以及用[电子装配机](../block/assembler.md)组装[机器人](../block/robot.md)、[无人机](../item/drone.md)、[微控制器](../block/microcontroller.md)和[平板电脑](../item/tablet.md)。

祝编程愉快！
