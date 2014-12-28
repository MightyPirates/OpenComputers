package li.cil.oc.server

import li.cil.oc.OpenComputers
import li.cil.oc.common.{Proxy => CommonProxy}
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.network.NetworkRegistry

private[oc] class Proxy extends CommonProxy {
  override def init(e: FMLInitializationEvent) {
    super.init(e)

    NetworkRegistry.INSTANCE.registerGuiHandler(OpenComputers, GuiHandler)
  }
}
