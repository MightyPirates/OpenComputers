## New features

* [#3533] Added support for observing the contents of fluid container items.
* [1.12.2] Ported some CoFH Core, Ender IO and Railcraft drivers and wrench support.
* Added Railcraft Anchor/Worldspike driver (repo-alt).
* Added Spanish translation (sanmofe).

## Fixes/improvements

* [#3620] Fixed OC 1.8.0+ regression involving API arguments and numbers.
* [#3013] Fixed rare server-side deadlock when sending disk activity update packets.
* Fixed bugs in internal wcwidth() implementation and updated it to cover Unicode 12.
* [1.7.10] Fixed the Database upgrade's documentation not showing up in NEI.
* Fixed server->client synchronization for some types of GPU bitblt operations.
* Fixed string.gmatch not supporting the "init" argument on Lua 5.4.
* Tweaks to server->client networking code:
  * Added support for configuring the maximum packet distance for effects, sounds, and all client packets.
  * Improved the method of synchronizing tile entity updates with the client.
  * Robot light colors are now sent to all observers of the tile entity, preventing a potential (rare) glitch.
* Update GNU Unifont to 15.0.05.

## OpenOS fixes/improvements

* [#3371] Fix minor bug in rm.lua.
* Fix "ls -l" command on Lua 5.4.
* General minor improvements to the codebase.

## List of contributors

asie, ds84182, Possseidon, repo-alt, sanmofe
