package li.cil.oc.client.renderer.block

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.api
import net.minecraft.block.state.IBlockState
import net.minecraft.client.renderer.block.statemap.StateMapperBase
import net.minecraft.client.resources.model.ModelResourceLocation
import net.minecraft.util.RegistrySimple
import net.minecraftforge.client.event.ModelBakeEvent
import net.minecraftforge.client.model.ModelLoader
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

import scala.collection.convert.WrapAsScala._

object ExtendedBlockModel {
  final val CableBlockLocation = new ModelResourceLocation(Settings.resourceDomain + ":" + Constants.BlockName.Cable, "normal")
  final val CableItemLocation = new ModelResourceLocation(Settings.resourceDomain + ":" + Constants.BlockName.Cable, "inventory")
  final val RobotBlockLocation = new ModelResourceLocation(Settings.resourceDomain + ":" + Constants.BlockName.Robot, "normal")
  final val RobotItemLocation = new ModelResourceLocation(Settings.resourceDomain + ":" + Constants.BlockName.Robot, "inventory")

  def preInit(): Unit = {
    MinecraftForge.EVENT_BUS.register(this)

    registerModel(Constants.BlockName.Cable, CableBlockLocation, CableItemLocation)
    registerModel(Constants.BlockName.Robot, RobotBlockLocation, RobotItemLocation)
  }

  private def registerModel(blockName: String, blockLocation: ModelResourceLocation, itemLocation: ModelResourceLocation): Unit = {
    val descriptor = api.Items.get(blockName)
    val block = descriptor.block()
    val stack = descriptor.createItemStack(1)

    ModelLoader.setCustomModelResourceLocation(stack.getItem, stack.getMetadata, itemLocation)
    ModelLoader.setCustomStateMapper(block, new StateMapperBase {
      override def getModelResourceLocation(state: IBlockState) = blockLocation
    })
  }

  @SubscribeEvent
  def onModelBake(e: ModelBakeEvent): Unit = {
    val registry = e.modelRegistry.asInstanceOf[RegistrySimple]

    registry.putObject(CableBlockLocation, CableModel)
    registry.putObject(CableItemLocation, CableModel)
    registry.putObject(RobotBlockLocation, RobotModel)
    registry.putObject(RobotItemLocation, RobotModel)

    registry.getKeys.collect {
      case location: ModelResourceLocation =>
        if (location.toString.matches("^" + Settings.resourceDomain + ":screen\\d#.*")) {
          registry.putObject(location, ScreenModel)
        }
    }
  }
}
