package li.cil.oc.integration.mcmp

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.api
import mcmultipart.capabilities.CapabilityWrapperRegistry
import mcmultipart.item.PartPlacementWrapper
import mcmultipart.multipart.MultipartRegistry
import net.minecraft.client.resources.model.IBakedModel
import net.minecraft.client.resources.model.ModelResourceLocation
import net.minecraft.util.RegistrySimple
import net.minecraftforge.client.event.ModelBakeEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object MCMultiPart {
  final val CableMultipartLocation = Settings.resourceDomain + ":" + "multipart_" + Constants.BlockName.Cable
  final val CableMultipartVariantLocation = new ModelResourceLocation(CableMultipartLocation, "multipart")

  def init(): Unit = {
    MultipartRegistry.registerPart(classOf[PartCable], PartProvider.PartTypeCable)
    MultipartRegistry.registerPartFactory(PartProvider, PartProvider.PartTypeCable)
    MultipartRegistry.registerPartConverter(PartConverter)
    MultipartRegistry.registerReversePartConverter(PartConverter)

    CapabilityWrapperRegistry.registerCapabilityWrapper(WrapperColored)
    CapabilityWrapperRegistry.registerCapabilityWrapper(WrapperEnvironment)
    CapabilityWrapperRegistry.registerCapabilityWrapper(WrapperSidedEnvironment)

    val placementWrapper = new PartPlacementWrapper(api.Items.get(Constants.BlockName.Cable).createItemStack(1), PartProvider)
    placementWrapper.register(PartProvider.PartTypeCable)

    MinecraftForge.EVENT_BUS.register(this)
  }

  @SubscribeEvent
  def onModelBake(e: ModelBakeEvent): Unit = {
    val registry = e.modelRegistry.asInstanceOf[RegistrySimple[ModelResourceLocation, IBakedModel]]

    registry.putObject(CableMultipartVariantLocation, PartCableModel)
  }
}
