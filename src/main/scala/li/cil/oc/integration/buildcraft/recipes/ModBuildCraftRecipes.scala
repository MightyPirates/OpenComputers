package li.cil.oc.integration.buildcraft.recipes

import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods

object ModBuildCraftRecipes extends ModProxy {
  override def getMod = Mods.BuildCraftTiles

  override def initialize(): Unit = {
    LootDiskProgrammableRecipe.register()
  }
}
