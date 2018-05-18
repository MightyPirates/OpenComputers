# OpenComputers使用手册

OpenComputers这个mod向游戏加入了连续性的(persistent只能这么翻译了QAQ), 模块化的, 高度可定制化的[电脑](general/computer.md), [服务器](item/server1.md), [机器人](block/robot.md), 和[无人机](item/drone.md). 所有设备均可通过LUA 5.3编写程序, 以便根据用途改变系统的复杂程度. 

通过[关于手册](item/manual.md)了解如何使用本手册(绿字是链接, 可点击).

## 内容

### 设备
- [电脑](general/computer.md)
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

### 指导
- [快速上手](general/quickstart.md)

## 概要

如上文所说, 电脑的工作是连续性的, 运行中的[电脑](general/computer.md)即使在所属区块被卸载也能保持自己的状态. 也就是说当玩家离开 [电脑](general/computer.md)所在的区块, 或者退出后, [电脑](general/computer.md) 将会记住离开前的状态，并在区块被加载后从断点恢复运转. 这项特性特性对[平板电脑](item/tablet.md)不适用.  

所有的设备都是模块化的，可以用多种组件组装, 正如现实的[电脑](general/computer.md)一般. 喜欢动手的玩家能够不断将他的设备打造成理想的样子. 只要你想，设备甚至可以在不满意的时候被[拆卸](block/disassembler.md)和重组. [电脑](general/computer.md)和[服务器](item/server1.md)的组件可以通过GUI热插拔。 

OpenComputers的设备与许多mod兼容. 通过[适配器](block/adapter.md), 或是[机器人](block/robot.md)和[无人机](item/drone.md)的某些升级组件, OC可以操控很多方块和实体. OC支持一大堆能量单位, 包括但不限于： RF, EU, MJ, AE2能源, 甚至是因式分解mod的电荷. 

OC的设备各有所长, 但也各有一些限制. [电脑](general/computer.md)是最基本的设备, 也是许多对比的基准. 它能够做到大多数事情, 也可以访问周围6个面的组件. [服务器](item/server1.md) 可以通过 [组件总线](item/componentBus1.md)连接更多的设备 (不论是内部的连接还是外部的), 但是服务器只能被安装在[机架](block/rack.md), 且只能被从[机架](block/rack.md)的一侧被访问到. 从哪一侧访问可以在[机架](block/rack.md)的GUI里设置. [单片机](block/microcontroller.md)功能十分有限, 因为他们不能装[硬盘](item/hdd1.md)和也不能用外置存储([软盘驱动器](block/diskDrive.md)), 这意味着它们无法安装[OpenOS](general/openOS.md). 但[单片机](block/microcontroller.md)有一个装[EEPROM](item/eeprom.md)的槽, 因此可以用一种极其精简的“操作系统”完成有限的工作. 

[机器人](block/robot.md)是可移动的 [电脑](general/computer.md), 可以与世界交互 (但无法和外部OC方块交互). 不像 [电脑](general/computer.md), [机器人](block/robot.md) 一经建造，内部的部件就不能再被改变或者移除. 为了解决这个问题, [机器人](block/robot.md)可以预装[升级插槽](item/upgradeContainer1.md)或者[扩展卡插槽](item/cardContainer1.md), 以允许热插拔. [OpenOS](general/openOS.md)可以通过装一个[硬盘](block/diskDrive.md)在[机器人](block/robot.md)上安装, 也允许插入 [软盘](item/floppy.md) disks, 或者插入预装了OpenOS的硬盘. 重设机器人需要将机器人[拆解](block/disassembler.md) . [无人机](item/drone.md)是阉割版机器人 [robots](block/robot.md). 他们只有少量物品栏，移动方式也和机器人不同, 像[单片机](block/microcontroller.md)一样安不上操作系统, [无人机](item/drone.md) 可以被插入预编程的 [E2PROM](item/eeprom.md)). 大部分情况下, [机器人](block/robot.md) 和 [无人机](item/drone.md) 使用相同的配件和更新; 然而, 这些东西在无人机和机器人的表现不统一,  [物品栏升级](item/inventoryUpgrade.md) 每次只提供四个物品栏, 最多8个,  [机器人](block/robot.md) 可以携带最多四个，每个提供16个物品的 [物品栏升级](item/inventoryUpgrade.md)

[Robots](block/robot.md) are moving [computers](general/computer.md), and are able to interact with the world (but cannot interact with external OpenComputers blocks). Unlike [computers](general/computer.md), once a robot is built, the components inside the [robot](block/robot.md) cannot be removed. To circumvent this limitation, [robots](block/robot.md) may be built with [upgrade](item/upgradeContainer1.md) or [card](item/cardContainer1.md) containers, allowing for on-the-fly swapping of cards or upgrades, if needed. [OpenOS](general/openOS.md) can be installed on [robots](block/robot.md) by placing a [disk drive](block/diskDrive.md) in a container slot, which will allow insertion of [floppy](item/floppy.md) disks, or by placing a [hard drive](item/hdd1.md) with [OpenOS](general/openOS.md) pre-installed in one of the [hard drive](item/hdd1.md) slots. To fully reconfigure a [robot](block/robot.md), it will need to be [disassembled](block/disassembler.md) first. [Drones](item/drone.md) are limited versions of [robots](block/robot.md). They move differently, contain fewer inventory slots, and lack an operating system (similarly to [microcontrollers](block/microcontroller.md), [drones](item/drone.md) can be configured with a programmed [EEPROM](item/eeprom.md) for a limited set of tasks). For the most part, [robots](block/robot.md) and [drones](item/drone.md) share the same upgrades and components; however, upgrades behave differently in [drones](item/drone.md), such as [inventory upgrades](item/inventoryUpgrade.md) only providing 4 slots per upgrade, for a total of 8 slots, while [robots](block/robot.md) are capable of taking more [inventory upgrades](item/inventoryUpgrade.md) (total of 4) as well as getting more slots per upgrade (16 slots per upgrade).

This manual contains detailed information regarding all blocks and items, how to set up different types of systems and devices, as well as an introduction to Lua programming.
