package li.cil.oc.integration.mcmp

import li.cil.oc.client.renderer.block.PrintModel
import li.cil.oc.client.renderer.block.SmartBlockModelBase
import mcmultipart.client.multipart.ISmartMultipartModel
import net.minecraft.block.state.IBlockState
import net.minecraft.client.resources.model.IBakedModel
import net.minecraft.item.ItemStack
import net.minecraftforge.common.property.IExtendedBlockState

object PartPrintModel extends SmartBlockModelBase with ISmartMultipartModel {
  override def handlePartState(state: IBlockState): IBakedModel = state match {
    case extended: IExtendedBlockState => new PrintModel.BlockModel(extended)
    case _ => missingModel
  }

  override def handleBlockState(state: IBlockState) = state match {
    case extended: IExtendedBlockState => new PrintModel.BlockModel(extended)
    case _ => missingModel
  }

  override def handleItemState(stack: ItemStack) = new PrintModel.ItemModel(stack)
}
