package li.cil.oc.client

import li.cil.oc.OpenComputers
import li.cil.oc.api
import li.cil.oc.client
import li.cil.oc.client.renderer.HighlightRenderer
import li.cil.oc.client.renderer.PetRenderer
import li.cil.oc.client.renderer.TextBufferRenderCache
import li.cil.oc.client.renderer.WirelessNetworkDebugRenderer
import li.cil.oc.client.renderer.block.ModelInitialization
import li.cil.oc.client.renderer.block.NetSplitterModel
import li.cil.oc.client.renderer.entity.DroneRenderer
import li.cil.oc.client.renderer.tileentity._
import li.cil.oc.common.component.TextBuffer
import li.cil.oc.common.entity.Drone
import li.cil.oc.common.event.NanomachinesHandler
import li.cil.oc.common.event.RackMountableRenderHandler
import li.cil.oc.common.item.traits.Delegate
import li.cil.oc.common.tileentity
import li.cil.oc.common.{Proxy => CommonProxy}
import li.cil.oc.util.Audio
import net.minecraft.block.Block
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.color.IBlockColor
import net.minecraft.client.renderer.color.IItemColor
import net.minecraft.item.Item
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.client.registry.ClientRegistry
import net.minecraftforge.fml.client.registry.RenderingRegistry
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import net.minecraftforge.fml.common.network.NetworkRegistry
import org.lwjgl.opengl.GLContext

import scala.collection.mutable

private[oc] class Proxy extends CommonProxy {
  override def preInit(e: FMLPreInitializationEvent) {
    super.preInit(e)

    api.API.manual = client.Manual

    CommandHandler.register()

    MinecraftForge.EVENT_BUS.register(Textures)
    MinecraftForge.EVENT_BUS.register(NetSplitterModel)

    ModelInitialization.preInit()
  }

  override def init(e: FMLInitializationEvent) {
    super.init(e)

    OpenComputers.channel.register(client.PacketHandler)

    ModelInitialization.init()
    coloredItems.foreach { case (colored, instance) => Minecraft.getMinecraft.getItemColors.registerItemColorHandler(colored, instance) }
    coloredBlocks.foreach { case (colored, instance) => Minecraft.getMinecraft.getBlockColors.registerBlockColorHandler(colored, instance) }

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
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.NetSplitter], NetSplitterRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.PowerDistributor], PowerDistributorRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.Printer], PrinterRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.Raid], RaidRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.Rack], RackRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.Switch], new SwitchRenderer[tileentity.Switch])
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.AccessPoint], new SwitchRenderer[tileentity.AccessPoint])
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.Relay], new SwitchRenderer[tileentity.Relay])
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.RobotProxy], RobotRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.Screen], ScreenRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.Transposer], TransposerRenderer)

    ClientRegistry.registerKeyBinding(KeyBindings.materialCosts)
    ClientRegistry.registerKeyBinding(KeyBindings.clipboardPaste)

    MinecraftForge.EVENT_BUS.register(HighlightRenderer)
    MinecraftForge.EVENT_BUS.register(NanomachinesHandler.Client)
    MinecraftForge.EVENT_BUS.register(PetRenderer)
    MinecraftForge.EVENT_BUS.register(RackMountableRenderHandler)
    MinecraftForge.EVENT_BUS.register(Sound)
    MinecraftForge.EVENT_BUS.register(TextBuffer)
    MinecraftForge.EVENT_BUS.register(WirelessNetworkDebugRenderer)

    NetworkRegistry.INSTANCE.registerGuiHandler(OpenComputers, GuiHandler)

    MinecraftForge.EVENT_BUS.register(Audio)
    MinecraftForge.EVENT_BUS.register(HologramRenderer)
    MinecraftForge.EVENT_BUS.register(PetRenderer)
    MinecraftForge.EVENT_BUS.register(Sound)
    MinecraftForge.EVENT_BUS.register(TextBufferRenderCache)
  }

  override def registerModel(instance: Delegate, id: String) = ModelInitialization.registerModel(instance, id)

  override def registerModel(instance: Item, id: String) = {
    ModelInitialization.registerModel(instance, id)
    instance match {
      case colored: IItemColor => coloredItems += colored -> instance
      case _ => // Nope.
    }
  }

  override def registerModel(instance: Block, id: String) = {
    ModelInitialization.registerModel(instance, id)
    instance match {
      case colored: IBlockColor => coloredBlocks += colored -> instance
      case _ => // Nope.
    }
  }

  val coloredItems = mutable.ArrayBuffer.empty[(IItemColor, Item)]
  val coloredBlocks = mutable.ArrayBuffer.empty[(IBlockColor, Block)]
}
