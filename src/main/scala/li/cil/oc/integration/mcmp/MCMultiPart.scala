package li.cil.oc.integration.mcmp

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.api
import mcmultipart.item.PartPlacementWrapper
import mcmultipart.multipart.MultipartRegistry
import net.minecraft.client.renderer.block.model.ModelResourceLocation
import net.minecraftforge.fml.common.FMLCommonHandler
import net.minecraftforge.fml.relauncher.Side

object MCMultiPart {
  final val CableMultipartRawLocation = Settings.resourceDomain + ":" + Constants.BlockName.Cable
  final val PrintMultipartRawLocation = Settings.resourceDomain + ":" + Constants.BlockName.Print

  def init(): Unit = {
    MultipartRegistry.registerPart(classOf[PartCable], PartFactory.PartTypeCable.toString)
    MultipartRegistry.registerPart(classOf[PartPrint], PartFactory.PartTypePrint.toString)
    MultipartRegistry.registerPartFactory(PartFactory, PartFactory.PartTypeCable.toString, PartFactory.PartTypePrint.toString)
    MultipartRegistry.registerPartConverter(PartConverter)
    MultipartRegistry.registerReversePartConverter(PartConverter)

    new PartPlacementWrapper(api.Items.get(Constants.BlockName.Cable).createItemStack(1), PartFactory).register(PartFactory.PartTypeCable.toString)
    new PartPlacementWrapper(api.Items.get(Constants.BlockName.Print).createItemStack(1), PartFactory).register(PartFactory.PartTypePrint.toString)

    if (FMLCommonHandler.instance.getSide == Side.CLIENT) {
      MCMultiPartClient.init()
    }
  }
}
