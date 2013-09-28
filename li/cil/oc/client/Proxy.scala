package li.cil.oc.client

import cpw.mods.fml.client.registry.ClientRegistry
import cpw.mods.fml.common.event.FMLInitializationEvent
import li.cil.oc.common.tileentity.TileEntityComputer
import li.cil.oc.common.tileentity.TileEntityScreen
import li.cil.oc.common.{Proxy => CommonProxy}

private[oc] class Proxy extends CommonProxy {
  override def init(e: FMLInitializationEvent) = {
    super.init(e)

    ClientRegistry.bindTileEntitySpecialRenderer(classOf[TileEntityScreen], ScreenRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[TileEntityComputer], ComputerRenderer)
  }
}