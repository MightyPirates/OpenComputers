## New Features/Support
* **Changed: Diamond Chip recipe**
  - You now require cutting wire to cut the diamond.
* Added: ExtraCells and Mekanism integration (DrummerMC) [1.10.2]
* Fixed: IC2-Classic mod incompatibility 
* Added: Allow getting yaw of player through tablet component (ChristoCoder)
* Fixed: Microcontroller interactions with EnderIO item conduits
* Added: Maximum packet parts to device info of modems (Linked and Network Cards)
* Fixed: Drones now properly work with chunkloader upgrades (TheCodex6824)
* Fixed: Tablets not turning off their screen properly (AmandaCameron)
* Fixed: Motion Sensor line of sight
* Misc: Updated various translations
  - Russian (S0ZDATEL, Fingercomp, makkarpov)
  - Traditional Chinese (mymagadsl)
  - German (Nex4rius)

## OpenOS fixes/improvements
* Fixed: Issues with booting OpenOS on very slow servers
* Added: Allow custom error objects to print to stderr
* Added: Allow mount points to use existing directories
* Added: Bind mounts to mount a directory as another directory
* Fixed: Allow .shrc to use tty stdin
* **Added: Lua REPL input is now parsed with an implicit `return`** (SquidDev)
  - Adding a `=` in front of the code to explicitly add it still works.
* Changed: Shell history no longer adds items if they are duplicates (SquidDev)
* Fixed: CTCP messages in IRC client (Michiyo, skyem123)
* Fixed: Reverse lookup of keys in Keyboard API
* Fixed: event.cancel and event.ignore
* Fixed: Protect lua shell from serialization OOM failure
* Fixed: Too long without yielding error in /bin/tree (LeshaInc)
* Misc: Improvements to the vt100 library
* Misc: Various minor improvements to reduce memory usage

## List of contributors
payonel, Vexatos,  
S0ZDATEL, Fingercomp, makkarpov,  
mymagadsl, Nex4rius, ChristoCoder,  
DrummerMC, LeshaInc, SquidDev, Michiyo,  
josephcsible, skyem123, TheCodex6824,  
AmandaCameron, Pwootage
