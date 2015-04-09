# Capacitor

![It's over 9000.](oredict:oc:capacitor)

The Capacitor stores energy to be used by the network, acting as an energy buffer when needed. Unlike conversion from other mod's energy to OC's internal energy type (using a [power converter](powerConverter.md), transfering energy inside a single OC subnetwork is instantaneous, so it can be advantageous to store some energy internally for tasks that consume a lot of energy, such as assembling devices in the [assembler](assembler.md) or charging [robots](robot.md).

The storage efficiency of capacitors increases with the number of capacitors in direct contact or in the vicinity. For example, two capacitors directly next to each other will have a higher storage capacity than the sum of two separated capacitors. This adjacency bonus applies for capacitors up to two blocks away, and is reduced as the distance between capacitors increases.

The Capacitor can be connected to a [power distributor](powerDistributor.md) to provide power to other [computers](../general/computer.md) or machines on the network. 