package li.cil.oc

import cpw.mods.fml.common.Mod

// This empty mod is used to avoid cyclic dependencies with mods we depend on
// for optional interfaces, for example, but that also depend on our API.
@Mod(modid = "OpenComputers|Core", name = "OpenComputers (Core)", version = "1.0.0", modLanguage = "scala")
object OpenComputersCore
