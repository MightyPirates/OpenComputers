# Remote Terminal

![Remote access.](oredict:oc:terminal)

终端提供了对[终端服务器](terminalServer.md)的远程访问. 激活一台在机架安装的[终端服务器](terminalServer.md), 选择要绑定到终端的服务器即可.

[终端服务器](terminalServer.md)提供了[虚拟屏幕](../block/screen1.md),[虚拟键盘](../block/keyboard.md)(类似于openssh server),可以通过终端访问.注意:切勿将实体键盘和屏幕连到终端服务器所在的子网,否则发生的事情是未定义行为. 使用绑定了的终端后，一个和电脑连接屏幕和键盘一样的界面会打开，你在这个GUI做的事情将会被反馈到远程的机器

多个终端可以被绑定到同一个[终端服务器](terminalServer.md), 但是他们共享显示和输入，这是和sshd不同的地方. [终端服务器](terminalServer.md) 只能绑定有限数量的终端. 达到上限后，继续绑定会踢出最先绑定的终端.
	