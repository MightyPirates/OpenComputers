package li.cil.oc.client.renderer.block

import java.util.Random

import li.cil.oc.Constants
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.common.item.CustomModel
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
import net.minecraft.util.IItemProvider
import net.minecraft.util.Direction
import net.minecraftforge.client.event.{ModelBakeEvent, ModelRegistryEvent}
import net.minecraftforge.client.model.data.IDynamicBakedModel
import net.minecraftforge.client.model.data.IModelData
import net.minecraftforge.eventbus.api.SubscribeEvent

import scala.collection.convert.ImplicitConversionsToScala._
import scala.collection.mutable

object ModelInitialization {
  final val CableBlockLocation = new ModelResourceLocation(Settings.resourceDomain + ":" + Constants.BlockName.Cable, "")
  final val CableItemLocation = new ModelResourceLocation(Settings.resourceDomain + ":" + Constants.BlockName.Cable, "inventory")
  final val NetSplitterBlockLocation = new ModelResourceLocation(Settings.resourceDomain + ":" + Constants.BlockName.NetSplitter, "")
  final val NetSplitterItemLocation = new ModelResourceLocation(Settings.resourceDomain + ":" + Constants.BlockName.NetSplitter, "inventory")
  final val PrintBlockLocation = new ModelResourceLocation(Settings.resourceDomain + ":" + Constants.BlockName.Print, "")
  final val PrintItemLocation = new ModelResourceLocation(Settings.resourceDomain + ":" + Constants.BlockName.Print, "inventory")
  final val RobotBlockLocation = new ModelResourceLocation(Settings.resourceDomain + ":" + Constants.BlockName.Robot, "")
  final val RobotItemLocation = new ModelResourceLocation(Settings.resourceDomain + ":" + Constants.BlockName.Robot, "inventory")
  final val RobotAfterimageBlockLocation = new ModelResourceLocation(Settings.resourceDomain + ":" + Constants.BlockName.RobotAfterimage, "")
  final val RackBlockLocation = new ModelResourceLocation(Settings.resourceDomain + ":" + Constants.BlockName.Rack, "")

  private val meshableItems = mutable.ArrayBuffer.empty[Item]
  private val modelRemappings = mutable.Map.empty[ModelResourceLocation, ModelResourceLocation]

  def preInit(): Unit = {
    registerModel(Constants.BlockName.Cable, CableBlockLocation, CableItemLocation)
    registerModel(Constants.BlockName.NetSplitter, NetSplitterBlockLocation, NetSplitterItemLocation)
    registerModel(Constants.BlockName.Print, PrintBlockLocation, PrintItemLocation)
    registerModel(Constants.BlockName.Robot, RobotBlockLocation, RobotItemLocation)
    registerModel(Constants.BlockName.RobotAfterimage, RobotAfterimageBlockLocation, null)
  }

  @SubscribeEvent
  def onModelRegistration(event: ModelRegistryEvent): Unit = {
    val shaper = Minecraft.getInstance.getItemRenderer.getItemModelShaper
    for (item <- meshableItems) {
      item match {
        case custom: CustomModel => custom.registerModelLocations()
        case _ => {
          Option(api.Items.get(new ItemStack(item))) match {
            case Some(descriptor) =>
              val location = Settings.resourceDomain + ":" + descriptor.name()
              shaper.register(item, new ModelResourceLocation(location, "inventory"))
            case _ =>
          }
        }
      }
    }
  }

  // ----------------------------------------------------------------------- //

  def registerModel(instance: IItemProvider, id: String): Unit = {
    meshableItems += instance.asItem
  }

  // ----------------------------------------------------------------------- //

  private def registerModel(blockName: String, blockLocation: ModelResourceLocation, itemLocation: ModelResourceLocation): Unit = {
    val descriptor = api.Items.get(blockName)
    val block = descriptor.block()
    val stack = descriptor.createItemStack(1)

    if (!stack.isEmpty) {
      val shaper = Minecraft.getInstance.getItemRenderer.getItemModelShaper
      shaper.register(stack.getItem, itemLocation)
    }
    block.getStateDefinition.getPossibleStates.foreach {
      modelRemappings += BlockModelShapes.stateToModelLocation(_) -> blockLocation
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

    for (item <- meshableItems) item match {
      case custom: CustomModel => {
        custom.bakeModels(e)
        val originalLocation = new ModelResourceLocation(custom.getRegistryName, "inventory")
        registry.get(originalLocation) match {
          case original: IBakedModel => {
            val overrides = new ItemOverrideList {
              override def resolve(base: IBakedModel, stack: ItemStack, world: ClientWorld, holder: LivingEntity) =
                Option(custom.getModelLocation(stack)).map(registry).getOrElse(original)
            }
            val fake = new IDynamicBakedModel {
              @Deprecated
              override def getQuads(state: BlockState, dir: Direction, rand: Random, data: IModelData) = original.getQuads(state, dir, rand, data)
        
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
      case _ =>
    }
    meshableItems.clear()

    val modelOverrides = Map[String, IBakedModel => IBakedModel](
      Constants.BlockName.ScreenTier1 -> (_ => ScreenModel),
      Constants.BlockName.ScreenTier2 -> (_ => ScreenModel),
      Constants.BlockName.ScreenTier3 -> (_ => ScreenModel),
      Constants.BlockName.Rack -> (parent => new ServerRackModel(parent))
    )

    registry.keySet.toArray.foreach {
      case location: ModelResourceLocation => {
        for ((name, model) <- modelOverrides) {
          val pattern = s"^${Settings.resourceDomain}:$name#.*"
          if (location.toString.matches(pattern)) {
            registry.put(location, model(registry.get(location)))
          }
        }
      }
      case _ =>
    }
    for ((real, virtual) <- modelRemappings) {
      registry.put(real, registry.get(virtual))
    }
  }
}
