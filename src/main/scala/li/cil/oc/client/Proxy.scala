package li.cil.oc.client

import li.cil.oc._
import li.cil.oc.client.renderer.PetRenderer
import li.cil.oc.client.renderer.TextBufferRenderCache
import li.cil.oc.client.renderer.WirelessNetworkDebugRenderer
import li.cil.oc.client.renderer.block.ExtendedBlockModel
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
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.ItemMeshDefinition
import net.minecraft.client.resources.model.ModelBakery
import net.minecraft.client.resources.model.ModelResourceLocation
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.client.registry.ClientRegistry
import net.minecraftforge.fml.client.registry.RenderingRegistry
import net.minecraftforge.fml.common.FMLCommonHandler
import net.minecraftforge.fml.common.Loader
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import net.minecraftforge.fml.common.network.NetworkRegistry
import org.lwjgl.opengl.GLContext

import scala.collection.mutable

private[oc] class Proxy extends CommonProxy {
  private val meshableItems = mutable.ArrayBuffer.empty[Item]
  private val itemDelegates = mutable.ArrayBuffer.empty[(String, Delegate)]

  override def preInit(e: FMLPreInitializationEvent) {
    if (Loader.isModLoaded("OpenComponents")) {
      throw new OpenComponentsPresentException()
    }

    super.preInit(e)

    MinecraftForge.EVENT_BUS.register(ExtendedBlockModel)
    MinecraftForge.EVENT_BUS.register(Sound)
    MinecraftForge.EVENT_BUS.register(Textures)
  }

  override def registerModel(instance: Delegate, id: String): Unit = {
    itemDelegates += id -> instance
  }

  override def registerModel(instance: Item, id: String): Unit = {
    meshableItems += instance
  }

  override def registerModel(instance: Block, id: String): Unit = {
    val item = Item.getItemFromBlock(instance)
    registerModel(item, id)
  }

  override def init(e: FMLInitializationEvent) {
    super.init(e)

    OpenComputers.channel.register(client.PacketHandler)

    val mesher = Minecraft.getMinecraft.getRenderItem.getItemModelMesher
    val meshDefinition = new ItemMeshDefinition {
      override def getModelLocation(stack: ItemStack) = {
        Option(api.Items.get(stack)) match {
          case Some(descriptor) => new ModelResourceLocation(Settings.resourceDomain + ":" + descriptor.name(), "inventory")
          case _ => null
        }
      }
    }
    for (item <- meshableItems) {
      mesher.register(item, meshDefinition)
    }
    meshableItems.clear()
    for ((id, item) <- itemDelegates) {
      val location = Settings.resourceDomain + ":" + id
      mesher.register(item.parent, item.itemId, new ModelResourceLocation(location, "inventory"))
      ModelBakery.addVariantName(item.parent, location)
    }
    itemDelegates.clear()

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
}
