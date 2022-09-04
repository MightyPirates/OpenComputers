# Capacitor

![It's over 9000.](oredict:opencomputers:capacitor)

The capacitor stores energy to be used by the network, acting as an energy buffer when needed. Unlike conversion from other mod's energy to OpenComputers' internal energy type (using a [power converter](powerConverter.md) for example), transferring energy inside a single subnetwork is instantaneous. Having an internal energy buffer will be useful for tasks that require a lot of energy, such as [assembling](assembler.md) and/or [charging](charger.md) devices such as [robots](robot.md) or [drones](../item/drone.md). 

The storage efficiency of capacitors increases with the number of capacitors in direct contact or in the vicinity. For example, two capacitors directly next to each other will have a higher storage capacity than the sum of two separated capacitors. This adjacency bonus applies for capacitors up to two blocks away, and is reduced as the distance between capacitors increases.

The capacitor can be connected to a [power distributor](powerDistributor.md) to provide power to other [computers](../general/computer.md) or machines on the network.
