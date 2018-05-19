# Computers

电脑由几种不同的[方块](../block/index.md)和组件构成. 最最简单的电脑包括一个[机箱](../block/case1.md), 一个[显示器](../block/screen1.md), 以及一块[键盘](../block/keyboard.md). [机箱](../block/case1.md)和[显示器](../block/screen1.md)有多个等级, 每一级的功效都有所不同, 这样的分级也增大了计算机系统的可变性. 为了使[显示器](../block/screen1.md)能被打开, 那块[键盘](../block/keyboard.md)必须紧邻[显示器](../block/screen1.md)(不管是放在显示器前面还是直接贴在上面).

做完这几件事后, 你就可以往[机箱](../block/case1.md)里塞组件了. 这里说的“组件”包括[CPU](../item/cpu1.md), [内存(RAM)](../item/ram1.md), [硬盘(HDD)](../item/hdd1.md), [图形处理器/显卡](../item/graphicsCard1.md)(用来启用屏幕), [网卡](../item/lanCard.md)(负责网络通信), 等等等等. 这么一大堆组件使得你可以及其灵活地定制用于特定目的的系统.



Lower tier computers also require a [disk drive](../block/diskDrive.md), which takes a [floppy](../item/floppy.md) disk. An [OpenOS](openOS.md) [floppy](../item/floppy.md) disk is needed for booting up the computer for the first time, and is used to install the operating system to the [HDD](../item/hdd1.md). Once installed to the [HDD](../item/hdd1.md), the [floppy](../item/floppy.md) disk is no longer necessary. Additional software is also available as [floppy](../item/floppy.md) disks (such as Open Programs Package Manager, or OPPM) and are obtained from dungeon loot. 

The final step necessary is to provide the computer with a power source. OpenComputers is compatible with most major power-providing mods, and many blocks can be powered directly. You can see which blocks can be connected to external power by checking if their tooltip contains an entry about the block's power conversion speed.
For a larger network with multiple computers, a [power converter](../block/powerConverter.md) (converts different mod's power to OC's internal Energy type), [power distributor](../block/powerDistributor.md) (distributes power to different computers on the network), and [capacitor](../block/capacitor.md) (power storage for the network) can be used to connect different computers on the network using [cables](../block/cable.md).