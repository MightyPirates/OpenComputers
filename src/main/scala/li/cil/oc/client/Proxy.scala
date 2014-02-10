package li.cil.oc.client

import cpw.mods.fml.client.registry.{RenderingRegistry, ClientRegistry}
import cpw.mods.fml.common.event.{FMLPreInitializationEvent, FMLPostInitializationEvent, FMLInitializationEvent}
import cpw.mods.fml.common.network.NetworkRegistry
import li.cil.oc.client
import li.cil.oc.client.renderer.WirelessNetworkDebugRenderer
import li.cil.oc.client.renderer.block.BlockRenderer
import li.cil.oc.client.renderer.item.UpgradeRenderer
import li.cil.oc.client.renderer.tileentity._
import li.cil.oc.common.tileentity
import li.cil.oc.common.{Proxy => CommonProxy}
import li.cil.oc.{Items, Settings, OpenComputers}
import net.minecraftforge.client.MinecraftForgeClient
import net.minecraftforge.common.MinecraftForge
import cpw.mods.fml.common.FMLCommonHandler

private[oc] class Proxy extends CommonProxy {
  override def preInit(e: FMLPreInitializationEvent) {
    super.preInit(e)

    MinecraftForge.EVENT_BUS.register(gui.Icons)
  }

  override def init(e: FMLInitializationEvent) {
    super.init(e)

    NetworkRegistry.INSTANCE.registerGuiHandler(OpenComputers, GuiHandler)

    BlockRenderer.getRenderId = RenderingRegistry.getNextAvailableRenderId
    RenderingRegistry.registerBlockHandler(BlockRenderer)

    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.Cable], CableRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.Case], CaseRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.PowerDistributor], PowerDistributorRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.Rack], RackRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.RobotProxy], RobotRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.Screen], ScreenRenderer)

    MinecraftForgeClient.registerItemRenderer(Items.multi, UpgradeRenderer)

    OpenComputers.channel.register(client.PacketHandler)
  }

  override def postInit(e: FMLPostInitializationEvent) {
    super.postInit(e)

    FMLCommonHandler.instance().bus().register(ScreenRenderer)
    if (Settings.get.rTreeDebugRenderer) {
      MinecraftForge.EVENT_BUS.register(WirelessNetworkDebugRenderer)
    }
  }
}