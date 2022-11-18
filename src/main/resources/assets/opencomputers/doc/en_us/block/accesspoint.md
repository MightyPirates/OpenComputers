# Access Point

![AAA](oredict:opencomputers:accessPoint)

*This block is deprecated and will be removed in a future version.* Craft it into a [relay](relay.md) to avoid losing it.

The access point is the wireless version of the [switch](switch.md). It can be used to separate subnetworks so that machines in them will not see [components](../general/computer.md) in other networks, while still allowing to send network messages to the machines in other networks.

In addition to that, this block can act as a repeater: it can re-send wired messages as wired messages to other devices; or wireless messages as wired or wireless messages. 

[Switches](switch.md) and access point do *not* keep track of which packets they relayed recently, so avoid cycles in your network or you may receive the same packet multiple times. Due to the limited buffer size of switches, packet loss can occur when trying to send network messages too frequently. You can upgrade your switches and access points to increase the speed at which they relay messages, as well as their internal message queue size.

Packets are only re-sent a certain number of times, so chaining an arbitrary number of [switches](switch.md) or access points is not possible. By default, a packet will be re-sent up to five times.
