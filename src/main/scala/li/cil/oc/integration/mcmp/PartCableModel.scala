package li.cil.oc.integration.mcmp

import li.cil.oc.client.renderer.block.CableModel
import li.cil.oc.util.BlockPosition
import mcmultipart.multipart.MultipartHelper
import mcmultipart.multipart.PartSlot

object PartCableModel extends CableModel {
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
