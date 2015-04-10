# Lua 

The Lua [reference manual](www.lua.org/manual/5.2/manual.html) is a good place to get started with the basics of Lua and becoming familiar with the basic syntax and standard libraries. OpenComputers has very similar standard libraries, and omits certain libraries entirely (such as the debug library). 

Non-standard libraries will need to be *required* in order to use them in a script. For example:

*local component = require("component")  *
*local rs = component.redstone  *

This will allow you to call all of the functions provided by the [redstone](../item/redstoneCard1.md) component. 

OpenComputers provides many custom libraries which can be used for many applications, from controlling and manipulating components attached to the [computer](computer.md), to reference APIs for colors and keyboard keycodes. Custom libraries can be used within a Lua script by using the *require()* function, as above. Some custom libraries require specific components to work, such as the Internet library requiring an [internet card](../item/internetCard.md). 