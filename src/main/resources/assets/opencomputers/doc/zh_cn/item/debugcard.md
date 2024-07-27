# 调试卡

![等等，如果我…… 啊——](item:OpenComputers:item@73)

调试卡是仅限创造模式的物品，最初目的仅是为了通过自动化进行某些处理来为调试提供便利。此后它有了更多的功能，让它能在地图制作领域发挥很大作用。

你可以手持此卡并潜行使用，来将它与你绑定或解绑，绑定后`runCommand`函数会使用你的权限等级，不使用OC模组默认的权限等级。

调试卡也可以像[连接卡](linkedCard.md)一样接收报文，接收后会触发`debug_message`事件。你可以通过调用其他调式卡的`sendDebugMessage`方法，或者执行Minecraft命令`/oc_sendDebugMessage`（或`/oc_sdbg`）发送此类报文。
