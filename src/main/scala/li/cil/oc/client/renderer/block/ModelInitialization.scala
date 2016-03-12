package li.cil.oc.client.renderer.block

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.common.item.CustomModel
import li.cil.oc.common.item.Delegator
import li.cil.oc.common.item.traits.Delegate
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.ItemMeshDefinition
import net.minecraft.client.renderer.block.statemap.StateMapperBase
import net.minecraft.client.resources.model.IBakedModel
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
  final val NetSplitterBlockLocation = new ModelResourceLocation(Settings.resourceDomain + ":" + Constants.BlockName.NetSplitter, "normal")
  final val NetSplitterItemLocation = new ModelResourceLocation(Settings.resourceDomain + ":" + Constants.BlockName.NetSplitter, "inventory")
  final val PrintBlockLocation = new ModelResourceLocation(Settings.resourceDomain + ":" + Constants.BlockName.Print, "normal")
  final val PrintItemLocation = new ModelResourceLocation(Settings.resourceDomain + ":" + Constants.BlockName.Print, "inventory")
  final val RobotBlockLocation = new ModelResourceLocation(Settings.resourceDomain + ":" + Constants.BlockName.Robot, "normal")
  final val RobotItemLocation = new ModelResourceLocation(Settings.resourceDomain + ":" + Constants.BlockName.Robot, "inventory")
  final val RobotAfterimageBlockLocation = new ModelResourceLocation(Settings.resourceDomain + ":" + Constants.BlockName.RobotAfterimage, "normal")
  final val RobotAfterimageItemLocation = new ModelResourceLocation(Settings.resourceDomain + ":" + Constants.BlockName.RobotAfterimage, "inventory")
  final val RackBlockLocation = new ModelResourceLocation(Settings.resourceDomain + ":" + Constants.BlockName.Rack, "normal")

  private val meshableItems = mutable.ArrayBuffer.empty[Item]
  private val itemDelegates = mutable.ArrayBuffer.empty[(String, Delegate)]
  private val itemDelegatesCustom = mutable.ArrayBuffer.empty[Delegate with CustomModel]

  def preInit(): Unit = {
    MinecraftForge.EVENT_BUS.register(this)

    registerModel(Constants.BlockName.Cable, CableBlockLocation, CableItemLocation)
    registerModel(Constants.BlockName.NetSplitter, NetSplitterBlockLocation, NetSplitterItemLocation)
    registerModel(Constants.BlockName.Print, PrintBlockLocation, PrintItemLocation)
    registerModel(Constants.BlockName.Robot, RobotBlockLocation, RobotItemLocation)
    registerModel(Constants.BlockName.RobotAfterimage, RobotAfterimageBlockLocation, RobotAfterimageItemLocation)
  }

  def init(): Unit = {
    registerItems()
    registerSubItems()
    registerSubItemsCustom()
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

  private def registerItems(): Unit = {
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
    for (item <- meshableItems) {
      modelMeshes.register(item, meshDefinition)
    }
    meshableItems.clear()
  }

  private def registerSubItems(): Unit = {
    val modelMeshes = Minecraft.getMinecraft.getRenderItem.getItemModelMesher
    for ((id, item) <- itemDelegates) {
      val location = Settings.resourceDomain + ":" + id
      modelMeshes.register(item.parent, item.itemId, new ModelResourceLocation(location, "inventory"))
      ModelBakery.addVariantName(item.parent, location)
    }
    itemDelegates.clear()
  }

  private def registerSubItemsCustom(): Unit = {
    val modelMeshes = Minecraft.getMinecraft.getRenderItem.getItemModelMesher
    for (item <- itemDelegatesCustom) {
      modelMeshes.register(item.parent, new ItemMeshDefinition {
        override def getModelLocation(stack: ItemStack): ModelResourceLocation = Delegator.subItem(stack) match {
          case Some(subItem: CustomModel) => subItem.getModelLocation(stack)
          case _ => null
        }
      })
      item.registerModelLocations()
    }
  }

  // ----------------------------------------------------------------------- //

  @SubscribeEvent
  def onModelBake(e: ModelBakeEvent): Unit = {
    val registry = e.modelRegistry.asInstanceOf[RegistrySimple[ModelResourceLocation, IBakedModel]]

    registry.putObject(CableBlockLocation, CableModel)
    registry.putObject(CableItemLocation, CableModel)
    registry.putObject(NetSplitterBlockLocation, NetSplitterModel)
    registry.putObject(NetSplitterItemLocation, NetSplitterModel)
    registry.putObject(PrintBlockLocation, PrintModel)
    registry.putObject(PrintItemLocation, PrintModel)
    registry.putObject(RobotBlockLocation, RobotModel)
    registry.putObject(RobotItemLocation, RobotModel)
    registry.putObject(RobotAfterimageBlockLocation, NullModel)
    registry.putObject(RobotAfterimageItemLocation, NullModel)

    for (item <- itemDelegatesCustom) {
      item.bakeModels(e)
    }

    val modelOverrides = Map[String, IFlexibleBakedModel => ISmartBlockModel](
      Constants.BlockName.ScreenTier1 -> (_ => ScreenModel),
      Constants.BlockName.ScreenTier2 -> (_ => ScreenModel),
      Constants.BlockName.ScreenTier3 -> (_ => ScreenModel),
      Constants.BlockName.Rack -> (parent => new ServerRackModel(parent))
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
