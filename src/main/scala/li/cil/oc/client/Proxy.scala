package li.cil.oc.client

import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.client
import li.cil.oc.client.renderer.PetRenderer
import li.cil.oc.client.renderer.TextBufferRenderCache
import li.cil.oc.client.renderer.WirelessNetworkDebugRenderer
import li.cil.oc.client.renderer.block.ExtendedBlockModel
import li.cil.oc.client.renderer.entity.DroneRenderer
import li.cil.oc.client.renderer.item.ItemRenderer
import li.cil.oc.client.renderer.tileentity._
import li.cil.oc.common.block.traits.Extended
import li.cil.oc.common.component.TextBuffer
import li.cil.oc.common.entity.Drone
import li.cil.oc.common.init.Items
import li.cil.oc.common.block
import li.cil.oc.common.tileentity
import li.cil.oc.common.tileentity.ServerRack
import li.cil.oc.common.{Proxy => CommonProxy}
import li.cil.oc.util.Audio
import net.minecraft.block.Block
import net.minecraft.block.properties.IProperty
import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.ItemMeshDefinition
import net.minecraft.client.renderer.block.statemap.IStateMapper
import net.minecraft.client.resources.model.ModelBakery
import net.minecraft.client.resources.model.ModelResourceLocation
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraftforge.client.MinecraftForgeClient
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.common.property.ExtendedBlockState
import net.minecraftforge.common.property.IExtendedBlockState
import net.minecraftforge.common.property.IUnlistedProperty
import net.minecraftforge.common.property.Properties
import net.minecraftforge.fml.client.registry.ClientRegistry
import net.minecraftforge.fml.client.registry.RenderingRegistry
import net.minecraftforge.fml.common.FMLCommonHandler
import net.minecraftforge.fml.common.Loader
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import net.minecraftforge.fml.common.network.NetworkRegistry
import org.lwjgl.opengl.GLContext

import scala.collection.mutable
import scala.collection.convert.WrapAsScala._
import scala.collection.convert.WrapAsJava._

private[oc] class Proxy extends CommonProxy {
  private val extendedBlocks = mutable.ArrayBuffer.empty[(Extended, String)]
  private val meshableItems = mutable.ArrayBuffer.empty[Item]

  override def preInit(e: FMLPreInitializationEvent) {
    if (Loader.isModLoaded("OpenComponents")) {
      throw new OpenComponentsPresentException()
    }

    super.preInit(e)

//    MinecraftForge.EVENT_BUS.register(ExtendedBlockModel)
    MinecraftForge.EVENT_BUS.register(Sound)
    MinecraftForge.EVENT_BUS.register(Textures)
  }

  override def registerModel(instance: Item): Unit = {
    meshableItems += instance
  }

  override def registerModel(instance: Block, id: String): Unit = {
    val item = Item.getItemFromBlock(instance)
    registerModel(item)
//    ExtendedBlockModel.registerBlock(instance)
    instance match {
      case extended: block.traits.Extended =>
        extendedBlocks += extended -> (Settings.resourceDomain + ":" + id)
//        val blockLocation = Settings.resourceDomain + ":" + id
//        Minecraft.getMinecraft.getRenderItem.getItemModelMesher.getModelManager.getBlockModelShapes.getBlockStateMapper.registerBlockStateMapper(extended, new IStateMapper {
//          override def putStateModelLocations(block: Block) = {
//            val state = extended.getDefaultState
//            val unlisted = extended.collectRawProperties()
//            val values = unlisted.map(property => property.getAllowedValues.collect {
//              case value: java.lang.Comparable[AnyRef]@unchecked => ((property, value), property.getName + "=" + property.getName(value))
//            })
//            values.foldLeft(Iterable((state, "")))((acc, value) => cross(acc, value)).map(variant => (variant._1, new ModelResourceLocation(blockLocation, variant._2.stripPrefix(",")))).toMap[AnyRef, AnyRef]
//          }
//        })
//
//

          //        val unlisted = extended.collectRawProperties()
//        val values = unlisted.map(property => property.getAllowedValues.collect {
//          case value: java.lang.Comparable[AnyRef]@unchecked => property.getName + "=" + property.getName(value)
//        })
//        val variants = values.foldLeft(Iterable(""))((acc, value) => cross(acc, value)).map(variant => id + "#" + variant.stripPrefix(",")).toSeq
//        ModelBakery.addVariantName(item, variants: _*)
      case _ =>
    }
  }

  private def cross(xs: Iterable[(IExtendedBlockState, String)], ys: Iterable[((IUnlistedProperty[_], Comparable[AnyRef]), String)]) =
    for { (state, stateString) <- xs; ((property, value), valueString) <- ys } yield (state.withProperty(property.asInstanceOf[IUnlistedProperty[Any]], value), stateString + "," + valueString)

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
    meshableItems.foreach(item => mesher.register(item, meshDefinition))
    meshableItems.clear()
    for((extended, blockLocation) <- extendedBlocks) {
      Minecraft.getMinecraft.getRenderItem.getItemModelMesher.getModelManager.getBlockModelShapes.getBlockStateMapper.registerBlockStateMapper(extended, new IStateMapper {
        override def putStateModelLocations(block: Block) = {
          val state = extended.getDefaultState.asInstanceOf[IExtendedBlockState]
          val unlisted = extended.collectRawProperties().toArray.sortBy(_._2.getName)
          val values = unlisted.map {
            case (property, propertyRaw) => propertyRaw.getAllowedValues.collect {
              case value: java.lang.Comparable[AnyRef]@unchecked => ((property, value), propertyRaw.getName + "=" + propertyRaw.getName(value))
            }
          }
          values.foldLeft(Iterable((state, "")))((acc, value) => cross(acc, value)).map(variant => (variant._1, new ModelResourceLocation(blockLocation, variant._2.stripPrefix(",")))).toMap[AnyRef, AnyRef]
        }
      })
    }
    extendedBlocks.clear()
//    ExtendedBlockModel.init()

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
