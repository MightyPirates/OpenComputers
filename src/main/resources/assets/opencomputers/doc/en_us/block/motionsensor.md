# Motion Sensor

![Don't. Blink.](oredict:opencomputers:motionSensor)

The motion sensor allows [computers](../general/computer.md) to detect movement of living entities. If an entity moves faster than a set threshold, a signal will be injected into [computers](../general/computer.md) connected to the motion sensor. The threshold can be configured using the component API that the motion sensor exposes to connected computers.

Movement is only detected if it happens within a radius of eight blocks around the motion sensor, and if there is a direct line of sight from the block to the entity that moved.
