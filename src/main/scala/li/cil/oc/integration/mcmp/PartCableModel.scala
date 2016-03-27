package li.cil.oc.integration.mcmp

import li.cil.oc.client.renderer.block.CableModel
import li.cil.oc.client.renderer.block.SmartBlockModelBase
import li.cil.oc.util.BlockPosition
import mcmultipart.client.multipart.ISmartMultipartModel
import mcmultipart.multipart.MultipartHelper
import mcmultipart.multipart.PartSlot
import net.minecraft.block.state.IBlockState
import net.minecraft.client.resources.model.IBakedModel
import net.minecraftforge.common.property.IExtendedBlockState

object PartCableModel extends SmartBlockModelBase with ISmartMultipartModel {
  override def handlePartState(state: IBlockState): IBakedModel = state match {
    case extended: IExtendedBlockState => new BlockModel(extended)
    case _ => missingModel
  }

  class BlockModel(state: IExtendedBlockState) extends CableModel.BlockModel(state) {
    override protected def isCable(pos: BlockPosition): Boolean = super.isCable(pos) || (pos.world match {
      case Some(world) =>
        val container = MultipartHelper.getPartContainer(world, pos.toBlockPos)
        (container != null) && (container.getPartInSlot(PartSlot.CENTER) match {
          case cable: PartCable => true
          case _ => false
        })
      case _ => false
    })
  }

}
