# Redstone I/O

![Hi Red.](oredict:oc:redstone)

The Redstone I/O block can be used to remotely read and emit redstone signals. It behaves like a hybrid of a tier 1 and 2 [redstone card](../item/redstoneCard1.md): it can read and emit simple analog as well as bundled signals, but cannot read or emit wireless redstone signals.

When providing a side to the methods of the component exposed by this block, the directions are the global principal directions, i.e. it is recommended to use sides.north, sides.east and so on.

Like the redstone card, this block injects a signal into connected computers when the state of a redstone signal changes - both for analog as well as for bundled signals.
