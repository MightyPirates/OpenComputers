package li.cil.oc.server

import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.network.NetworkRegistry
import li.cil.oc.OpenComputers
import li.cil.oc.common.{Proxy => CommonProxy}

private[oc] class Proxy extends CommonProxy {
  override def init(e: FMLInitializationEvent) {
    super.init(e)

    NetworkRegistry.INSTANCE.registerGuiHandler(OpenComputers, GuiHandler)
  }
}
