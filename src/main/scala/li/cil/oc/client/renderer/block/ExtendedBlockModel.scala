package li.cil.oc.client.renderer.block

import li.cil.oc.OpenComputers
import li.cil.oc.api
import li.cil.oc.Settings
import net.minecraft.block.state.IBlockState
import net.minecraft.client.resources.model.ModelResourceLocation
import net.minecraft.util.RegistrySimple
import net.minecraftforge.client.event.ModelBakeEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

import scala.collection.convert.WrapAsScala._

object ExtendedBlockModel {
  @SubscribeEvent
  def onModelBake(e: ModelBakeEvent): Unit = {
    val registry = e.modelRegistry.asInstanceOf[RegistrySimple]
    val models = Array(
      "^" + Settings.resourceDomain + ":screen\\d#.*" -> ScreenModel
    )

    registry.putObject(new ModelResourceLocation(Settings.resourceDomain + ":cable", null), CableModel)
    registry.putObject(new ModelResourceLocation(Settings.resourceDomain + ":cable", "inventory"), CableModel)

    registry.getKeys.collect {
      case location: ModelResourceLocation =>
        models.find(entry => location.toString.matches(entry._1)) match {
          case Some((_, model)) => registry.putObject(location, model)
          case _ =>
        }
    }
  }
}
