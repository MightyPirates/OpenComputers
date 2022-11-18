# Switch

![Building bridges.](oredict:opencomputers:switch)

*本方块已废弃，将会在未来版本被移除* 请在工作台中将其合成为[中继器](relay.md)以避免丢失。

The switch can be used to allow different subnetworks to send network messages to each other, without exposing components to [computers](../general/computer.md) in other networks. Keeping components local is usually a good idea, to avoid [computers](../general/computer.md) using the wrong [screen](screen1.md) or to avoid component overflows to happen (causing [computers](../general/computer.md) to crash and refuse to boot up).

There is also a wireless variation of this block, called the [access point](accessPoint.md), which will also relay messages wirelessly. Wireless messages can be received and relayed by other [access points](accessPoint.md), or by [computers](../general/computer.md) with a [wireless network card](../item/wlanCard1.md).

Switches and [access points](accessPoint.md) do *not* keep track of which packets they relayed recently, so avoid cycles in your network or you may receive the same packet multiple times. Due to the limited buffer size of switches, sending messages too frequently will result in packet loss. You can upgrade your switches and [access points](accessPoint.md) to increase the speed with which they relay messages, as well as their internal message queue size.

Packets are only re-sent a certain number of times, so chaining an arbitrary number of switches or access points is not possible. By default, a packet will be re-sent up to five times.
