# 3D打印机

![2D打印太过时了。](oredict:oc:printer)

3D打印机能让你打印出具有任意形状以及任意材质的方块。使用3D打印机的第一步是将3D打印机方块放置在电脑旁边。这样电脑就可以使用`printer3d`组件API了。你可以用其提供的函数创建并打印出[模型](print.md)来。

3D打印机更简便的配置方法是使用开放式软件包管理器（OPPM）。在安装OPPM（`oppm install oppm`之后，你还需要确认[电脑](../general/computer.md)上装有[因特网卡](../item/internetCard.md)，最后执行以下命令：  
`oppm install print3d-examples`

示例文件的位置在`/usr/share/models/`，格式为.3dm文件。请浏览示例文件，尤其是`example.3dm`，以获取可用操作。此外，你还可以在安装有[因特网卡](../item/internetCard.md)的电脑上使用`wget`从OpenPrograms处下载`print3d`与`print3d-examples`程序。

要打印模型，首先需要用[电脑](../general/computer.md)对3D打印机进行配置。如果设定为不间断打印模式，那么在任务开始后就无需电脑了。你还需要提供[墨盒](../item/inkCartridge.md)以及[变色材料](../item/chamelium.md)作为打印材料。变色材料用量取决于3D打印件的体积，墨水用量则取决于所打印物品的表面积。

要打印物品，需要执行需要以下命令：
`print3d <指向.3dm文件的路径>`
你需要给函数提供指向.3dm文件的路径。

可在`/usr/share/models/example.3dm`找到有关如何创建模型的文档。
