package li.cil.oc.integration.gregtech

import cpw.mods.fml.common.event.FMLInterModComms
import li.cil.oc.api.Driver
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods
import net.minecraftforge.common.MinecraftForge

object ModGregtech extends ModProxy {
  override def getMod = Mods.GregTech

  override def initialize() {
    FMLInterModComms.sendMessage(Mods.IDs.OpenComputers, "registerToolDurabilityProvider", "li.cil.oc.integration.gregtech.EventHandlerGregTech.getDurability")

    MinecraftForge.EVENT_BUS.register(EventHandlerGregTech)

    Driver.add(new DriverEnergyContainer)

    RecipeHandler.init()
  }
}