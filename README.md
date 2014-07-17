This is an addon for OpenComputers, providing drivers for additional blocks to be accessible via the Adapter block. It is *not* a full re-implementation of OpenPeripherals for OpenComputers, it's a little more simplistic in its architecture only provides drivers for a subset of the blocks OP supports - for now, anyway.

[Download](http://ci.cil.li/job/OpenComponents/)
--------

OpenComponents is generally in continuous development, in that support for more blocks is added. Any 'deeper' changes will not be made in the master branch, meaning it should generally be considered stable. Meaning you can simply get the [latest build from the Jenkins build server](http://ci.cil.li/job/OpenComponents-MC1.7/).

Contributing
------------

Please refer to the existing code to get an idea for the code conventions. Formatting is generally done using the IntelliJ autoformatter, so if you can use that or a compatible configuration that will help greatly.

We'd be happy for added compatibility for any mod, as long as this mod does not become *dependent* on the other mod. All mod support must be purely *optional*. This mod must work fine in a Minecraft installation that has OpenComputers as the only other mod in it. Please also stick to read-only behavior for the most part. There are always exceptions to the rule, of course. For example, the `IInventory` driver allows manipulating inventories in a fashion similar to how robots can manipulate their own inventory.
