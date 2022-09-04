# 3D 打印机

![2D 打印过时了。](oredict:opencomputers:printer)

3D 打印机允许你在任何方块上用任何纹理打印出你想要的样子。首先你需要在电脑边放一台打印机，这样电脑就能使用 `printer3d` 这个 API，通过这个 API 就能控制打印机打印出[模型](print.md)了。

用 OPPM 来配置打印机更方便。安装 OPPM（`oppm install oppm`）后，确保你电脑上装有[网卡](../item/internetCard.md)，然后执行以下命令：  
`oppm install print3d-examples`

这些 .3dm 格式的示例可在 `/usr/share/models/` 找到。 阅读示例文件，特别是那个 `example.3dm`，以获取打印选项的信息。另外，你也可以在安装了[网卡](../item/internetCard.md)的电脑上用 `wget` 从 OpenProgram 上下载 `print3d` 和 `print3d-examples`。

打印 3D 模型前需要确保打印机已通过[电脑](../general/computer.md)配置完成。如果设置了循环打印，开始打印后电脑就可以搬走了。你还需要提供[墨盒](../item/inkCartridge.md)和[变色材料](../item/chamelium.md)作为打印材料。变色材料用量和成品大小有关，墨水用量则取决于表面积。

打印需要以下命令：
`print3d /到打印文件的路径/文件.3dm`
也就是说你需要指定 .3dm 文件的路径。

相关文档可在 `/usr/share/models/example.3dm` 找到。
