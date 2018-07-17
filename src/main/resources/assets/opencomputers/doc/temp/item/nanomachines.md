# 纳米机械

![Nanomachines, son.](oredict:oc:nanomachines)

这些小机械可以让你变得更快更高更强，或者干掉你. 有时候这些是同时的! 纳米机械往他们寄生的玩家身上添加buff和debuff. 吃下他们就可以安装了!

安装以后你的屏幕上会显示出这些机械还有多少能源运作. 站在[充电机](../block/charger.md)附近就可以充电.装的机器越多，能耗越大.

纳米机器提供了特定数量可以被玩家触发的输入，效果从玩家周身的粒子效果到药水效果选择，以至于更多奇怪的功能。

触发那些输入取决于当前纳米机器的配置, 每次配置时，实际的连接情况是随机的. 这意味着你要尝试启用不同的输入来验证他们做了什么. 如果对你的配置不满, 你可以吃下一批新的机器重新配置. 如果你要完全清掉纳米机器，喝点 [酸液](acid.md). 注意一次启用过多输入会给你带来debuff！

缺省情况下纳米机器是待机状态. 你要用无线消息来控制, 因此很有必要带一个安了无线网卡的[平板](tablet.md). 纳米机器只会对两格子内机器发出的信号有反应, 不限端口不限机器!

纳米机器使用一套简单的协议: 每个封包必须包含数个部分, 第一部分称为"header" 并且必须是 `nanomachines`这个字符串. 第二部分是命令名. 其他部分是命令参数. 以下命令可用，格式为 `commandName(arg1, ...)`:

- `setResponsePort(port:number)` - 设定纳米机器响应的端口，命令的响应在此接受.
- `getPowerState()` - 返回当前能量和最大值.
- `getHealth()` - 返回当前玩家的生命.
- `getHunger()` - 返回当前玩家的饥饿值.
- `getAge()` - 返回当前玩家的存活时间（秒） +1s.
- `getName()` - 返回玩家的名字.
- `getExperience()` - 返回玩家的经验数量.
- `getTotalInputCount()` - 返回可用输入端口总数.
- `getSafeActiveInputs()` - 返回安全的输入端口总数.
- `getMaxActiveInputs()` - 返回活动输入数量最大值.
- `getInput(index:number)` - 得到对应索引值处的输入状态.
- `setInput(index:number, value:boolean)` - 设定对应索引处的输入状态.
- `getActiveEffects()` - 返回活动的纳米机器作用列表. 注意部分效果是不显示的.
- `saveConfiguration()` - 返回物品栏里面的纳米机器, 将会把当前的配置存到他们里面.

OpenOS用例:
- `component.modem.broadcast(1, "nanomachines", "setInput", 1, true)` 将启用第一输入.
- `component.modem.broadcast(1, "nanomachines", "getHealth")` 将得到玩家的血量.
