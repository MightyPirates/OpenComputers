# Lua 

The Lua [reference manual](http://www.lua.org/manual/5.2/manual.html) and the [Programming in Lua](http://www.lua.org/pil/) books (first edition is available for free online) are a good place to get started with the basics of Lua and becoming familiar with the basic syntax and standard libraries. [OpenOS](openOS.md) strives to emulate the standard libraries very closely, with a few deviations, such as the mostly missing debug library (for sandboxing reasons). These differences are [documented on the wiki](https://ocdoc.cil.li/api:non-standard-lua-libs).

Non-standard libraries will need to be
`require`d in order to use them in a script. For example:

`local component = require("component")`
`local rs = component.redstone`

This will allow you to call all of the functions provided by the [redstone](../item/redstoneCard1.md) component. For example:

`rs.setOutput(require("sides").front, 15)`

**Important**: when working in the Lua interpreter, *do not* use `local`, as that will make the variables local to a single line of input. Meaning if you were to enter the lines above one after another in the interpreter, the third one would error, telling you that `rs` is a `nil` value. Why only on the third line, you ask? Because, for ease of testing, the interpreter tries to load unknown variables as libraries. So even though the assignment to `component` from the first line would do nothing, the use of `component` on the second line would cause that library to be loaded and used. Libraries are not automatically used when using Lua scripts to keep memory usage low, because that's a limited resource.

OpenOS provides many custom libraries which can be used for many applications, from controlling and manipulating components attached to the [computer](computer.md), to reference APIs for colors used in bundled redstone control and [keyboard](../block/keyboard.md) keycodes. Custom libraries can be used within a Lua script by using the `require()` function, as above. Some custom libraries require specific components to work, such as the `internet` library requiring an [internet card](../item/internetCard.md). In that particular case it is even being provided by it, i.e. the library will show up once you install an internet card - technically speaking, it is contained on a small, read-only file system on the internet card.
