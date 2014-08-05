package li.cil.oc.client

import cpw.mods.fml.client.registry.{ClientRegistry, RenderingRegistry}
import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.common.event.{FMLInitializationEvent, FMLPreInitializationEvent}
import cpw.mods.fml.common.network.NetworkRegistry
import li.cil.oc.client.renderer.block.BlockRenderer
import li.cil.oc.client.renderer.item.ItemRenderer
import li.cil.oc.client.renderer.tileentity._
import li.cil.oc.client.renderer.{PetRenderer, TextBufferRenderCache, WirelessNetworkDebugRenderer}
import li.cil.oc.common.component.TextBuffer
import li.cil.oc.common.tileentity.ServerRack
import li.cil.oc.common.{tileentity, Proxy => CommonProxy}
import li.cil.oc.util.Audio
import li.cil.oc.{Items, OpenComputers, Settings, client}
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

    OpenComputers.channel.register(client.PacketHandler)

    Settings.blockRenderId = RenderingRegistry.getNextAvailableRenderId
    RenderingRegistry.registerBlockHandler(BlockRenderer)

    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.Case], CaseRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.Charger], ChargerRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.Disassembler], DisassemblerRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.DiskDrive], DiskDriveRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.Geolyzer], GeolyzerRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.Hologram], HologramRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.PowerDistributor], PowerDistributorRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.ServerRack], ServerRackRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.RobotAssembler], RobotAssemblerRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.Switch], SwitchRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.AccessPoint], SwitchRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.RobotProxy], RobotRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.Screen], ScreenRenderer)

    MinecraftForgeClient.registerItemRenderer(Items.multi, ItemRenderer)

    ClientRegistry.registerKeyBinding(KeyBindings.extendedTooltip)
    ClientRegistry.registerKeyBinding(KeyBindings.materialCosts)
    ClientRegistry.registerKeyBinding(KeyBindings.clipboardPaste)

    MinecraftForge.EVENT_BUS.register(PetRenderer)
    MinecraftForge.EVENT_BUS.register(ServerRack)
    MinecraftForge.EVENT_BUS.register(TextBuffer)
    MinecraftForge.EVENT_BUS.register(WirelessNetworkDebugRenderer)

    NetworkRegistry.INSTANCE.registerGuiHandler(OpenComputers, GuiHandler)

    FMLCommonHandler.instance.bus.register(Audio)
    FMLCommonHandler.instance.bus.register(HologramRenderer)
    FMLCommonHandler.instance.bus.register(PetRenderer)
    FMLCommonHandler.instance.bus.register(TextBufferRenderCache)
  }
}