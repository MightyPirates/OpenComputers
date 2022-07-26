package li.cil.oc.client

import com.mojang.blaze3d.systems.IRenderCall
import com.mojang.blaze3d.systems.RenderSystem
import li.cil.oc.OpenComputers
import li.cil.oc.api
import li.cil.oc.client
import li.cil.oc.client.renderer.HighlightRenderer
import li.cil.oc.client.renderer.MFUTargetRenderer
import li.cil.oc.client.renderer.PetRenderer
import li.cil.oc.client.renderer.TextBufferRenderCache
import li.cil.oc.client.renderer.WirelessNetworkDebugRenderer
import li.cil.oc.client.renderer.block.ModelInitialization
import li.cil.oc.client.renderer.block.NetSplitterModel
import li.cil.oc.client.renderer.entity.DroneRenderer
import li.cil.oc.client.renderer.tileentity._
import li.cil.oc.common
import li.cil.oc.common.{PacketHandler => CommonPacketHandler}
import li.cil.oc.common.{Proxy => CommonProxy}
import li.cil.oc.common.component.TextBuffer
import li.cil.oc.common.entity.Drone
import li.cil.oc.common.entity.EntityTypes
import li.cil.oc.common.event.NanomachinesHandler
import li.cil.oc.common.event.RackMountableRenderHandler
import li.cil.oc.common.item.traits.Delegate
import li.cil.oc.common.tileentity
import li.cil.oc.util.Audio
import net.minecraft.block.Block
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.player.ClientPlayerEntity
import net.minecraft.client.gui.screen.Screen
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.world.World
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.client.registry.ClientRegistry
import net.minecraftforge.fml.client.registry.RenderingRegistry
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent
import net.minecraftforge.fml.network.NetworkRegistry

@Deprecated
private[oc] class Proxy extends CommonProxy {
  override def preInit() {
    super.preInit()

    api.API.manual = client.Manual

    MinecraftForge.EVENT_BUS.register(Textures)
    MinecraftForge.EVENT_BUS.register(NetSplitterModel)
  }

  override def init(e: FMLCommonSetupEvent) {
    super.init(e)

    ModelInitialization.preInit()

    CommonPacketHandler.clientHandler = PacketHandler

    ColorHandler.init()

    RenderingRegistry.registerEntityRenderingHandler(EntityTypes.DRONE, DroneRenderer)

    ClientRegistry.bindTileEntityRenderer(null, AdapterRenderer) // TEMP
    ClientRegistry.bindTileEntityRenderer(null, AssemblerRenderer) // TEMP
    ClientRegistry.bindTileEntityRenderer(null, CaseRenderer) // TEMP
    ClientRegistry.bindTileEntityRenderer(null, ChargerRenderer) // TEMP
    ClientRegistry.bindTileEntityRenderer(null, DisassemblerRenderer) // TEMP
    ClientRegistry.bindTileEntityRenderer(null, DiskDriveRenderer) // TEMP
    ClientRegistry.bindTileEntityRenderer(null, GeolyzerRenderer) // TEMP
    ClientRegistry.bindTileEntityRenderer(null, HologramRenderer) // TEMP
    ClientRegistry.bindTileEntityRenderer(null, MicrocontrollerRenderer) // TEMP
    ClientRegistry.bindTileEntityRenderer(null, NetSplitterRenderer) // TEMP
    ClientRegistry.bindTileEntityRenderer(null, PowerDistributorRenderer) // TEMP
    ClientRegistry.bindTileEntityRenderer(null, PrinterRenderer) // TEMP
    ClientRegistry.bindTileEntityRenderer(null, RaidRenderer) // TEMP
    ClientRegistry.bindTileEntityRenderer(null, RackRenderer) // TEMP
    ClientRegistry.bindTileEntityRenderer(null, RelayRenderer) // TEMP
    ClientRegistry.bindTileEntityRenderer(null, RobotRenderer) // TEMP
    ClientRegistry.bindTileEntityRenderer(null, ScreenRenderer) // TEMP
    ClientRegistry.bindTileEntityRenderer(null, TransposerRenderer) // TEMP

    ClientRegistry.registerKeyBinding(KeyBindings.analyzeCopyAddr)
    ClientRegistry.registerKeyBinding(KeyBindings.clipboardPaste)

    MinecraftForge.EVENT_BUS.register(HighlightRenderer)
    MinecraftForge.EVENT_BUS.register(NanomachinesHandler.Client)
    MinecraftForge.EVENT_BUS.register(PetRenderer)
    MinecraftForge.EVENT_BUS.register(RackMountableRenderHandler)
    MinecraftForge.EVENT_BUS.register(Sound)
    MinecraftForge.EVENT_BUS.register(TextBuffer)
    MinecraftForge.EVENT_BUS.register(MFUTargetRenderer)
    MinecraftForge.EVENT_BUS.register(WirelessNetworkDebugRenderer)
    MinecraftForge.EVENT_BUS.register(Audio)
    MinecraftForge.EVENT_BUS.register(HologramRenderer)

    runOnRenderThread(() => MinecraftForge.EVENT_BUS.register(TextBufferRenderCache))
  }

  def runOnRenderThread(call: IRenderCall) {
    if (RenderSystem.isOnRenderThreadOrInit) call.execute()
    else RenderSystem.recordRenderCall(call)
  }

  override def getGuiHandler(): common.GuiHandler = client.GuiHandler

  @Deprecated
  override def openGui(player: PlayerEntity, guiId: Int, world: World, x: Int, y: Int, z: Int): Unit = {
    player match {
      case _: ClientPlayerEntity => {
        val screen = getGuiHandler.getClientGuiElement(guiId, 0, player, world, x, y, z).asInstanceOf[Screen]
        if (screen != null) Minecraft.getInstance.pushGuiLayer(screen)
      }
      case _ => super.openGui(player, guiId, world, x, y, z)
    }
  }

  override def registerModel(instance: Delegate, id: String): Unit = ModelInitialization.registerModel(instance, id)

  override def registerModel(instance: Item, id: String): Unit = ModelInitialization.registerModel(instance, id)

  override def registerModel(instance: Block, id: String): Unit = ModelInitialization.registerModel(instance, id)
}
