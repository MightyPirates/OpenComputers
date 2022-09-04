# 纳米机器

![这是纳米机器，我的儿子。](oredict:opencomputers:nanomachines)

这些跟你的神经系统打交道的玩意能让你变得更快、更高、更强，或者干掉你。甚至有时候这些是同时发生的！简单来说，纳米机器的用途就是往宿主玩家上加 buff 和 debuff。“安装”方法：吃下去！

“安装”纳米机器后，你的屏幕上就会多出一个用于显示它们的剩余电量的 HUD。站在[充电机](../block/charger.md)附近就可以给他们充电。纳米机械越多，能耗越大。

纳米机器提供了一些可触发的“输入”，效果从玩家周身的粒子效果到药水效果选择，再到更多稀奇古怪的功能，应有尽有！

哪个输入触发哪个效果由当前纳米机器的配置决定。每次配置时，实际的连接情况是随机的。这意味着你要尝试启用不同的输入来验证他们做了什么。如果你对配置不满，你可以吃下一批新的机器重新配置。如果你要完全清掉纳米机器，那就来杯[酸液](acid.md)。注意一次启用过多输入会给你带来严重负面效果！

默认，纳米机器处于待机状态。你需要用无线消息来控制，在此强烈建议配备装有无线网卡的[平板](tablet.md)来完成这个任务。纳米机器只会对两格内设备发出的信号有反应，不限端口不限机器！

纳米机器使用一套简单的专有协议，每个封包必须包含数个部分：第一部分必须是 `nanomachines` 这个字符串，作为数据包“头部”，第二部分是命令名，其他部分是命令参数。纳米机械目前可接受以下命令，格式为 `commandName(arg1, ...)`：

- `setResponsePort(port:number)` - 设定纳米机器响应的端口，命令的响应在此接受。
- `getPowerState()` - 返回当前能量和最大值。
- `getHealth()` - 返回当前玩家的生命。
- `getHunger()` - 返回当前玩家的饥饿值。
- `getAge()` - 返回当前玩家的存活时间，单位为秒。
- `getName()` - 返回玩家的显示名字。
- `getExperience()` - 返回玩家的经验数量。
- `getTotalInputCount()` - 返回可用输入端口总数。
- `getSafeActiveInputs()` - 返回安全的输入端口总数。
- `getMaxActiveInputs()` - 返回活动输入数量最大值。
- `getInput(index:number)` - 得到对应索引值处的输入状态。
- `setInput(index:number, value:boolean)` - 设定对应索引处的输入状态。
- `getActiveEffects()` - 返回活动的纳米机器作用列表，注意部分效果是不显示的。
- `saveConfiguration()` - 返回物品栏里面的纳米机器，将会把当前的配置存到他们里面。

OpenOS 用例：
- `component.modem.broadcast(1, "nanomachines", "setInput", 1, true)` 将启用第一输入。
- `component.modem.broadcast(1, "nanomachines", "getHealth")` 将得到玩家当前的生命值。
