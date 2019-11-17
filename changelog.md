## New Features/Support

**This will be the last version for Minecraft 1.11.2. Minecraft 1.7.10, 1.10.2, and 1.12.2 will keep receiving updates.**

* **Added: Barcode reader upgrade!** (AmandaCameron)
  - An Analyzer can now be installed in a Tablet as an upgrade.
  - Provides the `barcode_reader` component.
  - When clicking on a block with a tablet containing this upgrade, the `tablet_use` event will contain information the Analyzer would normally reveal.
  - This allows getting components' addresses into OC directly by clicking on blocks. 
* Added: Config option to set max signal queue size (default 256, the same as before).
  - Signals pushed to the computer when the queue is full are dropped.
* Added: Allow different HTTP request methods in `internet.request` (the method to use is now the fourth optional argument).
* Added: You can now install Angel Upgrades in drones (Minecraft 1.12 only).
* Added: Chargers can now charge items in nearby players' inventories.
* Added: Experience Upgrade now shows its level in its tooltip (Minecraft 1.12 only).
* Added: Extended item information to Thaumcraft Essentia Jars on Minecraft 1.12 (seebs)
* Added: Support for SimpleLogic bundled cables on Minecraft 1.12. (asiekierka)
* Added: Re-added Wireless Redstone (ChickenBones Edition) support on Minecraft 1.12.
* Misc: Hide bounding box wireframe on screens while not sneaking
* Misc: More robot names.
* Misc: Updated the chinese translation of the manual. (3TUSK, ZeroAurora, JackyWangMislantiaJnirvana)
* Changed: Cleaned up some wording in the config file.
* Changed: `gpu.bind` is now faster.
* Changed: `computer.pushSignal` now accepts tables of simple key-value pairs, but not nested tables.
* Changed: APU tiers now correspond to their CPU tiers.
* Changed: Putting unmanaged hard drives into a Raid now forces them into managed mode along with wiping them.
* Fixed: Robots being unable to use buckets.
* Fixed: Fluid dupe bug that I will _not_ explain to you.
* Fixed: Tier 2 wireless network card not receiving wired messages.
* Fixed: Return value of `robot.swing` when the block breaks too fast.
* Fixed: Server racks not sending messages to mountables quickly enough.
* Fixed: Relays not displaying traffic accurately.
* Fixed: Relay message relaying issues.
* Fixed: `itemDamageRate` config option set to 0 not working. (svitoos)
* Fixed: Crash with `hologram.copy`.
* Fixed: Geolyzer's `isSunVisible`.
* Fixed: Crash with remote terminals.
* Fixed: A Robot without inventory deleting the items it drops.
* Fixed: `too long without yielding` sometimes not triggering when it should.
* Fixed: Crash when blowing up a computer while code is running.
* Fixed: Another fluid dupe bug that I will definitely not explain to you either. Stop asking.
* Fixed: Available architectures not always being what they should be.
* Fixed: Crashes in AE2 integration.
* Fixed: The AE2 ME Interface part not having network control.
* Fixed: AE2 ME cells not having all intended information on inspection. (wkalinin)
* Fixed: Crash with AE2 when power usage is disabled.
* Fixed: AE2 interface not being recognized as components when channels are disabled.
* Fixed: Some AE2 integration not working on 1.7.10. (wkalinin)
* Fixed: Another crash with AE2 when power usage is disabled.
* Fixed: Specific AE2 integration being very slow.
* Fixed: Crash with IC2 Classic.

## OpenOS fixes/improvements

* Fixed: Error related to installing OPPM.
* Fixed: OpenOS timers being starved during blocking pulls.
* Fixed: `reset` alias to reset the screen resolution to its maximum.
* Fixed: Certain TCP connections in Network loot disk
* Fixed: Various vt100 fixes
* Fixed: Now errors properly on using `print` with bad `__string` metamethods 

## List of contributors
payonel,  
AmandaCameron, wkalinin,  
LizzyTrickster, svitoos,  
kchanakira, seebs,  
asiekierka,  
3TUSK, ZeroAurora,  
JackyWangMislantiaJnirvana
