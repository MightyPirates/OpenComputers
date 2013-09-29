package li.cil.oc.client

import cpw.mods.fml.client.registry.ClientRegistry
import cpw.mods.fml.common.event.FMLInitializationEvent
import li.cil.oc.common.tileentity.Computer
import li.cil.oc.common.tileentity.Screen
import li.cil.oc.common.{Proxy => CommonProxy}

private[oc] class Proxy extends CommonProxy {
  override def init(e: FMLInitializationEvent) = {
    super.init(e)

    ClientRegistry.bindTileEntitySpecialRenderer(classOf[Screen], ScreenRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[Computer], ComputerRenderer)
  }
}