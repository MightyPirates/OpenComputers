# OpenComputers Manual

OpenComputers is a mod that adds persistent, modular, and highly configurable [computers](general/computer.md), [servers](item/server1.md), [robots](block/robot.md), and [drones](item/drone.md) to the game. All devices can be programmed using Lua 5.2, allowing for systems with varying complexity depending on the usage. 

To learn about how to use the manual, check out [the page about the manual](item/manual.md) (that green text is a link, you can click it).

## Table of Contents

### Devices
- [Computers](general/computer.md)
- [Servers](item/server1.md)
- [Microcontrollers](block/microcontroller.md)
- [Robots](block/robot.md)
- [Drones](item/drone.md)

### Software and Programming
- [OpenOS](general/openos.md)
- [Lua](general/lua.md)

### Blocks and Items
- [Items](item/index.md)
- [Blocks](block/index.md)

### Guides
- [Getting Started](general/quickstart.md)

## Overview

As mentioned above, computers in OpenComputers feature persistence, which means that a running [computer](general/computer.md) retains its state when the chunk it is in is unloaded. This means that if the player moves away from the [computer](general/computer.md), or logs off, the [computer](general/computer.md) will remember its last known state and continue from that point on when the player goes near the [computer](general/computer.md). Persistence works for all devices except for [tablets](item/tablet.md).  

All devices are modular and can be assembled with a wide range of components, just like [computers](general/computer.md) in real life. Players who enjoy tinkering will be able to optimize devices to their heart's content. If desired, devices can be [dismantled](block/disassembler.md) and rebuilt if the initial configuration wasn't satisfactory. For [computers](general/computer.md) and [servers](item/server1.md), components can be swapped out on-the-fly simply by opening the corresponding GUI. 

OpenComputers devices are compatible with many different mods for manipulation of blocks and entities (through the [adapter](block/adapter.md), or specific upgrades in a [robot](block/robot.md) or [drone](item/drone.md)). Power can be supplied using a large range of other mods, including, but not limited to, Redstone Flux, IndustrialCraft2 EU, Mekanism Joules, Applied Energistics 2 energy as well as Factorization Charge. 

Devices in OpenComputers have extra features as well as some limitations. [Computers](general/computer.md) are the base-line, and are able to take a fair number of components, controlled by the CPU tier being used. [Computers](general/computer.md) also have access to components on all six sides. [Servers](item/server1.md) are able to connect to more components (internally or externally) than a [computer](general/computer.md), through the use of [component buses](item/componentBus1.md); however, due to the [rack](block/rack.md), the [server](item/server1.md) is only able to access components from a single side of the [rack](block/rack.md), as configured in the [rack](block/rack.md) GUI. [Microcontrollers](block/microcontroller.md) are further limited (compared to [computers](general/computer.md)) by their lack of [hard drive](item/hdd1.md) and [disk drive](block/diskDrive.md) slot, which means [OpenOS](general/openOS.md) can not be installed on a [microcontroller](block/microcontroller.md). [Microcontrollers](block/microcontroller.md) do have a slot for an [EEPROM](item/eeprom.md), and can be programmed with a more focused operating system for a limited set of tasks. 

[Robots](block/robot.md) are moving [computers](general/computer.md), and are able to interact with the world (but cannot interact with external OpenComputers blocks). Unlike [computers](general/computer.md), once a robot is built, the components inside the [robot](block/robot.md) cannot be removed. To circumvent this limitation, [robots](block/robot.md) may be built with [upgrade](item/upgradeContainer1.md) or [card](item/cardContainer1.md) containers, allowing for on-the-fly swapping of cards or upgrades, if needed. [OpenOS](general/openOS.md) can be installed on [robots](block/robot.md) by placing a [disk drive](block/diskDrive.md) in a container slot, which will allow insertion of [floppy](item/floppy.md) disks, or by placing a [hard drive](item/hdd1.md) with [OpenOS](general/openOS.md) pre-installed in one of the [hard drive](item/hdd1.md) slots. To fully reconfigure a [robot](block/robot.md), it will need to be [disassembled](block/disassembler.md) first. [Drones](item/drone.md) are limited versions of [robots](block/robot.md). They move differently, contain fewer inventory slots, and lack an operating system (similarly to [microcontrollers](block/microcontroller.md), [drones](item/drone.md) can be configured with a programmed [EEPROM](item/eeprom.md) for a limited set of tasks). For the most part, [robots](block/robot.md) and [drones](item/drone.md) share the same upgrades and components; however, upgrades behave differently in [drones](item/drone.md), such as [inventory upgrades](item/inventoryUpgrade.md) only providing 4 slots per upgrade, for a total of 8 slots, while [robots](block/robot.md) are capable of taking more [inventory upgrades](item/inventoryUpgrade.md) (total of 4) as well as getting more slots per upgrade (16 slots per upgrade).

This manual contains detailed information regarding all blocks and items, how to set up different types of systems and devices, as well as an introduction to Lua programming.
