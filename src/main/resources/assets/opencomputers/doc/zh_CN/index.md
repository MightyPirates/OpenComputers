# OpenComputers Manual

OpenComputers 是一个在 Minecraft 中加入了专业的，模块化的以及高度可配置的[计算机](general/computer.md), [服务器](item/server1.md), [机器人](block/robot.md), 和[无人机](item/drone.md)的模组. 所有设备都可以使用 Lua 5.2 进行编程，并可以根据使用情况的不同组成不同复杂度的系统.

你可以点击[手册介绍页](item/manual.md) 获取更多信息(绿色的文本是链接,你可以点击它).

## 目录

### 设备
- [计算机](general/computer.md)
- [服务器](item/server1.md)
- [单片机](block/microcontroller.md)
- [机器人](block/robot.md)
- [无人机](item/drone.md)

### 软件与编程
- [OpenOS](general/openOS.md)
- [Lua](general/lua.md)

### 方块与物品
- [物品](item/index.md)
- [方块](block/index.md)

### 指南
- [如何开始](general/quickstart.md)

## 概览

正如刚刚所说的, 在 OpenComputers 里,计算机是不易失的,  这意味着运行着的 [计算机](general/computer.md) 即使所在区块并没有加载时它依旧能够保持状态. 换句话来说这意味当玩家远离[计算机](general/computer.md)或者离开游戏时, [计算机](general/computer.md)会记住你最后一次离开时的状态,并且当你重新进入游戏或者接近[计算机](general/computer.md)时它会从你离开的时状态继续运行.持久性对于所有的设备都是有效的除了[平板电脑](item/tablet.md).  

所有的设备都是模块化的并且可以由许多的组件构成, 就如同现实世界中的[计算机](general/computer.md) 一样. 对于那些喜欢捣鼓的玩家，他们将有机会优化设备到他们心满意足为止. 如果你需要的话,例如初始配置不是那么令人满意,设备可以被[分解器](block/disassembler.md)分解并重新组装. 对于[计算机](general/computer.md)和[服务器](item/server1.md)来说, 通过打开相应的图形界面,组件可以即时的简单的替换. 

OpenComputers 的设备可以和许多不同的模组兼容，用于控制方块和实体(通过[适配器](block/adapter.md),或者[机器人](block/robot.md)和[无人机](item/drone.md)的特殊升级). OpenComputers 可以使用大量其他模组提供的能源,包括但不限于:Redstone(Flux),IndustrialCraft2(EU),Mekanism(Joules),Applied Energistics 2(energy)以及Factorization(Charge). 

OpenComputers 中的设备具有一些额外的功能和限制. [计算机](general/computer.md)是一切的基础, 它能够使用一定数量的组件,并通过CPU来控制它们. [计算机](general/computer.md) 也可以通过它相邻的六个面与其他的组件相连接. [服务器](item/server1.md)通过使用[组件总线](item/componentBus1.md)可以连接比[计算机](general/computer.md)更多的组件(内部和外部); 但是，因为[机架](block/rack.md)的限制 , [服务器](item/server1.md)只能够通过[机架](block/rack.md)的一个面与其他组件相连接, 这可以在[机架](block/rack.md)的图形界面中进行配置. [单片机](block/microcontroller.md)是一种更加受限的设备(相对于[计算机](general/computer.md))而言,它们缺乏[硬盘](item/hdd1.md)插槽和[软盘驱动器](block/diskDrive.md),这意味着我们不能够在[单片机](block/microcontroller.md)中安装[OpenOS](general/openOS.md).但[单片机](block/microcontroller.md)有可以插入[EEPROM](item/eeprom.md)的插槽, [EEPROM](item/eeprom.md)可以针对一组有限的任务进行操作系统级别的编程. 

[机器人](block/robot.md)是可以移动的[计算机](general/computer.md), 并且它们能够与世界进行互动(但不能与外部的 OpenComputers 方块进行交互). 不像[计算机](general/computer.md),一旦机器人组装完毕,在[机器人](block/robot.md)内部的组件就无法被移除.为了突破这个限制,在组装时[机器人](block/robot.md)可被设置为为允许 [升级](item/upgradeContainer1.md)或者添加[卡槽](item/cardContainer1.md),如果你需要的话,机器人可以即时的更换或者增加功能卡,用以升级机器人.你可以通过将[软盘驱动器](block/diskDrive.md)放到扩展槽中使得[机器人](block/robot.md)可以通过放置[软盘](item/floppy.md)的方式来安装使用[OpenOS](general/openOS.md),或在机器人组装时放入预安装[OpenOS](general/openOS.md)的硬盘来使用[OpenOS](general/openOS.md).  为了完整的改装一个[机器人](block/robot.md),我们首先需要[分解器](block/disassembler.md). [无人机](item/drone.md)是[机器人](block/robot.md)的受限版本.它们以完全不同的方式移动, 拥有的扩展槽也更少并且没有操作系统(就像[单片机](block/microcontroller.md)一样, [无人机](item/drone.md)可以用经过编程的[EEPROM](item/eeprom.md)进行配置以完成一组有限的任务).大部分情况下, [机器人](block/robot.md)和[无人机](item/drone.md)共享同样的升级插件与组件; 但是,升级插件的效果在[无人机](item/drone.md)中变得非常不同,例如[物品栏升级](item/inventoryUpgrade.md)在无人机中每次升级提供4个物品槽, 最多提供8个物品槽,但与此同时 [机器人](block/robot.md)能安装更多的[物品栏升级](item/inventoryUpgrade.md) (总共4个) 同时每次升级也会增加更多的物品槽(每次16个).

这本手册包含了关于模组中所有方块与物品的详细信息,以及搭建不同类型的设备与系统的方法, 同时还有一个关于 Lua 编程的简介.