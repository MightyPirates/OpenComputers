# 活塞升级

![推他。](oredict:opencomputers:pistonUpgrade)

活塞升级能让某些设备的行为变得像原版活塞那样。安装后会暴露一个只有一个 `push()` 方法的组件。调用此方法时，设备将会试图将它面前的方块推出去。对于[机器人](../block/robot.md)和[单片机](../block/microcontroller.md)来说这就是它们的正面；对于[平板](tablet.md)来说，是玩家视角的那个方向。

推方块的逻辑和原版活塞一致。
