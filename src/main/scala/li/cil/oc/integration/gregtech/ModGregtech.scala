package li.cil.oc.integration.gregtech

import cpw.mods.fml.common.event.FMLInterModComms
import li.cil.oc.api.Driver
import li.cil.oc.integration.{ModProxy, Mods}
import net.minecraftforge.common.MinecraftForge

object ModGregtech extends ModProxy {
  override def getMod = Mods.GregTech

  override def initialize() {
    FMLInterModComms.sendMessage("OpenComputers", "registerToolDurabilityProvider", "li.cil.oc.integration.gregtech.EventHandlerGregTech.getDurability")

    MinecraftForge.EVENT_BUS.register(EventHandlerGregTech)

    Driver.add(new DriverEnergyContainer)
  }
}