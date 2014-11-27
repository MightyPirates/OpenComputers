package li.cil.oc.integration.railcraft

import cpw.mods.fml.common.event.FMLInterModComms
import li.cil.oc.api.Driver
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods

object ModRailcraft extends ModProxy {
  override def getMod = Mods.Railcraft

  override def initialize() {
    FMLInterModComms.sendMessage(Mods.IDs.OpenComputers, "registerWrenchTool", "li.cil.oc.integration.railcraft.EventHandlerRailcraft.useWrench")

    Driver.add(new DriverBoilerFirebox)
    Driver.add(new DriverSteamTurbine)
  }
}