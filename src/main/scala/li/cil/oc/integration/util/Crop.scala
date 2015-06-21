package li.cil.oc.integration.util

import li.cil.oc.util.BlockPosition
import net.minecraft.block.Block

import scala.collection.mutable

object Crop {


  val providers = mutable.Buffer.empty[CropProvider]

  def addProvider(provider: CropProvider): Unit = providers += provider

  def getProviderForBlock(block: Block): Option[CropProvider] = {
    providers.find(_.isValidFor(block))
  }

  trait CropProvider {
    def getInformation(pos: BlockPosition): Array[AnyRef]

    def isValidFor(block: Block): Boolean
  }


}
