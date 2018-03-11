## New Features/Support
* Added: Tier 1 Wireless Network Card (TheCodex6824)
  - Can only have one open port at a time
  - Has a signal strength of 16 blocks
  - The already existing wireless network card is now the Tier 2 card, has not been changed in any other way.
* **Added: Sheep Power** (and ocelot power)
  - **Carpeted Capacitors** exist now. They work and connect just like normal capacitors and form blocks with them.
  - Carpeted Capacitors generate power when sheep or ocelots walk on them.
  - Power is generated when at least 2 of a type of animal are present. A single sheep and a single ocelot generate no power.
  - Ocelots are notorious for their tendency to summon electrostatic fields from the feline dimension. They generate more power than sheep do.
  - Yes, this means you can now use OC without any energy-producing mod again without having to change the config. 
  - Insert interesting reference mentioning electric sheep here
* Added: Creative Component Bus (Xyxen)
  - Allows servers to support up to 1024 components
* **Changed: Lua 5.3 is now the default architecture for newly crafted CPUs**
  - CPUs that were crafted prior to this update will continue running whichever architecture they were set to, or Lua 5.2
* Added: You can now change the name of a robot using another computer.
  - Added `setName` and `getName` to robots.
  - Computers connected to the robot can access the robot as a component to call these functions.
  - The Robot must be shut down for this to work
* Fixed: Certain characters and glyph width in screen rendering
* Fixed: Blocks with inventories failing to save under certain circumstances
* Fixed: Drones with chunkloader upgrades not always properly loading chunks (TheCodex6824)
* Fixed: AppliedEnergistics 2 integration
  - Certain filters for `getItemsInNetwork()` not working
  - `getCpus()` not returning the correct number
  - `exportIntoSlot()` not working
  - Added `isCraftable` to gathered item data
* Fixed: `computer.addUser` not erroring properly
* Fixed: Made cable collision box closer to cable shape (SquidDev)
* Fixed: Crafting Upgrade not always crafting what it should be crafting
* Fixed: Crafting Upgrade making items uncraftable
* Fixed: Motion Sensor still not working properly
* Fixed: Robots interacting with items that directly modify their inventory (like IC2 fluid cells)
* Fixed: Potential memory leak in networking code
* Fixed: `getMetadata` on the Debug Card on Minecraft 1.10 and above (BrisingrAerowing)
* Added: `getBlockState` for the Debug Card  on Minecraft 1.10 and above (BrisingrAerowing)
* Fixed: Crafting a robot or drone with an EEPROM not working on Minecraft 1.10 and above
* Fixed: `getAllStacks()` on the Inventory Controller Upgrade and Transposer has been backported to Minecraft 1.7.10.

* Misc: Updated French translation (Naheulf)

## OpenOS fixes/improvements
* Fixed: install.lua now should work more like one would expect
* Changed: uuid.lua is now generating valid RFC4122 version 4 UUIDs (jobe1986)
* Fixed: Various fixes to vt100 support
* Fixed: Memory leak in process loading
* Fixed: Made modifier keypresses more specific in /bin/edit

## List of contributors
payonel, Vexatos,  
Xyxen, TheCodex6824, SquidDev,  
BrisingrAerowing, jobe1986,  
Naheulf, SDPhantom,  
Zerotiger, anar4732
