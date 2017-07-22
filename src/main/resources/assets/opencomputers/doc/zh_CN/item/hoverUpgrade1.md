# 悬浮升级

![Float like a feather.](oredict:oc:hoverUpgrade1)

允许 [机器人](../block/robot.md)飞得更高.缺省状态下机器人只能往上走8格子.平常因为这足够爬墙所以这足够了.
规律是这样的:
- 机器人只会在起点,终点都有效的情况下才会动(e.g.允许搭桥).
- 机器人下方的位置,永远有效(也就是说:任何时候都可以往下走).
- 一个固体方块上方的飞行上限内的格子,都是有效的
- 拥有正对当前位置,相邻的表面的点是有效的(允许爬墙).

这几个规则演示一下就是这样:

![Robot movement rules visualized.](opencomputers:doc/img/robotMovement.png)

如果不想被地球引力限制就安一个吧