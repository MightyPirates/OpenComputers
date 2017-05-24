# OpenComputers Manual

OpenComputers 是一个持久化，模块化，高度可定制化的mod，在游戏中提供了 [电脑](general/computer.md), [服务器](item/server1.md), [机器人](block/robot.md), 和 [无人机](item/drone.md) .所有设备均可通过LUA5.2编写程序, 实现各种复杂的系统. 

通过 [关于手册](item/manual.md)学习mod (绿字是链接，可点击).

## 内容

### 设备
- [电脑](general/computer.md)
- [服务器](item/server1.md)
- [单片机](block/microcontroller.md)
- [机器人](block/robot.md)
- [无人机](item/drone.md)

### 软件和程序编写
- [OpenOS](general/openOS.md)
- [Lua](general/lua.md)

### 方块和物品
- [物品](item/index.md)
- [方块](block/index.md)

### Guides
- [Getting Started](general/quickstart.md)

## Overview

如上文所说, 电脑可以持久化的存储自己的状态, 意味着运行的 [电脑](general/computer.md) 即使在所属区块被卸载时也能保持住自己的状态. 也就是说当玩家离开 [电脑](general/computer.md)所在的区块, 或者退出后,  [电脑](general/computer.md) 将会记住离开前最后一刻的状态， 并在区块被加载后重新从断电恢复运转[电脑](general/computer.md). 特性 不包括[平板](item/tablet.md).  

所有的设备高度模块化，可以用多种材料制作, 正如现实的 [电脑](general/computer.md) . 善于发现的玩家能够不断将他的设备打造成理想的样子. 只要想，设备甚至在设计不满意的时候可以被 [拆卸](block/disassembler.md) 并重组. 对于 [电脑](general/computer.md) 和 [服务器](item/server1.md), 设备可以被通过GUI热插拔。 

OpenComputers 与多个mod兼容，不论是方块还是实体。(通过 [适配器](block/adapter.md), 或者对 [机器人](block/robot.md) 和 [无人机](item/drone.md)进行适当的升级). 多个mod均可以用来为这些电脑供能, 包括但不限于以下： RF,EU, MJ,AE2能源 甚至是因式分解mod的能量. 

虽说有些限制，OC mod依然提供了大量的可能性. [电脑](general/computer.md) 是基础, 能够做到大多数事情, CPU是核心. [电脑](general/computer.md) 可以访问周围6个面的组件. [服务器](item/server1.md) 可以通过 [总线](item/componentBus1.md)链接更多设备 (内部或外部互联), ; 但是服务器只能被安装在[机架](block/rack.md), 他只能被从[机架](block/rack.md)的一侧被访问到, 访问的侧可以被从 [机架](block/rack.md) GUI设定. [单片机](block/microcontroller.md) 功能十分受限 (比起普通的[电脑](general/computer.md))， 因为他们只有可怜的存储[硬盘](item/hdd1.md) 和外置存储 [软盘驱动器](block/diskDrive.md) , 意味着通常的 [OpenOS](general/openOS.md) 无法被安装到 [单片机](block/microcontroller.md). [单片机](block/microcontroller.md)只有一个 [E2PROM](item/eeprom.md)槽, 只能被编程为执行有限的简单任务. 

[机器人](block/robot.md) 是移动的 [电脑](general/computer.md), 可以与世界交互 (但无法和外部电脑方块交互).不像 [电脑](general/computer.md), [机器人](block/robot.md) 一经建造，内部的部件就不能再被改变或者去除. 为了解决这个问题, [机器人](block/robot.md) 可以被 [升级](item/upgradeContainer1.md) 或者 [卡片](item/cardContainer1.md) 插入, 允许热升级或者插拔卡片. [OpenOS](general/openOS.md) 可以在 [机器人](block/robot.md) 上面通过放一个[硬盘](block/diskDrive.md) 安装, 也允许插入 [软盘](item/floppy.md) disks, 或者插入预装了OpenOS的硬盘. 重设机器人需要将机器人[拆解](block/disassembler.md) . [无人机](item/drone.md)是阉割版机器人 [robots](block/robot.md). 他们只有少量物品栏，移动方式也和机器人不同, 像[单片机](block/microcontroller.md)一样安不上操作系统, [无人机](item/drone.md) 可以被插入预编程的 [E2PROM](item/eeprom.md)). 大部分情况下, [机器人](block/robot.md) 和 [无人机](item/drone.md) 使用相同的配件和更新; 然而, 这些东西在无人机和机器人的表现不统一,  [物品栏升级](item/inventoryUpgrade.md) 每次只提供四个物品栏, 最多8个,  [机器人](block/robot.md) 可以携带最多四个，每个提供16个物品的 [物品栏升级](item/inventoryUpgrade.md)

##机器人

制作机器人请将机箱放到组装器里面，这样就可以做机器人了

可以装入各种升级和卡片，配备软驱的机器人还可以安装OS

机器人的汉化卡死，所以就简单的说下好了

E文好的可以去看E文手册
