# 开放式电脑手册

开放式电脑（OpenComputers，下简称OC）是一个向Minecraft添加了可持续的、模块化的、可高度定制的[电脑](general/computer.md)、[服务器](item/server1.md)、[机器人](block/robot.md)和[无人机](item/drone.md)的Mod。所有设备均可通过Lua 5.2编程，用户可根据需求构建复杂度不同的电脑系统。

你可以通过[手册说明](item/manual.md)来学习这本手册的使用方法（绿色文本为链接，可点击）。

## 目录

### 设备
- [电脑](general/computer.md)
- [服务器](item/server1.md)
- [微控制器](block/microcontroller.md)
- [机器人](block/robot.md)
- [无人机](item/drone.md)

### 软件与编程
- [OpenOS](general/openOS.md)
- [Lua](general/lua.md)

### 方块与物品
- [物品](item/index.md)
- [方块](block/index.md)

### 教程
- [入门教程](general/quickstart.md)

## 概要

如上文所述，OC模组的电脑有可持续的特性，意思是运行中的[电脑](general/computer.md)在其所在区块停止加载后会保存运行状态。也就是说，当玩家离开[电脑](general/computer.md)所在区块，或退出游戏后，[电脑](general/computer.md)会记忆自己的最后运行状态，并在玩家再次靠近[电脑](general/computer.md)时从保存的那一刻继续运行。除[平板电脑](item/tablet.md)以外的所有设备均可持续。  

所有设备均是模块化的，可用多种组件来组装，正如现实中的[电脑](general/computer.md)一样。善于思考的玩家能够将设备调配成自己心中想要的样子。只要你想，你还可以把配置不满意的设备[分解](block/disassembler.md)并重新构建。对[电脑](general/computer.md)和[服务器](item/server1.md)而言可以打开GUI热插拔更换组件。

OC模组的设备兼容多种Mod，可操作方块与实体（通过[适配器](block/adapter.md)或[机器人](block/robot.md)和[无人机](item/drone.md)的特定升级实现）。设备可通过多个其他模组供能，包括但不限于红石通量（RF）、工业时代2的EU、通用机械的焦耳（Joules）、应用能源2的能量，以及因式分解/工厂化的Charge（CG）。

OC模组的设备还有其他特性，以及一些限制。[电脑](general/computer.md)是基准，能够安装数量中等的组件，具体数量随所用CPU等级不同而变化。[电脑](general/computer.md)还能访问周围全部六个面的组件。相比[电脑](general/computer.md)来说，[服务器](item/server1.md)通过使用[组件总线](item/componentBus1.md)获得了连接更多（内部或外部）组件的能力；但是因为[机架](block/rack.md)的限制，服务器只能访问[机架](block/rack.md)某一面连接的组件，具体哪一面可在[机架](block/rack.md)的GUI中设定。[微控制器](block/microcontroller.md)所受限制更多（相较于[电脑](general/computer.md)而言），原因是缺少[硬盘](item/hdd1.md)和[软盘驱动器](block/diskDrive.md)槽位，意味着[微控制器](block/microcontroller.md)上无法安装[OpenOS](general/openOS.md)。但[微控制器](block/microcontroller.md)有一个[EEPROM](item/eeprom.md)槽位，可在其中编写更专注于实现功能的系统，处理种类有限的任务。

[机器人](block/robot.md)是可移动的[电脑](general/computer.md)，可以与世界交互（但无法和外部的OC模组方块交互）。与[电脑](general/computer.md)不同，[机器人](block/robot.md)组装完成后内部的组件无法拆除。要规避此限制，你可以在[机器人](block/robot.md)中安装[升级容器](item/upgradeContainer1.md)或[扩展卡容器](item/cardContainer1.md)，这两者能够让你在需要时热插拔更换扩展卡或组件。[机器人](block/robot.md)上面可以安装[OpenOS](general/openOS.md)，一种方式为在容器（升级或扩展卡容器）槽位中放入[软盘驱动器](block/diskDrive.md)，这样即可向机器人中插入[软盘](item/floppy.md)）。另一种方式为组装时在任意[硬盘](item/hdd1.md)槽位中插入预装了[OpenOS](general/openOS.md)的[硬盘](item/hdd1.md)。要完全修改一台机器人的配置，只能先将机器人[拆解](block/disassembler.md)。[无人机](item/drone.md)是[机器人](block/robot.md)的更受限版本，它们的移动方式不同，物品栏槽位更少，也没有操作系统（类似[微控制器](block/microcontroller.md)，[无人机](item/drone.md)也能通过写有程序的[EEPROM](item/eeprom.md)进行配置，以完成种类有限的任务）。大部分情况下，[机器人](block/robot.md)和[无人机](item/drone.md)的升级和组件通用；但是某些升级在[无人机](item/drone.md)中的工作效果不一样。比如每个[物品栏升级](item/inventoryUpgrade.md)只能给无人机提供4个槽位，总共最多提升8个槽位。而[机器人](block/robot.md)能安装更多[物品栏升级](item/inventoryUpgrade.md)（总共可安装4个），每个升级能提供的槽位也更多（每个升级提供16槽位）。

本手册包含了关于模组中所有方块与物品的详细信息、如何搭建各种系统与设备，以及Lua编程的简单介绍。
