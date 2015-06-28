package li.cil.oc.server.command

import net.minecraftforge.fml.common.event.FMLServerStartingEvent

object CommandHandler {
  def register(e: FMLServerStartingEvent) {
    e.registerServerCommand(WirelessRenderingCommand)
    e.registerServerCommand(NonDisassemblyAgreementCommand)
  }
}
