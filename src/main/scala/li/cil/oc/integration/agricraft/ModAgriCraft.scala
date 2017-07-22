package li.cil.oc.integration.agricraft

import li.cil.oc.api.Driver
import li.cil.oc.integration.Mod
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods
import net.minecraftforge.common.MinecraftForge

object ModAgriCraft extends ModProxy {
  override def getMod: Mod = Mods.AgriCraft

  override def initialize(): Unit = {
    Driver.add(ConverterSeeds)

    MinecraftForge.EVENT_BUS.register(EventHandlerAgriCraft)
  }
}
