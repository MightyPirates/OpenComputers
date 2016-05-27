package li.cil.oc.integration.mcmp

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.api
import mcmultipart.item.PartPlacementWrapper
import mcmultipart.multipart.MultipartRegistry
import net.minecraft.client.resources.model.ModelResourceLocation
import net.minecraftforge.fml.common.FMLCommonHandler
import net.minecraftforge.fml.relauncher.Side

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

    if (FMLCommonHandler.instance.getSide == Side.CLIENT) {
      MCMultiPartClient.init()
    }
  }
}
