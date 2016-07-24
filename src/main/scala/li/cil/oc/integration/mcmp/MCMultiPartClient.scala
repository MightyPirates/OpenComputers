package li.cil.oc.integration.mcmp

import li.cil.oc.client.renderer.block.ModelInitialization
import li.cil.oc.client.renderer.block.PrintModel
import net.minecraft.client.renderer.block.model.ModelResourceLocation
import net.minecraftforge.client.event.ModelBakeEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object MCMultiPartClient {
  final val CableMultipartLocation = new ModelResourceLocation(MCMultiPart.CableMultipartRawLocation, "multipart")
  final val PrintMultipartLocation = new ModelResourceLocation(MCMultiPart.PrintMultipartRawLocation, "multipart")

  def init(): Unit = {
    MinecraftForge.EVENT_BUS.register(this)
  }

  @SubscribeEvent(priority = EventPriority.LOW)
  def onModelBake(e: ModelBakeEvent): Unit = {
    val registry = e.getModelRegistry

    // Replace default cable model with part model to properly handle connection
    // rendering to multipart cables.
    registry.putObject(ModelInitialization.CableBlockLocation, PartCableModel)
    registry.putObject(CableMultipartLocation, PartCableModel)
    registry.putObject(PrintMultipartLocation, PrintModel)
  }
}
