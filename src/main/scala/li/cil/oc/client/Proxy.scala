package li.cil.oc.client

import li.cil.oc._
import li.cil.oc.client.renderer.HighlightRenderer
import li.cil.oc.client.renderer.PetRenderer
import li.cil.oc.client.renderer.TextBufferRenderCache
import li.cil.oc.client.renderer.WirelessNetworkDebugRenderer
import li.cil.oc.client.renderer.block.ModelInitialization
import li.cil.oc.client.renderer.entity.DroneRenderer
import li.cil.oc.client.renderer.tileentity._
import li.cil.oc.common.component.TextBuffer
import li.cil.oc.common.entity.Drone
import li.cil.oc.common.item.Delegate
import li.cil.oc.common.tileentity
import li.cil.oc.common.tileentity.ServerRack
import li.cil.oc.common.{Proxy => CommonProxy}
import li.cil.oc.util.Audio
import net.minecraft.block.Block
import net.minecraft.item.Item
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.client.registry.ClientRegistry
import net.minecraftforge.fml.client.registry.RenderingRegistry
import net.minecraftforge.fml.common.FMLCommonHandler
import net.minecraftforge.fml.common.Loader
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import net.minecraftforge.fml.common.network.NetworkRegistry
import org.lwjgl.opengl.GLContext

private[oc] class Proxy extends CommonProxy {
  override def preInit(e: FMLPreInitializationEvent) {
    if (Loader.isModLoaded("OpenComponents")) {
      throw new OpenComponentsPresentException()
    }

    super.preInit(e)

    MinecraftForge.EVENT_BUS.register(Sound)
    MinecraftForge.EVENT_BUS.register(Textures)
    MinecraftForge.EVENT_BUS.register(HighlightRenderer)

    ModelInitialization.preInit()
  }

  override def init(e: FMLInitializationEvent) {
    super.init(e)

    OpenComputers.channel.register(client.PacketHandler)

    ModelInitialization.init()

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
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.Raid], RaidRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.ServerRack], ServerRackRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.Switch], SwitchRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.AccessPoint], SwitchRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.RobotProxy], RobotRenderer)
    ClientRegistry.bindTileEntitySpecialRenderer(classOf[tileentity.Screen], ScreenRenderer)

    // TODO labels on floppies and such
    //    MinecraftForgeClient.registerItemRenderer(Items.multi, ItemRenderer)

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

  override def registerModel(instance: Delegate, id: String) = ModelInitialization.registerModel(instance, id)

  override def registerModel(instance: Item, id: String) = ModelInitialization.registerModel(instance, id)

  override def registerModel(instance: Block, id: String) = ModelInitialization.registerModel(instance, id)
}
