package li.cil.oc.client

import cpw.mods.fml.client.registry.ClientRegistry
import cpw.mods.fml.client.registry.RenderingRegistry
import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.common.event.FMLInitializationEvent
import cpw.mods.fml.common.event.FMLPreInitializationEvent
import cpw.mods.fml.common.network.NetworkRegistry
import li.cil.oc.Constants
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.client
import li.cil.oc.client.renderer.HighlightRenderer
import li.cil.oc.client.renderer.PetRenderer
import li.cil.oc.client.renderer.TextBufferRenderCache
import li.cil.oc.client.renderer.WirelessNetworkDebugRenderer
import li.cil.oc.client.renderer.block.BlockRenderer
import li.cil.oc.client.renderer.entity.DroneRenderer
import li.cil.oc.client.renderer.item.ItemRenderer
import li.cil.oc.client.renderer.tileentity._
import li.cil.oc.common.component.TextBuffer
import li.cil.oc.common.entity.Drone
import li.cil.oc.common.init.Items
import li.cil.oc.common.tileentity
import li.cil.oc.common.tileentity.ServerRack
import li.cil.oc.common.{Proxy => CommonProxy}
import li.cil.oc.util.Audio
import net.minecraftforge.client.MinecraftForgeClient
import net.minecraftforge.common.MinecraftForge
import org.lwjgl.opengl.GLContext

private[oc] class Proxy extends CommonProxy {
  override def preInit(e: FMLPreInitializationEvent) {
    super.preInit(e)

    MinecraftForge.EVENT_BUS.register(Sound)
    MinecraftForge.EVENT_BUS.register(gui.Icons)
    MinecraftForge.EVENT_BUS.register(HighlightRenderer)
  }

  override def init(e: FMLInitializationEvent) {
    super.init(e)

    OpenComputers.channel.register(client.PacketHandler)

    Settings.blockRenderId = RenderingRegistry.getNextAvailableRenderId
    RenderingRegistry.registerBlockHandler(BlockRenderer)
    RenderingRegistry.registerEntityRenderingHandler(classOf[Drone], DroneRenderer)

    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.Assembler], AssemblerRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.Case], CaseRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.Charger], ChargerRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.Disassembler], DisassemblerRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.DiskDrive], DiskDriveRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.Geolyzer], GeolyzerRenderer)
    if (GLContext.getCapabilities.OpenGL15)
      ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.Hologram], HologramRenderer)
    else
      ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.Hologram], HologramRendererFallback)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.Microcontroller], MicrocontrollerRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.PowerDistributor], PowerDistributorRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.Printer], PrinterRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.Raid], RaidRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.ServerRack], ServerRackRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.Switch], SwitchRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.AccessPoint], SwitchRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.RobotProxy], RobotRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.Screen], ScreenRenderer)

    MinecraftForgeClient.registerItemRenderer(Items.get(Constants.ItemName.Floppy).createItemStack(1).getItem, ItemRenderer)
    MinecraftForgeClient.registerItemRenderer(Items.get(Constants.BlockName.Print).createItemStack(1).getItem, ItemRenderer)

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
