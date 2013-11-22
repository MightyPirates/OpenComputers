package li.cil.oc.client

import cpw.mods.fml.client.registry.{RenderingRegistry, ClientRegistry}
import cpw.mods.fml.common.event.{FMLPostInitializationEvent, FMLInitializationEvent}
import cpw.mods.fml.common.network.NetworkRegistry
import cpw.mods.fml.common.registry.TickRegistry
import cpw.mods.fml.relauncher.Side
import li.cil.oc.client.renderer.WirelessNetworkDebugRenderer
import li.cil.oc.client.renderer.block.BlockRenderer
import li.cil.oc.client.renderer.tileentity._
import li.cil.oc.common.tileentity
import li.cil.oc.common.{Proxy => CommonProxy}
import li.cil.oc.{Config, OpenComputers}
import net.minecraftforge.common.MinecraftForge

private[oc] class Proxy extends CommonProxy {
  override def init(e: FMLInitializationEvent) = {
    super.init(e)

    NetworkRegistry.instance.registerGuiHandler(OpenComputers, GuiHandler)

    BlockRenderer.getRenderId = RenderingRegistry.getNextAvailableRenderId
    RenderingRegistry.registerBlockHandler(BlockRenderer)

    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.Cable], CableRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.Case], CaseRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.PowerDistributor], PowerDistributorRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.RobotProxy], RobotRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.Screen], ScreenRenderer)

    TickRegistry.registerTickHandler(ScreenRenderer, Side.CLIENT)

    MinecraftForge.EVENT_BUS.register(gui.Icons)
  }

  override def postInit(e: FMLPostInitializationEvent) {
    super.postInit(e)

    if (Config.rTreeDebugRenderer) {
      MinecraftForge.EVENT_BUS.register(WirelessNetworkDebugRenderer)
    }
  }
}