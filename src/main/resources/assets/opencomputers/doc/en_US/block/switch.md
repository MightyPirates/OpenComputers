# Switch

![Building bridges.](oredict:oc:switch)

The switch can be used to allow different subnetworks to send network messages to each other, without exposing components to computers in other networks. Keeping components local is usually a good idea, to avoid computers using the wrong screen or to avoid component overflows to happen (in which computers will crash / not start anymore).

There is also a wireless variation of this block, the [access point](accessPoint.md), which will also relay messages wirelessly.

Switches and access point do *not* keep track of which packets they relayed recently, so avoid cycles in your network, or you may receive the same packet multiple times.

Packets are only re-sent a certain number of times, so chaining an arbitrary number of switches or access points is not possible.
