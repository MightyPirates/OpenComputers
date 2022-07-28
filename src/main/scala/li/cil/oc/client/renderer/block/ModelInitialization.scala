package li.cil.oc.client.renderer.block

import java.util.Random

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.common.item.CustomModel
import li.cil.oc.common.item.Delegator
import li.cil.oc.common.item.traits.Delegate
import net.minecraft.block.BlockState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.BlockModelShapes
import net.minecraft.client.renderer.model.IBakedModel
import net.minecraft.client.renderer.model.ItemOverrideList
import net.minecraft.client.renderer.model.ModelResourceLocation
import net.minecraft.client.world.ClientWorld
import net.minecraft.entity.LivingEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.Direction
import net.minecraft.util.IItemProvider
import net.minecraft.util.ResourceLocation
import net.minecraftforge.client.event.{ModelBakeEvent, ModelRegistryEvent}
import net.minecraftforge.client.model.ModelLoader
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.eventbus.api.SubscribeEvent

import scala.collection.convert.ImplicitConversionsToScala._
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
  private val delegatorOverrides = mutable.Map.empty[Delegator, ItemStack => ModelResourceLocation]
  private val modelRemappings = mutable.Map.empty[ModelResourceLocation, ModelResourceLocation]

  def preInit(): Unit = {
    MinecraftForge.EVENT_BUS.register(this)

    registerModel(Constants.BlockName.Cable, CableBlockLocation, CableItemLocation)
    registerModel(Constants.BlockName.NetSplitter, NetSplitterBlockLocation, NetSplitterItemLocation)
    registerModel(Constants.BlockName.Print, PrintBlockLocation, PrintItemLocation)
    registerModel(Constants.BlockName.Robot, RobotBlockLocation, RobotItemLocation)
    registerModel(Constants.BlockName.RobotAfterimage, RobotAfterimageBlockLocation, RobotAfterimageItemLocation)
  }

  @SubscribeEvent
  def onModelRegistration(event: ModelRegistryEvent): Unit = {
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

  def registerModel(instance: IItemProvider, id: String): Unit = {
    meshableItems += instance.asItem
  }

  // ----------------------------------------------------------------------- //

  private def registerModel(blockName: String, blockLocation: ModelResourceLocation, itemLocation: ModelResourceLocation): Unit = {
    val descriptor = api.Items.get(blockName)
    val block = descriptor.block()
    val stack = descriptor.createItemStack(1)

    val shaper = Minecraft.getInstance.getItemRenderer.getItemModelShaper
    shaper.register(stack.getItem, itemLocation)
    block.getStateDefinition.getPossibleStates.foreach {
      modelRemappings += BlockModelShapes.stateToModelLocation(_) -> blockLocation
    }
  }

  private def registerItems(): Unit = {
    val shaper = Minecraft.getInstance.getItemRenderer.getItemModelShaper
    for (item <- meshableItems) {
      Option(api.Items.get(new ItemStack(item))) match {
        case Some(descriptor) =>
          val location = Settings.resourceDomain + ":" + descriptor.name()
          shaper.register(item, new ModelResourceLocation(location, "inventory"))
        case _ =>
      }
    }
    meshableItems.clear()
  }

  private def registerSubItems(): Unit = {
    for ((id, item) <- itemDelegates) {
      val location = Settings.resourceDomain + ":" + id
      delegatorOverrides.get(item.parent) match {
        case Some(func) => delegatorOverrides.put(item.parent, stack => {
            if (stack.getDamageValue != item.itemId) func(stack)
            else new ModelResourceLocation(location, "inventory")
          })
        case _ => delegatorOverrides.put(item.parent, stack => {
            if (stack.getDamageValue != item.itemId) null
            else new ModelResourceLocation(location, "inventory")
          })
      }
    }
  }

  private def registerSubItemsCustom(): Unit = {
    for (item <- itemDelegatesCustom) {
      delegatorOverrides.get(item.parent) match {
        case Some(func) => delegatorOverrides.put(item.parent, stack => {
            Delegator.subItem(stack) match {
              case Some(subItem: CustomModel) => subItem.getModelLocation(stack)
              case _ => func(stack)
            }
          })
        case _ => delegatorOverrides.put(item.parent, stack => {
            Delegator.subItem(stack) match {
              case Some(subItem: CustomModel) => subItem.getModelLocation(stack)
              case _ => null
            }
          })
      }
      item.registerModelLocations()
    }
  }

  // ----------------------------------------------------------------------- //

  @SubscribeEvent
  def onModelBake(e: ModelBakeEvent): Unit = {
    val registry = e.getModelRegistry

    registry.put(CableBlockLocation, CableModel)
    registry.put(CableItemLocation, CableModel)
    registry.put(NetSplitterBlockLocation, NetSplitterModel)
    registry.put(NetSplitterItemLocation, NetSplitterModel)
    registry.put(PrintBlockLocation, PrintModel)
    registry.put(PrintItemLocation, PrintModel)
    registry.put(RobotBlockLocation, RobotModel)
    registry.put(RobotItemLocation, RobotModel)
    registry.put(RobotAfterimageBlockLocation, NullModel)
    registry.put(RobotAfterimageItemLocation, NullModel)

    for ((id, item) <- itemDelegates) {
      val location = Settings.resourceDomain + ":" + id
      ModelLoader.addSpecialModel(new ResourceLocation(location))
    }
    for (item <- itemDelegatesCustom) {
      item.bakeModels(e)
    }

    val modelOverrides = Map[String, IBakedModel => IBakedModel](
      Constants.BlockName.ScreenTier1 -> (_ => ScreenModel),
      Constants.BlockName.ScreenTier2 -> (_ => ScreenModel),
      Constants.BlockName.ScreenTier3 -> (_ => ScreenModel),
      Constants.BlockName.Rack -> (parent => new ServerRackModel(parent))
    )

    registry.keySet.toArray.foreach {
      case location: ModelResourceLocation => {
        var parent = registry.get(location)
        for ((name, model) <- modelOverrides) {
          val pattern = s"^${Settings.resourceDomain}:$name#.*"
          if (location.toString.matches(pattern)) {
            parent = model(parent)
            registry.put(location, parent)
          }
        }
        modelRemappings.get(location) match {
          case Some(remapped) => registry.put(remapped, parent)
          case _ =>
        }
      }
      case _ =>
    }

    // Temporary hack to apply model overrides from registerModel calls.
    for ((delegator, handler) <- delegatorOverrides) {
      val originalLocation = new ModelResourceLocation(delegator.getRegistryName, "inventory")
      registry.get(originalLocation) match {
        case original: IBakedModel => {
          val overrides = new ItemOverrideList {
            override def resolve(base: IBakedModel, stack: ItemStack, world: ClientWorld, holder: LivingEntity) =
              Option(handler(stack)).map(registry).getOrElse(original.getOverrides.resolve(base, stack, world, holder))
          }
          val fake = new IBakedModel {
            @Deprecated
            override def getQuads(state: BlockState, dir: Direction, rand: Random) = original.getQuads(state, dir, rand)

            override def useAmbientOcclusion() = original.useAmbientOcclusion

            override def isGui3d() = original.isGui3d

            override def usesBlockLight() = original.usesBlockLight

            override def isCustomRenderer() = original.isCustomRenderer

            @Deprecated
            override def getParticleIcon() = original.getParticleIcon

            @Deprecated
            override def getTransforms() = original.getTransforms

            override def getOverrides() = overrides
          }
          registry.put(originalLocation, fake)
        }
        case _ =>
      }
    }
  }
}
