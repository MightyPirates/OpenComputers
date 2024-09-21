# 远程终端

![远程访问。](oredict:oc:terminal)

远程终端可通过[终端服务器](terminalServer.md)对电脑进行远程控制。要使用远程终端，首先要激活一台装在[机架](../block/rack.md)里的[终端服务器](terminalServer.md)（手持远程终端，对准[机架](../block/rack.md)里的[终端服务器](terminalServer.md)点击即可绑定）。

[终端服务器](terminalServer.md)提供了可供远程终端控制的虚拟[显示屏](../block/screen1.md)和[键盘](../block/keyboard.md)。如果在[终端服务器](terminalServer.md)所在子网中还存在其他实体显示屏和/或键盘，那么会出现不可预料的行为，因此请尽量避免。在远程终端绑定过之后，手持并使用它即可打开它的GUI，模式与连接有[键盘](../block/keyboard.md)的[显示屏](../block/screen1.md)一样。

多个远程终端可以绑定到同一个[终端服务器](terminalServer.md)上，但它们的显示内容会保持相同，因为它们共享同一套虚拟[显示屏](../block/screen1.md)和[键盘](../block/keyboard.md)。绑定到[终端服务器](terminalServer.md)的终端数有限。当绑定数达到上限时，再绑定新终端会解绑最早绑定的终端。
