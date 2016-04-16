package li.cil.oc.integration.mcmp

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.client.renderer.block.ModelInitialization
import mcmultipart.item.PartPlacementWrapper
import mcmultipart.multipart.MultipartRegistry
import net.minecraft.client.resources.model.IBakedModel
import net.minecraft.client.resources.model.ModelResourceLocation
import net.minecraft.util.RegistrySimple
import net.minecraftforge.client.event.ModelBakeEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority

object MCMultiPart {
  final val CableMultipartLocation = new ModelResourceLocation(Settings.resourceDomain + ":" + Constants.BlockName.Cable, "multipart")
  final val PrintMultipartLocation = new ModelResourceLocation(Settings.resourceDomain + ":" + Constants.BlockName.Print, "multipart")

  def init(): Unit = {
    MultipartRegistry.registerPart(classOf[PartCable], PartFactory.PartTypeCable)
    MultipartRegistry.registerPart(classOf[PartPrint], PartFactory.PartTypePrint)
    MultipartRegistry.registerPartFactory(PartFactory, PartFactory.PartTypeCable)
    MultipartRegistry.registerPartConverter(PartConverter)
    MultipartRegistry.registerReversePartConverter(PartConverter)

    new PartPlacementWrapper(api.Items.get(Constants.BlockName.Cable).createItemStack(1), PartFactory).register(PartFactory.PartTypeCable)
    new PartPlacementWrapper(api.Items.get(Constants.BlockName.Print).createItemStack(1), PartFactory).register(PartFactory.PartTypePrint)

    MinecraftForge.EVENT_BUS.register(this)
  }

  @SubscribeEvent(priority = EventPriority.LOW)
  def onModelBake(e: ModelBakeEvent): Unit = {
    val registry = e.modelRegistry.asInstanceOf[RegistrySimple[ModelResourceLocation, IBakedModel]]

    // Replace default cable model with part model to properly handle connection
    // rendering to multipart cables.
    registry.putObject(ModelInitialization.CableBlockLocation, PartCableModel)
    registry.putObject(CableMultipartLocation, PartCableModel)
    registry.putObject(PrintMultipartLocation, PartPrintModel)
  }
}
