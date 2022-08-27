## New Features/Support

* Added: Access to waypoint address UUIDs in the Navigation Upgrade. (hohserg1)
* Added: [#779] Graphics Card video RAM system.
  - Graphics Cards now have multiple internal video RAM buffers, which can be allocated and freed.
  - Reads and writes to Video RAM have zero costs.
  - Writing to the text buffer outside of the viewport now has zero costs.
* Added: More complete Unicode support!
  - Unscii has been upgraded to version 2.1 (with funscii patches).
  - Unifont 14.0.04 can now be used to fill in missing glyphs, thanks to the license change.
  - The above mean that OpenComputers now supports the near-complete Unicode Basic Multilingual Plane.
* Added: More Upgrades now have a descriptive tooltip.
* Added: New mod integrations:
  - GregTech: Seismic Prospector data reading. (repo-alt)
  - Thaumic Energistics: Distillation pattern aspect information. (repo-alt)
* Changed: Block 0.0.0.0/8 from internet card by default. (divergentdave)
* Changed: Game logs now contain the dimension when reporting a machine's position. (D-Cysteine, repo-alt)
* Changed: Make Lua BIOS take use tail call optimisation. (skyem123)
* Changed: [#3440] 'media()' is now implemented on Server disk drives.
* Changed: Motion Sensor now considers both feet and eyes when checking for the visibility of an entity.
* Changed: The default CPU architecture is now Lua 5.3.
* Changed: When creating a new Rack, the "Relay Mode" is now disabled by default.
* Misc: Updated the following translations:
  - Chinese (Low-power)
  - German (JakobDev)
  - Portuguese (guilherme-puida)
  - Russian (Fingercomp, Smollet777)
* (1.7.10) Fixed: AE2 filtering by keys which are not always present.
* Fixed: AE2 item stack sizes larger than 2^31-1.
* Fixed: Barcode Reader upgrade crash when scanning anything that is not a valid target. (AmandaCameron)
* Fixed: [#3509] ByteBufInputStream memory leak.
* Fixed: [#3187] Crash with CodeChickenLib and IC2 installed.
* Fixed: [#3247] Disassembler accepted whole stack via direct inventory access.
* Fixed: [#3254] Edge case issues with Hologram copy().
* Fixed: [#2999, #3225] Edge case issues with deleting computer/robot persistence data.
* (1.12.2) Fixed: Ender IO/Project: Red wrench compatibility.
* Fixed: [#3159] Error when calling 'debug.sendToDebugCard()'.
* Fixed: [#3494] Errors when using block GUIs on larger/negative Y values (f.e. with Cubic Chunks).
* Fixed: [#3391] Generator upgrade destroys fuel containers.
* Fixed: Inconsistent 3D print item stacking (Quant1um).
* Fixed: [#2911] Inconsistent values used by getGameType() and setGameType() in Debug Card.
* (1.12.2) Fixed: [#3472] Incorrect 3D print lighting.
* Fixed: [#3226] Incorrect Hard Drive reported maximum stack size when formatted.
* Fixed: [#3184] Incorrect redstone card sides inside racks and computers.
* Fixed: [#3182] Incorrect reporting of entity inventory names in Transposer, plus other Transposer interaction issues.
* Fixed: Missing null check for Blood Magic integration.
* Fixed: [#3336] Missing null check for GregTech data stick NBTs.
* Fixed: [#3249] NullPointerException when remote terminal is missing.
* Fixed: [#3401] 'rawSetForeground', 'rawSetBackground' not working correctly.
* Fixed: [#3265] Relay 'setStrength' unlimited upper bound. (JamesOrdner)
* Fixed: [#1999] 'string.gsub' patterns now allow numbers.
* Fixed: [#3195] Tier 1 Wireless Cards not receiving messages.
* (1.7.10) Fixed: [#3239] Unnecessary/unwanted canEntityDestroy check in OpenComputers fake player.
* Fixed: Update issues in the Floppy Drive GUI.

## OpenOS fixes/improvements

* Changed: Added binary support to 'text.internal.reader'.
* Changed: Errors are now passed back to 'shell.execute'.
* Changed: 'install' no longer clobbers '/etc/rc.cfg' nor '/home/.shrc'.
* Changed: If /home is read-only, a helpful message is displayed to tell the user to run 'install'.
* Changed: Removed '-i' from 'cp' alias.
* Changed: [#3320] VT ABCD should move 1 character by default.
* Changed: [#3305] 'wget' now passes a default user agent.
* Fixed: [#3423] Can't yield from an orphan coroutine.
* Fixed: Crash when calling 'tty.setViewport' without arguments.
* Fixed: [#3499] 'edit' crashing once clicking somewhere.
* Fixed: [#3196] Env pass in 'sh' command.
* Fixed: [#3201] 'io.input' implementation inconsistency.
* Fixed: [#1207] I/O buffer reading splitting UTF-8 sequences.
* Fixed: Minor issues in the OpenOS manpage for 'ls'. (avevad) 
* Fixed: [#3308] Out of memory error isn't reported in the shell in certain conditions.
* Fixed: Shift+Backspace handling in '/bin/edit'.
* Numerous small improvements to the codebase.

## List of contributors

payonel, asie,
AmandaCameron, avevad,
D-Cysteine,
divergentdave, hohserg1,
JamesOrdner, repo-alt,
Fingercomp, guilherme-puida,
JakobDev, Low-power,
Quant1um, skyem123,
Smollet777
