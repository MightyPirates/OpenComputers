# EEPROM

![Let's get this party started.](oredict:opencomputers:eeprom)

The EEPROM is what contains the code used to initialize a computer when it is being booted. This data is stored as a plain byte array, and may mean different things to different [CPU](cpu1.md) architectures. For example, for Lua it is usually a small script that searches for file systems with an init script, for other architectures it may be actual machine code.

EEPROMs can be programmed for specialized purposes, such as [drones](drone.md) and [microcontrollers](../block/microcontroller.md).
