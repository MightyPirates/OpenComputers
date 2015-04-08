# Access Point

![AAA](oredict:oc:accessPoint)

The Access Point is the wireless version of the [switch block](switch.md). It can be used to separate subnetworks so that machines in them will not see components in other networks, while still allowing to send network messages to the machines in other networks.

In addition to that, this block will resend any wired messages it receives as wireless ones, wireless messages it receives as wired messages, and repeat wireless messages as wireless ones.

Switches and access point do *not* keep track of which packets they relayed recently, so avoid cycles in your network, or you may receive the same packet multiple times.

Packets are only re-sent a certain number of times, so chaining an arbitrary number of switches or access points is not possible.
