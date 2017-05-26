# 3D Printer

![2D printing is so yesteryear.](oredict:oc:printer)

3D打印机允许你在任何方块上,用任何纹理打印出你想要的样子.要打印东西, 需要在电脑边放一台打印机.`printer3d` 组件API将启用, 电脑将能够通过这些API,控制打印机打印出[模型](print.md).

用OPPM来设置打印机更方便. 一旦安装 (`oppm install oppm`), 确保你有 [因特网卡](../item/internetCard.md),并执行以下命令:
`oppm install print3d-examples`

示例将会在 `/usr/share/models/` 以.3dm的扩展名存在. 阅读示例文件以获取打印选项的信息, 尤其是这个 `example.3dm`. 你也可以通过安装了网卡的计算机，用wget从OpenProgram下载`print3d` and `print3d-examples`.

要打印模型, 需要通过 [电脑](../general/computer.md)配置打印机. 如果设置了循环打印, 开始任务后电脑就可以搬走了. 你还需要提供 [墨盒](../item/inkCartridge.md) , [油墨](../item/chamelium.md) 作为打印材料. 油墨用量和3D打印大小有关, 墨水取决于表面积.

打印需要以下命令:
`print3d /到打印文件的路径/文件.3dm`

可以在 `/usr/share/models/example.3dm`.找到相关文档