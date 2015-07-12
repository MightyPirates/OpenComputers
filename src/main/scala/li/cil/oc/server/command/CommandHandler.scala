package li.cil.oc.server.command

import cpw.mods.fml.common.event.FMLServerStartingEvent

object CommandHandler {
  def register(e: FMLServerStartingEvent) {
    e.registerServerCommand(NonDisassemblyAgreementCommand)
    e.registerServerCommand(WirelessRenderingCommand)
    e.registerServerCommand(SpawnComputerCommand)
  }
}
