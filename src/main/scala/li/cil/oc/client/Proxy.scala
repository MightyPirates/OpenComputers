package li.cil.oc.client

import com.mojang.blaze3d.systems.IRenderCall
import com.mojang.blaze3d.systems.RenderSystem
import li.cil.oc.OpenComputers
import li.cil.oc.api
import li.cil.oc.client
import li.cil.oc.client.gui.GuiTypes
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

private[oc] class Proxy extends CommonProxy {
  modBus.register(classOf[GuiTypes])
  modBus.register(ModelInitialization)
  modBus.register(NetSplitterModel)
  modBus.register(Textures)

  override def preInit() {
    super.preInit()

    api.API.manual = client.Manual
  }

  override def init(e: FMLCommonSetupEvent) {
    super.init(e)

    CommonPacketHandler.clientHandler = PacketHandler

    e.enqueueWork((() => {
      ModelInitialization.preInit()

      ColorHandler.init()

      RenderingRegistry.registerEntityRenderingHandler(EntityTypes.DRONE, DroneRenderer)

      ClientRegistry.bindTileEntityRenderer(tileentity.TileEntityTypes.ADAPTER, AdapterRenderer)
      ClientRegistry.bindTileEntityRenderer(tileentity.TileEntityTypes.ASSEMBLER, AssemblerRenderer)
      ClientRegistry.bindTileEntityRenderer(tileentity.TileEntityTypes.CASE, CaseRenderer)
      ClientRegistry.bindTileEntityRenderer(tileentity.TileEntityTypes.CHARGER, ChargerRenderer)
      ClientRegistry.bindTileEntityRenderer(tileentity.TileEntityTypes.DISASSEMBLER, DisassemblerRenderer)
      ClientRegistry.bindTileEntityRenderer(tileentity.TileEntityTypes.DISK_DRIVE, DiskDriveRenderer)
      ClientRegistry.bindTileEntityRenderer(tileentity.TileEntityTypes.GEOLYZER, GeolyzerRenderer)
      ClientRegistry.bindTileEntityRenderer(tileentity.TileEntityTypes.HOLOGRAM, HologramRenderer)
      ClientRegistry.bindTileEntityRenderer(tileentity.TileEntityTypes.MICROCONTROLLER, MicrocontrollerRenderer)
      ClientRegistry.bindTileEntityRenderer(tileentity.TileEntityTypes.NET_SPLITTER, NetSplitterRenderer)
      ClientRegistry.bindTileEntityRenderer(tileentity.TileEntityTypes.POWER_DISTRIBUTOR, PowerDistributorRenderer)
      ClientRegistry.bindTileEntityRenderer(tileentity.TileEntityTypes.PRINTER, PrinterRenderer)
      ClientRegistry.bindTileEntityRenderer(tileentity.TileEntityTypes.RAID, RaidRenderer)
      ClientRegistry.bindTileEntityRenderer(tileentity.TileEntityTypes.RACK, RackRenderer)
      ClientRegistry.bindTileEntityRenderer(tileentity.TileEntityTypes.RELAY, RelayRenderer)
      ClientRegistry.bindTileEntityRenderer(tileentity.TileEntityTypes.ROBOT, RobotRenderer)
      ClientRegistry.bindTileEntityRenderer(tileentity.TileEntityTypes.SCREEN, ScreenRenderer)
      ClientRegistry.bindTileEntityRenderer(tileentity.TileEntityTypes.TRANSPOSER, TransposerRenderer)

      ClientRegistry.registerKeyBinding(KeyBindings.extendedTooltip)
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
    }): Runnable)

    runOnRenderThread(() => MinecraftForge.EVENT_BUS.register(TextBufferRenderCache))
  }

  def runOnRenderThread(call: IRenderCall) {
    if (RenderSystem.isOnRenderThreadOrInit) call.execute()
    else RenderSystem.recordRenderCall(call)
  }

  override def registerModel(instance: Item, id: String): Unit = ModelInitialization.registerModel(instance, id)

  override def registerModel(instance: Block, id: String): Unit = ModelInitialization.registerModel(instance, id)
}
