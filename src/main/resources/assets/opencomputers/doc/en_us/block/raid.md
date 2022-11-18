# Raid

![40 man instance.](oredict:opencomputers:raid)

The raid block houses three [hard drives](../item/hdd1.md) which will be combined into a single file system. This combined file system has the size of the sum of the capacities of the individual [hard drives](../item/hdd1.md) and is available to all [computers](../general/computer.md) connected to the raid.

The raid only works (and shows up as a file system) when three [hard drives](../item/hdd1.md) are present. The [hard drives](../item/hdd1.md) may differ in size.

Beware that adding a [hard drive](../item/hdd1.md) to the raid block will wipe it of its contents. Removing a single [hard drives](../item/hdd1.md) from a complete raid will wipe the entire raid. Adding the disk back in will *not* restore the old files; the raid will be re-initialized as an empty file system.

Breaking a raid block will retain its contents, so it can be safely relocated without losing any data.
