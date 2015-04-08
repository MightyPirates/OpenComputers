# Raid

![40 man instance.](oredict:oc:raid)

The Raid block houses three hard drives which will be combined into a single file system. This combined file system has the size of the sum of the capacities of the individual hard drives and is available to all computers connected to the raid.

The raid only works (and shows up as a file system) when three disks are present. The disks may differ in size.

Beware that adding a hard drive to the raid block will wipe it of its contents. Removing a single disk from a complete raid will also wipe the raid. Adding the disk back in will *not* restore it, the raid's new file system will not contain any files.
