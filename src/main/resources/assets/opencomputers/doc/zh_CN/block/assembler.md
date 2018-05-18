# 组装机

![Harder, better, faster, stronger.](oredict:oc:assembler)

组装机用来制作更加复杂的物品, 如 [机器人](robot.md), [无人机](../item/drone.md) 和 [平板电脑](../item/tablet.md). 组装机需要大量能源来组装设备, 因此推荐使用[电容](capacitor.md)供电.

制作过程：首先, 将设备的基础零件放入组装机. 对于[机器人](robot.md), 你需要需要放入[机箱](case1.md); 对于[平板](../item/tablet.md), 则需要[平板外壳](../item/tabletCase1.md). 像大多数OC物品栏那样, 部件可以被放入特定的几个槽; 在物品/槽上悬停鼠标, 游戏会高亮能够放进去的槽/物品. 有NEI的话, NEI也能显示合适的物品. 继续添加你希望装进去的配件. 尤其注意别忘了装操作系统, 或者是准备好安装系统的方法(以机器人为例, 你可以装一个[软盘驱动器](diskDrive.md)以便以后插[软盘](../item/floppy.md)). 大多数设备的[EEPROM](../item/eeprom.md)在组装完成后可以更换, 只要把该设备和一张新的[EEPROM](../item/eeprom.md)合成, 设备就会装上它. 原来的[EEPROM](../item/eeprom.md)会退还.

注意[机器人](robot.md)也可以装[屏幕](screen1.md). 你可以安装一个[T1屏幕](screen1.md), 否则你的机器人就没有屏幕; 还要安装[键盘](keyboard.md), 除非你不想/需要打字. 对于 [平板](../item/tablet.md), [屏幕](screen1.md)预装在[平板外壳](../item/tabletCase1.md)里面了, 但是你还是要安一个 [键盘](keyboard.md).

一切就位后, 按下开始, 设备会被组装并和充能. 记住, 组装完成后设备就*无法*修改了, 除非你选择[拆解](disassembler.md)(注意拆解有5%的损耗率).

复杂度: 物件的等级决定了它要多少复杂度. T1物品消耗1复杂度, T2物品2, T3物品3. 插槽升级是个例外, 它消耗的复杂度是级数的两倍(比如说, 一个T2的[升级插槽](../item/upgradeContainer1.md)需要4点复杂度, [扩展卡插槽](../item/cardContainer1.md)同理). 如果复杂度超过了上限, 组装机将拒绝组装.