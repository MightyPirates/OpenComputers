package li.cil.oc.client

import cpw.mods.fml.client.registry.{RenderingRegistry, ClientRegistry}
import cpw.mods.fml.common.event.{FMLPostInitializationEvent, FMLInitializationEvent}
import cpw.mods.fml.common.network.NetworkRegistry
import cpw.mods.fml.common.registry.TickRegistry
import cpw.mods.fml.relauncher.Side
import li.cil.oc.client.renderer.WirelessNetworkDebugRenderer
import li.cil.oc.client.renderer.block.BlockRenderer
import li.cil.oc.client.renderer.item.UpgradeRenderer
import li.cil.oc.client.renderer.tileentity._
import li.cil.oc.common.tileentity
import li.cil.oc.common.{Proxy => CommonProxy}
import li.cil.oc.{Items, Settings, OpenComputers}
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.ReloadableResourceManager
import net.minecraftforge.client.MinecraftForgeClient
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
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.Rack], RackRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.RobotProxy], RobotRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.Screen], ScreenRenderer)

    MinecraftForgeClient.registerItemRenderer(Items.multi.itemID, UpgradeRenderer)

    MinecraftForge.EVENT_BUS.register(gui.Icons)

    Minecraft.getMinecraft.getResourceManager match {
      case manager: ReloadableResourceManager =>
        manager.registerReloadListener(TexturePreloader)
      case _ =>
    }
  }

  override def postInit(e: FMLPostInitializationEvent) {
    super.postInit(e)

    TickRegistry.registerTickHandler(ScreenRenderer, Side.CLIENT)
    if (Settings.get.rTreeDebugRenderer) {
      MinecraftForge.EVENT_BUS.register(WirelessNetworkDebugRenderer)
    }
  }
}