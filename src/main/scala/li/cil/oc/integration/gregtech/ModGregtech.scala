package li.cil.oc.integration.gregtech

import li.cil.oc.api
import li.cil.oc.api.Driver
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods
import net.minecraftforge.common.MinecraftForge

object ModGregtech extends ModProxy {
  override def getMod = Mods.GregTech

  override def initialize() {
    api.IMC.registerToolDurabilityProvider("li.cil.oc.integration.gregtech.EventHandlerGregTech.getDurability")

    MinecraftForge.EVENT_BUS.register(EventHandlerGregTech)

    Driver.add(new DriverEnergyContainer)

    RecipeHandler.init()
  }
}