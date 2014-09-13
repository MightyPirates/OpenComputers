package li.cil.oc.client

import cpw.mods.fml.client.registry.{ClientRegistry, KeyBindingRegistry, RenderingRegistry}
import cpw.mods.fml.common.event.{FMLInitializationEvent, FMLPreInitializationEvent}
import cpw.mods.fml.common.network.NetworkRegistry
import cpw.mods.fml.common.registry.TickRegistry
import cpw.mods.fml.relauncher.Side
import li.cil.oc.client.gui.Icons
import li.cil.oc.client.renderer.block.BlockRenderer
import li.cil.oc.client.renderer.item.ItemRenderer
import li.cil.oc.client.renderer.tileentity._
import li.cil.oc.client.renderer.{PetRenderer, TextBufferRenderCache, WirelessNetworkDebugRenderer}
import li.cil.oc.common.component.TextBuffer
import li.cil.oc.common.tileentity.ServerRack
import li.cil.oc.common.{tileentity, Proxy => CommonProxy}
import li.cil.oc.util.Audio
import li.cil.oc.{Items, OpenComputers, Settings}
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

    MinecraftForgeClient.registerItemRenderer(Items.multi.itemID, ItemRenderer)

    KeyBindingRegistry.registerKeyBinding(KeyBindings.Handler)

    MinecraftForge.EVENT_BUS.register(Icons)
    MinecraftForge.EVENT_BUS.register(PetRenderer)
    MinecraftForge.EVENT_BUS.register(ServerRack)
    MinecraftForge.EVENT_BUS.register(TextBuffer)
    MinecraftForge.EVENT_BUS.register(WirelessNetworkDebugRenderer)

    Minecraft.getMinecraft.getResourceManager match {
      case manager: ReloadableResourceManager =>
        manager.registerReloadListener(Textures)
      case _ =>
    }

    NetworkRegistry.instance.registerGuiHandler(OpenComputers, GuiHandler)

    TickRegistry.registerTickHandler(Audio, Side.CLIENT)
    TickRegistry.registerTickHandler(HologramRenderer, Side.CLIENT)
    TickRegistry.registerTickHandler(PetRenderer, Side.CLIENT)
    TickRegistry.registerTickHandler(TextBufferRenderCache, Side.CLIENT)
  }
}