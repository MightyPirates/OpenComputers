package li.cil.oc.client

import cpw.mods.fml.client.registry.{ClientRegistry, RenderingRegistry}
import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.common.event.{FMLInitializationEvent, FMLPostInitializationEvent, FMLPreInitializationEvent}
import cpw.mods.fml.common.network.NetworkRegistry
import li.cil.oc.client.renderer.WirelessNetworkDebugRenderer
import li.cil.oc.client.renderer.block.BlockRenderer
import li.cil.oc.client.renderer.item.ItemRenderer
import li.cil.oc.client.renderer.tileentity._
import li.cil.oc.common.component.TextBuffer
import li.cil.oc.common.tileentity.Rack
import li.cil.oc.common.{tileentity, Proxy => CommonProxy}
import li.cil.oc.{Items, OpenComputers, client}
import net.minecraftforge.client.MinecraftForgeClient
import net.minecraftforge.common.MinecraftForge

private[oc] class Proxy extends CommonProxy {
  override def preInit(e: FMLPreInitializationEvent) {
    super.preInit(e)

    MinecraftForge.EVENT_BUS.register(Sound)
    MinecraftForge.EVENT_BUS.register(gui.Icons)
  }

  override def init(e: FMLInitializationEvent) {
    super.init(e)

    NetworkRegistry.INSTANCE.registerGuiHandler(OpenComputers, GuiHandler)

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

    MinecraftForgeClient.registerItemRenderer(Items.multi, ItemRenderer)

    OpenComputers.channel.register(client.PacketHandler)

    ClientRegistry.registerKeyBinding(KeyBindings.extendedTooltip)
    ClientRegistry.registerKeyBinding(KeyBindings.materialCosts)
    ClientRegistry.registerKeyBinding(KeyBindings.clipboardPaste)
  }

  override def postInit(e: FMLPostInitializationEvent) {
    super.postInit(e)

    FMLCommonHandler.instance().bus().register(HologramRenderer)
    FMLCommonHandler.instance().bus().register(ScreenRenderer)
    MinecraftForge.EVENT_BUS.register(WirelessNetworkDebugRenderer)
    MinecraftForge.EVENT_BUS.register(Rack)
    MinecraftForge.EVENT_BUS.register(TextBuffer)
  }
}