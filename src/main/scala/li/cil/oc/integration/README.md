This package contains code use to integrate with other mods. This is usually done by implementing block drivers for other mods' blocks, or by implementing (item stack) converters.

### General Structure
The general structure for mod integration is as follows:
- All mods' IDs are defined in `Mods.IDs` (`Mods.scala` file).
- For most mods, a `SimpleMod` instance suffices, some may require a specialized implementation. These instances are an internal way of checking for mod availablity.
- For each individual mod, there is a package with a `ModModname` class implementing the `ModProxy` interface. This class is initialized from `Mods.init()` if the mod it represents is available.
- Integration such as driver registration is performed in `ModProxy.initialize()`.

Have a look at the existing modules for examples if that description was too abstract for you.

### On pull requests
The basic guidelines from the main readme still apply, but I'd like to stress again that all integration must be *optional*. Make sure you properly test OC still works with and without the mod you added support for.

An additional guideline is on what drivers should actually *do*. Drivers built into OC should, in general, err on the side of being limited. This way addons can still add more "powerful" functionality, if so desired, while the other way around would not work (addons would not be able to limit existing functionality). Here's a few rules-of-thumb:
- Drivers should primarily be used to *read* information.
- Drivers may "trigger" blocks, as long as the operation is not "physical". For example, setting the maximum energy output of a block would be fine, wheres making a block drop an item from its inventory would not - unless that block is providing such functionality itself, such as a dispenser (remember, *guidelines* :-P).
- Drivers should respect the mod they add integration for. For example, MFR has "mystery" safari nets - it should not be possible to read the contents of those.
- Drivers and converters should avoid exposing "implementation detail". This includes things such as actual block and item ids, for example.
- If there is an upgrade for it, don't write a driver for it. If you're up to it, adjust the upgrade to work in the adapter, otherwise let me know and I'll have a look.

When in doubt, ask on the IRC or open an issue to discuss the driver you'd like to add!
