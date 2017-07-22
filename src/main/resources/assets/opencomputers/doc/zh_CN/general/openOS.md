# OpenOS

OpenOS is a basic operating system available in OpenComputers. It is required to boot up a [computer](computer.md) for the first time, and can be crafted by placing an empty [floppy disk](../item/floppy.md) and an OpenComputers [manual](../item/manual.md) inside a crafting table.

Once crafted, the [floppy disk](../item/floppy.md) can be placed inside a [disk drive](../block/diskDrive.md) connected to a [correctly configured](quickstart.md) [computer](computer.md) system, which will allow the [computer](computer.md) to boot up OpenOS.
Once booted, it is advisable to install OpenOS to an empty [hard drive](../item/hdd1.md), foregoing the need for a [floppy disk](../item/floppy.md) and to gain access to a read-write file system (the OpenOS [floppy disk](../item/floppy.md) and other "loot" disks are read-only). A tier 3 [computer case](../block/case3.md) does not require a [disk drive](../block/diskDrive.md), as it has a slot built in for the [floppy disk](../item/floppy.md).

OpenOS can be installed by simply running `install`, and following the on-screen prompts to complete the installation. The [floppy disk](../item/floppy.md) may be removed once the system has been rebooted. OpenOS can be installed on all devices except [drones](../item/drone.md) and [microcontrollers](../block/microcontroller.md) (both of which require manually programming an [EEPROM](../item/eeprom.md) to provide functionality, because they have no built in file system).

OpenOS has many built in functions, the most useful of them is the `lua` command, which opens up a Lua interpreter. This is a good testing space for trying out various commands, and experimenting with component API, before writing the commands into a .lua script. Take note of the information displayed when starting the interpreter, it will tell you how to show results of the commands you enter, and how to exit it.

For more information on programming, refer to the [Lua Programming](lua.md) page. To run Lua scripts, simply type in the name of the file and hit enter (for example, `script.lua` can be run by typing the `script` command in the terminal).
