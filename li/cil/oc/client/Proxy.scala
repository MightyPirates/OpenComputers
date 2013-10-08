package li.cil.oc.client

import cpw.mods.fml.client.registry.ClientRegistry
import cpw.mods.fml.common.event.FMLInitializationEvent
import cpw.mods.fml.common.network.NetworkRegistry
import cpw.mods.fml.common.registry.TickRegistry
import cpw.mods.fml.relauncher.Side
import li.cil.oc.OpenComputers
import li.cil.oc.common.tileentity.Computer
import li.cil.oc.common.tileentity.Screen
import li.cil.oc.common.{Proxy => CommonProxy}

private[oc] class Proxy extends CommonProxy {
  override def init(e: FMLInitializationEvent) = {
    super.init(e)

    NetworkRegistry.instance.registerGuiHandler(OpenComputers, GuiHandler)

    ClientRegistry.bindTileEntitySpecialRenderer(classOf[Screen], ScreenRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[Computer], ComputerRenderer)

    TickRegistry.registerTickHandler(ScreenRenderer, Side.CLIENT)
  }
}