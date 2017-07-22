# Lua 

[LUA编程手册](http://www.lua.org/manual/5.2/manual.html) 和 [LUA编程第一版](http://www.lua.org/pil/) 是学习基本功能并熟悉lua标准库的好帮手. [OpenOS](openOS.md) 尽可能准确的模拟标准库,当然有些许变化，比如某些高权限的debug库去掉了. 这些不同点写在了[这里](http://ocdoc.cil.li/api:non-standard-lua-libs).

require命令用来在脚本里引用模块
比如
`local component = require("component")` //引入组件API相关，所有组件api的函数可以调用了
`local rs = component.redstone` //引入红石相关，红石API可以用了
这将允许你使用[红石卡](../item/redstoneCard1.md)相关的组件功能，如:
`rs.setOutput(require("sides").front, 15)` //将前方的红石信号强度设为15
所有标准库不需要用require引用

重要：在Lua解释器的环境下请不要用local修饰符（脚本里面没有这个限制），这会在命令完成后将这个变量回收掉，你会发现你得不到任何变量，这是由于mod环境资源有限，不可能随时将库载入内存。

OpenOS提供了大量第三方库，从控制电脑连接的组件到机器人和无人机，有些库只有在相关卡安装后才能用，也就是说这些组件相当于包含了一个小型的只读文件系统