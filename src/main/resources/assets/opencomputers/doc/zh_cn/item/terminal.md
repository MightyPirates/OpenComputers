# 终端

![远程访问。](oredict:opencomputers:terminal)

终端可用于远程访问[终端服务器](terminalServer.md)。只需对准[机架](../block/rack.md)里安装的[终端服务器](terminalServer.md)手持远程终端右击即可完成绑定。

[终端服务器](terminalServer.md)提供了可供终端控制的虚拟[屏幕](../block/screen1.md)和[键盘](../block/keyboard.md)。注意，切勿将实体键盘和屏幕连到[终端服务器](terminalServer.md)所在的子网，否则后果难以预料。打开与服务器绑定的终端后，你会看到一个和打开连着[屏幕](../block/screen1.md)的[键盘](../block/keyboard.md)后看到的一样的界面，在此你可以操作服务器。

多个终端可以绑定到同一个[终端服务器](terminalServer.md)上，但是他们共享显示和输入。[终端服务器](terminalServer.md)只能绑定有限数量的终端，达到上限后，继续绑定会自动与最早绑定的终端解绑。
