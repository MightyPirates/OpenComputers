package li.cil.oc.client

import cpw.mods.fml.client.registry.{KeyBindingRegistry, RenderingRegistry, ClientRegistry}
import cpw.mods.fml.common.event.{FMLPreInitializationEvent, FMLPostInitializationEvent, FMLInitializationEvent}
import cpw.mods.fml.common.network.NetworkRegistry
import cpw.mods.fml.common.registry.TickRegistry
import cpw.mods.fml.relauncher.Side
import li.cil.oc.client.renderer.block.BlockRenderer
import li.cil.oc.client.renderer.item.ItemRenderer
import li.cil.oc.client.renderer.tileentity._
import li.cil.oc.client.renderer.{TextBufferRenderCache, WirelessNetworkDebugRenderer}
import li.cil.oc.common.{Proxy => CommonProxy, tileentity}
import li.cil.oc.common.component.TextBuffer
import li.cil.oc.common.tileentity.Rack
import li.cil.oc.{Items, OpenComputers}
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.ReloadableResourceManager
import net.minecraftforge.client.MinecraftForgeClient
import net.minecraftforge.common.MinecraftForge

private[oc] class Proxy extends CommonProxy {
  override def preInit(e: FMLPreInitializationEvent) {
    super.preInit(e)

    MinecraftForge.EVENT_BUS.register(Sound)
  }

  override def init(e: FMLInitializationEvent) = {
    super.init(e)

    NetworkRegistry.instance.registerGuiHandler(OpenComputers, GuiHandler)

    BlockRenderer.getRenderId = RenderingRegistry.getNextAvailableRenderId
    RenderingRegistry.registerBlockHandler(BlockRenderer)

    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.Cable], CableRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.Case], CaseRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.Charger], ChargerRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.Disassembler], DisassemblerRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.Geolyzer], GeolyzerRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.Hologram], HologramRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.PowerDistributor], PowerDistributorRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.Rack], RackRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.RobotAssembler], RobotAssemblerRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.Router], RouterRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.WirelessRouter], RouterRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.RobotProxy], RobotRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.Screen], ScreenRenderer)

    MinecraftForgeClient.registerItemRenderer(Items.multi.itemID, ItemRenderer)

    MinecraftForge.EVENT_BUS.register(gui.Icons)

    Minecraft.getMinecraft.getResourceManager match {
      case manager: ReloadableResourceManager =>
        manager.registerReloadListener(Textures)
      case _ =>
    }

    KeyBindingRegistry.registerKeyBinding(KeyBindings.Handler)
  }

  override def postInit(e: FMLPostInitializationEvent) {
    super.postInit(e)

    TickRegistry.registerTickHandler(HologramRenderer, Side.CLIENT)
    TickRegistry.registerTickHandler(TextBufferRenderCache, Side.CLIENT)
    MinecraftForge.EVENT_BUS.register(WirelessNetworkDebugRenderer)
    MinecraftForge.EVENT_BUS.register(Rack)
    MinecraftForge.EVENT_BUS.register(TextBuffer)
  }
}