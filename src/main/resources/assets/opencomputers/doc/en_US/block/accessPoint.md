# Access Point

![AAA](oredict:oc:accessPoint)

The access point is the wireless version of the [switch](switch.md). It can be used to separate subnetworks so that machines in them will not see [components](../general/computer.md) in other networks, while still allowing to send network messages to the machines in other networks.

In addition to that, this block will resend any wired messages it receives as wireless ones, wireless messages it receives as wired messages, and repeat wireless messages as wireless ones.

[Switches](switch.md) and access point do *not* keep track of which packets they relayed recently, so avoid cycles in your network, or you may receive the same packet multiple times. Due to the limited buffer size of switches, you also should be careful of sending too many messages in a short time, or you will experience package loss. You can upgrade your switches and access points to increase the speed with which they relay messages, as well as their internal message queue size.

Packets are only re-sent a certain number of times, so chaining an arbitrary number of switches or access points is not possible. By default, a packet will hop up to five times.
