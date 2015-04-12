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
- [OpenOS](general/openOS.md)
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

Most devices are able to run a basic operating system called [OpenOS](general/openOS.md) (with the exception of drones and microcontrollers). OpenComputers allows for creation of custom OSes and Architectures, should the player desire it.

Devices have access to various resources such as [disk space](item/hdd1.md) and [memory (RAM)](item/ram1.md). [Microcontrollers](block/microcontroller.md) are cheap [computers](general/computer.md) with less functionality and components, and do not have an operating system, requiring creative use of programming. [Robots](block/robot.md) are mobile [computers](general/computer.md) and are able to interact with blocks and entities (but are unable to interact with external OpenComputers components). [Drones](item/drone.md) are fast, entity-based [robots](block/robot.md) with limited functionality, able to move differently and are able to interact differently with the world. [Servers](item/server1.md) are higher tier [computers](general/computer.md) and are able to hold more components, increasing the amount of resources available to control larger networks and run larger programs. 

This manual contains detailed information regarding all blocks and items, how to set up different types of systems and devices, as well as an introduction to Lua programming.
