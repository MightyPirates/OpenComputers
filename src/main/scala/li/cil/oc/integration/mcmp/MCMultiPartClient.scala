package li.cil.oc.integration.mcmp

import li.cil.oc.client.renderer.block.ModelInitialization
import net.minecraft.client.resources.model.IBakedModel
import net.minecraft.client.resources.model.ModelResourceLocation
import net.minecraft.util.RegistrySimple
import net.minecraftforge.client.event.ModelBakeEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object MCMultiPartClient {
  def init(): Unit = {
    MinecraftForge.EVENT_BUS.register(this)
  }

  @SubscribeEvent(priority = EventPriority.LOW)
  def onModelBake(e: ModelBakeEvent): Unit = {
    val registry = e.modelRegistry.asInstanceOf[RegistrySimple[ModelResourceLocation, IBakedModel]]

    // Replace default cable model with part model to properly handle connection
    // rendering to multipart cables.
    registry.putObject(ModelInitialization.CableBlockLocation, PartCableModel)
    registry.putObject(MCMultiPart.CableMultipartLocation, PartCableModel)
    registry.putObject(MCMultiPart.PrintMultipartLocation, PartPrintModel)
  }
}
