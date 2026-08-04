package li.cil.oc.integration.forestry

import cpw.mods.fml.common.registry.GameRegistry
import li.cil.oc.Constants
import li.cil.oc.api.Driver
import li.cil.oc.common.recipe.Recipes
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods

object ModForestry extends ModProxy {
  override def getMod = Mods.Forestry

  override def initialize() {
    Driver.add(new ConverterIAlleles)
    Driver.add(new ConverterIIndividual)
    Driver.add(ConverterItemStack)
    Driver.add(new DriverAnalyzer)
    Driver.add(new DriverBeeHouse)
    Driver.add(DriverUpgradeBeekeeper)
    Driver.add(DriverUpgradeBeekeeper.Provider)
    val multi = new li.cil.oc.common.item.Delegator()
    GameRegistry.registerItem(multi, "item.forestry")
    Recipes.addSubItem(new li.cil.oc.integration.forestry.item.UpgradeBeekeeper(multi), Constants.ItemName.BeekeeperUpgrade, "oc:beekeeperUpgrade")
  }
}