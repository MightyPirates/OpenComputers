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
* Added: Official support for AArch64 on Linux and macOS.
* Added: Subtle indentations to Redstone I/O texture.
  * The amount of darkened dots on each side marks the ordinal number of the side it responds to.
* Added: Support for non-BMP Unicode codepoints!
  * To clarify, Unicode characters >= 0x10000 should now be fully supported - as long as they're provided in the font.
* Changed: New limitFlightHeight configuration definition.
  * This allows values above 256 to be used, which may be useful for Cubic Chunks users.
* Changed: The game now crashes instead of reloading defaults if a config file is present but invalid.
* Removed: Native Lua library support for FreeBSD and x86 macOS.
* (1.7.10) Fixed: [#3239] Inconsistencies in Robot block clicking.

## OpenOS fixes/improvements

* Fixed: [#3558] Invalid file modification timestamps in /bin/ls.

## List of contributors

asie, payonel, Smok1e
