# 调试卡

![等等，如果…… 哦。](item:opencomputers:debugcard)

调试卡本身是一种仅用于在创造模式下对设备进行快速调试的卡片。有鉴于其丰富的功能，它对于地图制作也很有用处。

你可以潜行时使用这张卡来将其与你绑定或解绑，绑定后 `runCommand` 将会使用你的权限等级，而非默认的 OpenComputers 使用的权限。

调试卡也可以像[连接卡](linkedCard.md)一样接受消息，此时它会触发 `debug_message` 事件。这样的消息可以通过其他调试卡上的 `sendDebugMessage` 方法发送，或者使用 Minecraft 命令 `/oc_sendDebugMessage`（或 `/oc_sdbg`）发送。
