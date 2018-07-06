# Lua

[《Lua 编程手册》](https://www.lua.org/manual/5.2/manual.html)和[《Lua 编程》](https://www.lua.org/pil/) 是学习基本功能并熟悉 Lua 标准库的好帮手（译注：两本书原文均为英文）。虽然 [OpenOS](openOS.md) 尽可能准确地模拟标准库，但它相比于标准的 Lua 仍然有所不同，比如大部分 debug 库的内容出于安全考虑去掉了。所有不同之处都可以在[这里](https://ocdoc.cil.li/api:non-standard-lua-libs)找到。

非标准库的内容需要使用 `require` 方能引用。比如：

`local component = require("component")`  
`local rs = component.redstone`

这样你就能使用[红石卡](../item/redstoneCard1.md)相关的组件功能了。就像这样：

`rs.setOutput(require("sides").front, 15)`

**重要**：在 Lua 解释器的环境下请**不要**用 `local` 修饰符，因为这样会让变量的有效范围只有那一行代码。换句话说，把上面三行代码依次复制进去执行时，第三行代码会报错，提示你 `rs` 变量的值是 `nil`。你也许会问：为什么是第三行而非第二行？因为解释器试图从未知变量中加载库。虽然第一行的赋值操作不会立刻有效果，但第二行对 `component` 的引用会加载对应的库。毕竟，内存是有限的，所以 Lua 不会自动就把库加载好。

OpenOS 提供了大量第三方库，从控制与[电脑](computer.md)连接的组件，到控制捆绑红石线所用的颜色及[键盘](../block/keyboard.md)键位，不一而足。使用第三方库需要像前文中提到的那样用 `require` 导入。有些库只有在安装对应的组件后才能使用，比如 `internet` 就需要一块[网卡](../item/internetCard.md)。对于网卡这样的例子来说，实际上这个库只有在安装了对应组件后才能使用，从技术角度来说这些组件包含了一个小型的只读文件系统。
