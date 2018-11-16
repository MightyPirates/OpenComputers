## New Features/Support

* Misc: Greatly improved Lua execution speed (asiekierka)
  - That means OC now executes Lua code roughly 70% faster than before.
* Misc: Improved Screen rendering performance (cam72cam)
  - Screens now render between 1 and 15 times faster than before, depending on your graphics card.
* Misc: Improved Filesystem and block data saving performance
  - Saving computers to disk is now anything between 5 and 500 times faster than before, depending on your system. Maybe even more.
* Added: Bundled redstone support for ComputerCraft (SquidDev)
* Added: `debug.getlocal` and `debug.getupvalue`
  - They only return the name of the variable, and nothing else.
* Added: `isSunVisible`, `canSeeSky`, and `detect` to geolyzers
* Added: Allow using morse code patterns like `.-.` in `computer.beep`
* Added: redstone component's `setOutput` can now accept values larger than 15
* Added: Allow the keyboard to connect to screens in more ways than before, e.g. facing a side of the screen other than its front
* Added: Readded Project Red support on Minecraft 1.12 (BrisingrAerowing)
* Added: Driver for the Reactor Chamber from IC2
* Added: Inventory GUI for the rack-mounted disk drives
  - Can be accessed either by clicking on the rack or by right clicking the drive in your inventory.
* Added: `getMerchantId` to trade offers from the Trading Upgrade to help with sorting them
* Added: Readded AE2 power support on Minecraft 1.12, meaning you can now power your computers directly off the ME network again.
* Added: `scanContentsAt` to debug card
* Added: More accessible information from Draconic Evolution items
* **Added: The Net Splitter is now a `net_splitter` component**
  - This allows using computers to connect and disconnect various parts of your network.
  - Make sure not to accidentally disconnect your controller!
* Added: Waypoints can now be placed facing up or down
* Added: You can now craft two linked cards together to link them to one another
  - This will unlink them from any previously connected Linked Card.
  - The link channel is also exposed as a property on the item that transposers etc. can read, meaning that you can easily manage multiple linked cards. 
* Added: Allow `setFrequency` on owned Ender Storage chests (payonel and amesgen)
* Added: You can now trigger wake-on-LAN over Linked Cards
* Added: `chunkloaderDimensionBlacklist` and `chunkloaderDimensionWhitelist` to config for (dis)allowing certain dimensions for the chunkloader upgrade
* Added: `disk_drive.media` function that returns the address of the inserted floppy disk
* Added: Forge Energy support to items
  - Battery upgrades, tablets, and hover boots can be charged in Forge Energy compatible devices
  - Battery upgrades also support power extraction, allowing them to recharge Forge Energy devices acting as normal batteries
* Added: The Analyzer now reports the internal components of an Adapter block when right-clicked
* **Added: New feature for filesystems: Locked mode!**
  - A locked filesystem is read-only and cannot be unlocked unless recrafted or its mode is switched between managed and unmanaged, either action wiping the drive.
  - The name of the player who locked it is shown in the tooltip, allowing authenticated sharing of data.
* **Changed: `redstone_changed` event for bundled signals**
  - Now includes the colour that changed, and only reports the old and new values for that colour
* Changed: The order in which cases are filled with components is now based on the slot tiers
* Changed: OpenComputers is now a lot more quiet in the server log. (kmecpp)
* **Changed: `robot.suck`, `robot.suckFromSlot`, and `transposer.transferItem` return values**
  - Instead of `true`, they now return the number of transferred items on success.
* Changed: Use less annoying particles for nanomachines
* Changed: Increased default number of platters in an (unmanaged) Tier 3 Hard Drive from 6 to 8
  - You will have to update an existing config yourself by changing `hddPlatterCounts`.
* Misc: Improved cable rendering (SquidDev)
* Misc: Robot inventories should now be compatible with even more modded inventory manipulation things
* Misc: Robot Crafting (the Crafting Upgrade) should now be compatible with even more modded recipes
* Misc: Screens should now stop working a lot less on server restarts etc. and be generally a lot more robust
* Misc: Robot `swing` and `use` should now be _a lot_ more robust and work with a lot more modded items like Hammers and Bows from Tinkers' Construct
* Misc: Robot tank interactions should now work well with a wide range of modded tanks.
* Misc: Improved chunkloader upgrade (svitoos)
  - Chunkloaders are now allowed in Microcontrollers.
* Misc: Added more unicode glyphs to font (asiekierka)
* Fixed: Inventory loss during minecraft server crashes
* Fixed: Crash when placing microcontroller
* Fixed: Allow the robot to swing at anything that would block its movement
* Fixed: `oc_nanomachines` or `oc_nm` command not always working on servers
* Fixed: Item duplication bug involving robots and voodoo magic
* Fixed: Robot `move` commands not always _actually_ returning whether the robot really moved or not
* Fixed: Forcing the use of the LuaJ architecture not forcing the use of the LuaJ architecture
* Fixed: `transferItem` checking the wrong side (cyb0124)
* Fixed: "Unknown error" when transfering fluid to certain machines
* Fixed: Item duplication bug involving drones and Wither voodoo magic
* Fixed: Potential error with IC2 on launch
* Fixed: Robots eating items they shouldn't eat when crafting
* Fixed: Angel Upgrade not doing the one job it had
* Fixed: Robots being really bad at trading with villagers. They were sent to business school so now they are a lot better at it.
* Fixed: Robots forgetting how to move
* Fixed: Item duplication bug involving robots and drones doing shady business with one another
* Fixed: Network floppy disk not installing
* Fixed: Fluid duplication bug involving robots being bad at draining fluids
* Fixed: Drones getting funky after a wake-on-LAN
* Fixed: Weird item update glitches involving robots and certain blocks like AE2 Interfaces
* Fixed: Item duplication bugs involving EEPROMs' desire to behave like quantum particles
* Fixed: Various fixes to AE2 integration
  - `slot` parameter in `exportIntoSlot` of the Export Bus is now an optional parameter
* Fixed: Crash with Applied Llamagistics
* Fixed: Crashes when you try to spawn computers by... unconventional means
* Fixed: Setting `enableNanomachinePfx` to `false` in the config not actually doing anything
* Fixed: When a robot gains experience, it now properly triggers modded effects that happen on XP Orb pickup
* Fixed: Confusing Analyzer reports on computers that are shut down
* Fixed: Microcontrollers now properly shutting down internal components
* Fixed: Leash upgrade erroring for addon developers (josephcsible)
* Fixed: World Sensor Card crafting recipe on Minecraft 1.10 and above
* Fixed: Client crash involving cables and chunk loading (thiakil)
* Fixed: Tablet screen freezing on certain events
* Fixed: Terminal servers not properly connecting their Remote Terminals
* Fixed: Lightning issues with ShaderMod (paulhobbel)

## OpenOS fixes/improvements

* Added: `reset` command that clears the screen and resets the resolution to maximum
* Added: rc errors are now being logged to /tmp/event.log
* Added: -f option to cp
* Added: aliases as part of tab complete in shell
* Added: devfs psuedo files can now be zero-size
* Added: Allow processes to handle hard interrupts
  - The process metadata now contains a `signal` field that is triggered on hard interrupts
* Added: Support for `\b` and `\r` characters to tty
* Added: Cut and Uncut to `edit` (AntiBlueQuirk)
  - Ctrl+K to cut, Ctrl+U to insert a line
* Misc: tty and cursor logic separated, reducing memory cost for custom cursor behavior with term options
* Misc: Improved command substitution, now more like Linux sh
* Misc: `less` is now the program used for `man`
* Fixed: Various vt100 fixes
* Fixed: Processes now close file handles on exit
* Fixed: Autorun on read-only filesystems
* Fixed: Crash in `edit`

## List of contributors
payonel, Vexatos,  
cam72cam, asiekierka,  
SquidDev, kmecpp,  
BrisingrAerowing, cyb0124,  
svitoos, AntiBlueQuirk,  
josephcsible, amesgen,  
thiakil, paulhobbel
