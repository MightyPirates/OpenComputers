# 装配器

![更硬，更好，更快，更强。](oredict:opencomputers:assembler)

装配器是个用来制作精密电子设备的高级工作台，像是[机器人](robot.md)、[无人机](../item/drone.md)和[平板](../item/tablet.md)这样的设备都需要在这里制作。组装电子设备的过程需要消耗大量能源，所以在此推荐使用[电容](capacitor.md)保证其能源供应。

在装配器中组装设备的第一步是它的“基板”。对于[机器人](robot.md)来说是任意[机箱](case1.md)，对于[平板](../item/tablet.md)则是[平板外壳](../item/tabletCase1.md)。和大多数 OpenComputer 设备的物品栏一样，在物品栏某个格子处悬停可以告诉你你这个格子可以装什么。有 NEI 的话，NEI也能显示合适的物品。现在只需要继续把你想要的元件装进去就好了。不过要特别注意，你需要给设备装个操作系统，或者留下一会再装操作系统的方式（比方说你可以给机器人装[磁盘驱动器](diskDrive.md)，这样它就能读写[软盘](../item/floppy.md)了）。对大多数设备来说，[EEPROM](../item/eeprom.md) 可以通过与新的 [EEPROM](../item/eeprom.md) 在工作台合成的方式替换，原有的 [EEPROM](../item/eeprom.md) 会返还。

注意[机器人](robot.md)也可以装[屏幕](screen1.md)，但只能装[基础屏幕](screen1.md)。要敲指令则需要装[键盘](keyboard.md)。对于[平板](../item/tablet.md)来说，[平板外壳](../item/tabletCase1.md)已经预装有[屏幕](screen1.md)了，但你还是要装[键盘](keyboard.md)才能敲指令。

一切就绪后，按下“组装”按钮，然后等它组装并完全充好电就可以了。记住，设备一旦组装完成就**无法**改装了，如果你忘装元件了，只能先[拆解](disassembler.md)再重来，但拆解的过程有可能会损坏元件。

最后，关于复杂度的一点提示：物品的级别决定了复杂度要求， 普通元件复杂度是 1，高级的是 2，依此类推。但升级组件容器是个例外，它们的复杂度是等级的二倍（比方说 T2 的[升级组件容器](../item/upgradeContainer1.md)复杂度是 4，对应的[卡槽](../item/cardContainer1.md)同理）。
