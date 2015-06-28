package li.cil.oc.server.command

import cpw.mods.fml.common.event.FMLServerStartingEvent

object CommandHandler {
  def register(e: FMLServerStartingEvent) {
    e.registerServerCommand(WirelessRenderingCommand)
    e.registerServerCommand(NonDisassemblyAgreementCommand)
  }
}
