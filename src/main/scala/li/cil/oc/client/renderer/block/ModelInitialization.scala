package li.cil.oc.client.renderer.block

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.common.item.CustomModel
import li.cil.oc.common.item.Delegate
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.ItemMeshDefinition
import net.minecraft.client.renderer.block.statemap.StateMapperBase
import net.minecraft.client.resources.model.ModelBakery
import net.minecraft.client.resources.model.ModelResourceLocation
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.RegistrySimple
import net.minecraftforge.client.event.ModelBakeEvent
import net.minecraftforge.client.model.IFlexibleBakedModel
import net.minecraftforge.client.model.ISmartBlockModel
import net.minecraftforge.client.model.ModelLoader
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

import scala.collection.convert.WrapAsScala._
import scala.collection.mutable

object ModelInitialization {
  final val CableBlockLocation = new ModelResourceLocation(Settings.resourceDomain + ":" + Constants.BlockName.Cable, "normal")
  final val CableItemLocation = new ModelResourceLocation(Settings.resourceDomain + ":" + Constants.BlockName.Cable, "inventory")
  final val RobotBlockLocation = new ModelResourceLocation(Settings.resourceDomain + ":" + Constants.BlockName.Robot, "normal")
  final val RobotItemLocation = new ModelResourceLocation(Settings.resourceDomain + ":" + Constants.BlockName.Robot, "inventory")
  final val RobotAfterimageBlockLocation = new ModelResourceLocation(Settings.resourceDomain + ":" + Constants.BlockName.RobotAfterimage, "normal")
  final val RobotAfterimageItemLocation = new ModelResourceLocation(Settings.resourceDomain + ":" + Constants.BlockName.RobotAfterimage, "inventory")
  final val RackBlockLocation = new ModelResourceLocation(Settings.resourceDomain + ":" + Constants.BlockName.ServerRack, "normal")

  private val meshableItems = mutable.ArrayBuffer.empty[Item]
  private val itemDelegates = mutable.ArrayBuffer.empty[(String, Delegate)]
  private val itemDelegatesCustom = mutable.ArrayBuffer.empty[Delegate with CustomModel]

  def preInit(): Unit = {
    MinecraftForge.EVENT_BUS.register(this)

    registerModel(Constants.BlockName.Cable, CableBlockLocation, CableItemLocation)
    registerModel(Constants.BlockName.Robot, RobotBlockLocation, RobotItemLocation)
    registerModel(Constants.BlockName.RobotAfterimage, RobotAfterimageBlockLocation, RobotAfterimageItemLocation)
  }

  def init(): Unit = {
    registerItems(meshableItems)
    registerSubItems(itemDelegates)
    registerSubItemsCustom(itemDelegatesCustom)
  }

  // ----------------------------------------------------------------------- //

  def registerModel(instance: Delegate, id: String): Unit = {
    instance match {
      case customModel: CustomModel => itemDelegatesCustom += customModel
      case _ => itemDelegates += id -> instance
    }
  }

  def registerModel(instance: Item, id: String): Unit = {
    meshableItems += instance
  }

  def registerModel(instance: Block, id: String): Unit = {
    val item = Item.getItemFromBlock(instance)
    registerModel(item, id)
  }

  // ----------------------------------------------------------------------- //

  private def registerModel(blockName: String, blockLocation: ModelResourceLocation, itemLocation: ModelResourceLocation): Unit = {
    val descriptor = api.Items.get(blockName)
    val block = descriptor.block()
    val stack = descriptor.createItemStack(1)

    ModelLoader.setCustomModelResourceLocation(stack.getItem, stack.getMetadata, itemLocation)
    ModelLoader.setCustomStateMapper(block, new StateMapperBase {
      override def getModelResourceLocation(state: IBlockState) = blockLocation
    })
  }

  private def registerItems(items: mutable.Buffer[Item]): Unit = {
    val meshDefinition = new ItemMeshDefinition {
      override def getModelLocation(stack: ItemStack) = {
        Option(api.Items.get(stack)) match {
          case Some(descriptor) =>
            val location = Settings.resourceDomain + ":" + descriptor.name()
            new ModelResourceLocation(location, "inventory")
          case _ => null
        }
      }
    }

    val modelMeshes = Minecraft.getMinecraft.getRenderItem.getItemModelMesher
    for (item <- items) {
      modelMeshes.register(item, meshDefinition)
    }
    items.clear()
  }

  private def registerSubItems(items: mutable.Buffer[(String, Delegate)]): Unit = {
    val modelMeshes = Minecraft.getMinecraft.getRenderItem.getItemModelMesher
    for ((id, item) <- items) {
      val location = Settings.resourceDomain + ":" + id
      modelMeshes.register(item.parent, item.itemId, new ModelResourceLocation(location, "inventory"))
      ModelBakery.addVariantName(item.parent, location)
    }
    items.clear()
  }

  private def registerSubItemsCustom(items: mutable.Buffer[Delegate with CustomModel]): Unit = {
    val modelMeshes = Minecraft.getMinecraft.getRenderItem.getItemModelMesher
    for (item <- items) {
      modelMeshes.register(item.parent, new ItemMeshDefinition {
        override def getModelLocation(stack: ItemStack): ModelResourceLocation = item.getModelLocation(stack)
      })
      item.registerModelLocations()
    }
    items.clear()
  }

  // ----------------------------------------------------------------------- //

  @SubscribeEvent
  def onModelBake(e: ModelBakeEvent): Unit = {
    val registry = e.modelRegistry.asInstanceOf[RegistrySimple]

    registry.putObject(CableBlockLocation, CableModel)
    registry.putObject(CableItemLocation, CableModel)
    registry.putObject(RobotBlockLocation, RobotModel)
    registry.putObject(RobotItemLocation, RobotModel)
    registry.putObject(RobotAfterimageBlockLocation, NullModel)
    registry.putObject(RobotAfterimageItemLocation, NullModel)

    val modelOverrides = Map[String, IFlexibleBakedModel => ISmartBlockModel](
      Constants.BlockName.ScreenTier1 -> (_ => ScreenModel),
      Constants.BlockName.ScreenTier2 -> (_ => ScreenModel),
      Constants.BlockName.ScreenTier3 -> (_ => ScreenModel),
      Constants.BlockName.ServerRack -> (parent => new ServerRackModel(parent))
    )

    registry.getKeys.collect {
      case location: ModelResourceLocation => registry.getObject(location) match {
        case parent: IFlexibleBakedModel =>
          for ((name, model) <- modelOverrides) {
            val pattern = s"^${Settings.resourceDomain}:$name#.*"
            if (location.toString.matches(pattern)) {
              registry.putObject(location, model(parent))
            }
          }
        case _ =>
      }
    }
  }
}
