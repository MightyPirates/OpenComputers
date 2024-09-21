# Lua

Lua[参考手册](https://www.lua.org/manual/5.2/manual.html)和[《Lua 编程》](https://www.lua.org/pil/)图书（第一版在网上免费）是入门Lua基础并熟悉基本语法和标准库的好选择。 [OpenOS](openOS.md)致力于尽量模拟标准库功能，但有少量差别，比如debug库基本缺失（因为沙盒环境的原因）。这些区别已在[官方wiki](https://ocdoc.cil.li/api:non-standard-lua-libs:zh)中列出。

非标准库需要先`require`才能在脚本中使用。例如：

`local component = require("component")`  
`local rs = component.redstone`

这样你就能调用[redstone](../item/redstoneCard1.md)组件提供的一切函数了。例如：

`rs.setOutput(require("sides").front, 15)`

**注意**：在Lua解释器中操作时，**不要**使用`local`，因为这样会让变量的作用域限定为输入的那一行代码。意味着如果你逐条向解释器输入上面的代码，第三条就会报错，告诉你`rs`变量的值为`nil`。你也许会问：为什么只有第三行呢？因为出于方便测试的目的，解释器会试图将未知变量当作库名，并将其加载。所以尽管第一行对`component`的赋值没有效果，但是第二行调用`component`时运行库就会被加载并使用。为了降低内存使用量，运行Lua脚本时并不会自动加载运行库，因为资源有限。

OpenOS提供了大量自定义库，它们可用于多种用途，从操控与[电脑](computer.md)连接的组件，到包含控制集束红石线时所用颜色或[键盘](../block/keyboard.md)按键编号的引用库。通过像上面那样使用`require()`函数即可在Lua脚本中使用自定义库。某些自定义库需要有特定组件才能工作，例如`internet`库需要有[网卡](../item/internetCard.md)才能使用。对此例而言，这个库甚至是由硬件提供的，即这个运行库在你安装了网卡后才会出现——从技术细节上讲，这个运行库存储在网卡上的一个小的，只读的文件系统中。
