# 电容

![It's over 9000.](oredict:oc:capacitor)

存储了网络需要的能源, 作为能源缓存.不像那种把其他能源转换过来的mod ([能源转换器](powerConverter.md)),

这个能源转化是即时的. 内部缓存对一些有大量需求的任务比较有用, 比如[组装](assembler.md) 或者 [充能](charger.md) 设备 如[机器人](robot.md) ， [无人机](../item/drone.md). 

存储效率和附近的电容总数有关. 比如两个相邻的电容总是比分开的两个存的多. 

相邻的判定是2个方块距离, 然后效率随着距离递减.

可以链接[能源分发器](powerDistributor.md)为网络上的设备供能。