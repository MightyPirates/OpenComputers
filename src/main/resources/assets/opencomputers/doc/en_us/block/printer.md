# 3D Printer

![2D printing is so yesteryear.](oredict:opencomputers:printer)

3D printers allow you to print any block of any shape, with any type of texture. To get started with 3D printers, you will need to place down a 3D printer block next to a computer. This will give access to the `printer3d` component API, allowing you to set up and print [models](print.md) using the provided functions.

A more convenient way to setup 3D printers is to use Open Programs Package Manager (OPPM). Once installed (`oppm install oppm`), make sure you have an [internet card](../item/internetCard.md) in your [computer](../general/computer.md) and run the following command:
`oppm install print3d-examples`

The examples can then be found in `/usr/share/models/` as .3dm files. Take a look through the example files for available options, in particular the `example.3dm` file. Alternatively, you can download the `print3d` and `print3d-examples` programs from OpenPrograms using `wget` and an [internet card](../item/internetCard.md).

In order to be able to print the models, a 3D printer needs to be configured via a [computer](../general/computer.md). If set to print non-stop, the computer will no longer be required thereafter. You will also need to provide an [ink cartridge](../item/inkCartridge.md) and some [chamelium](../item/chamelium.md) as input materials. The amount of chamelium used depends on the volume of the 3D print, while the amount of ink used depends on the surface area of the printed item.

To print an item, use the following command:
`print3d /path/to/file.3dm`
providing the path to the .3dm file.

Documentation pertaining to creating your own models can be found in `/usr/share/models/example.3dm`.
