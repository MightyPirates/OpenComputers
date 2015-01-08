package li.cil.oc.client.renderer.block

import li.cil.oc.client.Textures
import li.cil.oc.common.block
import li.cil.oc.common.tileentity
import net.minecraft.block.state.IBlockState
import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing
import net.minecraftforge.client.model.ISmartItemModel
import net.minecraftforge.common.property.IExtendedBlockState

import scala.collection.convert.WrapAsJava._
import scala.collection.mutable

object CableModel extends SmartBlockModelBase with ISmartItemModel {
  override def handleBlockState(state: IBlockState) = state match {
    case extended: IExtendedBlockState => new BlockModel(extended)
    case _ => missingModel
  }

  override def handleItemState(stack: ItemStack) = new ItemModel(stack)

  val verticesMid = Array(-3/16f, -3/16f, 3/16f, 3/16f)
  val verticesSideUnconnected = Array(
    Array(-4/16f, -3/16f, -3/16f, 3/16f),
    Array(-3/16f, -4/16f, 3/16f, -3/16f),
    Array(3/16f, -3/16f, 4/16f, 3/16f),
    Array(-3/16f, 3/16f, 3/16f, 4/16f)
  )
  val verticesSideConnected = Array(
    Array(-1f, -3/16f, -3/16f, 3/16f),
    Array(-3/16f, -1f, 3/16f, -3/16f),
    Array(3/16f, -3/16f, 1f, 3/16f),
    Array(-3/16f, 3/16f, 3/16f, 1f)
  )

  def cableTexture = Textures.Block.getSprite(Textures.Block.CableCap)

  class BlockModel(val state: IExtendedBlockState) extends SmartBlockModelBase {
    override def getFaceQuads(side: EnumFacing) =
      state.getValue(block.Cable.Tile) match {
        case cable: tileentity.Cable =>
          val faces = mutable.ArrayBuffer.empty[BakedQuad]

          makeQuad(???, side, cableTexture, 0, None)

          bufferAsJavaList(faces)
          super.getFaceQuads(side)
        case _ => super.getFaceQuads(side)
      }
  }

  class ItemModel(val stack: ItemStack) extends SmartBlockModelBase {
    override def getFaceQuads(side: EnumFacing) = {
      seqAsJavaList(Seq(new BakedQuad(makeQuad(side, cableTexture, 0, None), -1, side)))
    }
  }

}
