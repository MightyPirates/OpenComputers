# Waypoint

!["This way!" - "No, that way!"](oredict:opencomputers:waypoint)

The waypoint's use lies not in itself, but in how it can be used. [Navigation upgrades](../item/navigationUpgrade.md) can detect waypoints, so devices with a navigation upgrade can use these waypoints to navigate the world. This is particularly useful for writing easily reusable programs for devices such as [robots](robot.md) and [drones](../item/drone.md).

Note that the actual position reported when queried by a navigation upgrade is *the block in front of the waypoint* (indicated by the particle effects). This way you can place it next to and above a chest, and can refer to the waypoint's position as "above the chest", without having to take the waypoint's rotation into account in your program.

A waypoint has two properties that can be used when querying it via a navigation upgrade: the current level of redstone signal it is receiving, and an editable label. The label is a 32-character long string that can be edited either via a GUI or via the component exposed by the waypoint block. These two properties can then be used on the device to determine what to do with the waypoint. For example, a sorting program can be set to treat all blocks with a high redstone signal as inputs and those with a low signal as outputs.
