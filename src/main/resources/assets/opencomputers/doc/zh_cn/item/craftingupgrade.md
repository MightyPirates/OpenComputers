# 合成升级

![心灵手巧，诡计多端。](oredict:oc:craftingUpgrade)

合成升级能让[机器人](../block/robot.md)用其自身[物品栏](../item/inventoryUpgrade.md)中的物品执行有序或无序合成配方。合成时，[机器人](../block/robot.md)[物品栏](../item/inventoryUpgrade.md)左上角的3X3区域将会被用作合成区。所用物品应当按照合成配方进行摆放。合成产物将会被放回[机器人](../block/robot.md)的[物品栏](../item/inventoryUpgrade.md)中。与机器人捡起掉落物时的逻辑相同，产物会被放置到选定槽位中，若失败则会放置到下一个可用空槽位中。若物品栏没有可用空间，产物会被丢到世界中。
