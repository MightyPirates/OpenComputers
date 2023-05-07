## New Features/Support

* Added: Experimental Lua 5.4 support (Lua 5.4.4).
  * For now, this is hidden behind a configuration option.
* Added: Forestry circuit boards' internal layout is now available to Lua scripts.
* Added: Major upgrade of Lua libraries.
  * Updated LuaJ to 3.0.2 with many third-party patches applied.
  * JNLua is now compiled with proper optimizations - ~2x better performance!
  * Lua 5.2 has been updated with gamax92's backported bugfixes.
  * Lua 5.3 has been updated from 5.3.2 to 5.3.6.
  * 64-bit integers in calls should now be handled properly.
* Added: New robot names.
* Added: Official support for AArch64 on Linux and macOS.
* Added: Source tank parameter for Transposer transferFluid(). (repo-alt)
* Added: Subtle indentations to Redstone I/O texture.
  * The amount of darkened dots on each side marks the ordinal number of the side it responds to.
* Added: Support for non-BMP Unicode codepoints!
  * To clarify, Unicode characters >= 0x10000 should now be fully supported - as long as they're provided in the font.
* Added: Support for partial font overrides in resource packs.
  * If a resource pack's font.hex file only contains some glyphs, missing glyphs present in parent resource packs won't disappear.
* Added: Support for the "PATCH" HTTP method. (hohserg1)
* Added: New config option: "transposerFluidTransferRate". (repo-alt)
* Changed: New limitFlightHeight configuration definition.
  * This allows values above 256 to be used, which may be useful for Cubic Chunks users.
* Changed: Replaced the forceNativeLibWithName config option with two new ones: forceNativeLibPlatform and forceNativeLibPathFirst. (TheCodex6824)
  * forceNativeLibPlatform allows overriding the normally auto-detected platform string to a custom value, in case the user is on an unsupported platform.
  * forceNativeLibPathFirst allows choosing a directory to check for natives in, instead of always searching in the jar for one. This allows custom natives to be used without packing them into the mod jar first, which should be much easier for end users.
* Changed: The game now crashes instead of reloading defaults if a config file is present but invalid.
* Fixed: [#3588] Renaming over other files does not properly free space.
* Fixed: [#3591] Memory leak with wrapped worlds from other mods.
* Fixed: [#3596] Freeze when connecting a ComputerCraft peripheral via an Adapter (Kosmos-Prime)
* Fixed: [#3603] computer.getDeviceInfo doesn't pause the computer immediately (AR2000AR)
* Fixed: [#3609] Swapped arguments in a graphics card "bitblt()" edge case. (Kosmos-Prime)
* Removed: Native Lua library support for x86 (32-bit) macOS.
* (1.7.10) Fixed: [#3239] Inconsistencies in Robot block clicking.

## OpenOS fixes/improvements

* Fixed: [#3558] Invalid file modification timestamps in /bin/ls.

## List of contributors

AR2000AR, asie, hohserg1, Kosmos-Prime, payonel, repo-alt, Smok1e, TheCodex6824
