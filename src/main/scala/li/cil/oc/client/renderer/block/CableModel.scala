package li.cil.oc.client.renderer.block

import li.cil.oc.client.Textures
import li.cil.oc.common.block
import li.cil.oc.common.block.Cable
import li.cil.oc.common.tileentity
import li.cil.oc.integration.Mods
import li.cil.oc.integration.mcmp.PartCableModel
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.Color
import li.cil.oc.util.ExtendedWorld._
import net.minecraft.block.state.IBlockState
import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.item.EnumDyeColor
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing
import net.minecraft.util.Vec3
import net.minecraftforge.client.model.ISmartItemModel
import net.minecraftforge.common.property.IExtendedBlockState

import scala.collection.convert.WrapAsJava._
import scala.collection.mutable

object CableModel extends SmartBlockModelBase with ISmartItemModel {
  final val ItemModel = new SmartBlockModelBase {
    override def getGeneralQuads = {
      val faces = mutable.ArrayBuffer.empty[BakedQuad]

      faces ++= bakeQuads(Middle, cableTexture, Some(Color.rgbValues(EnumDyeColor.SILVER)))
      faces ++= bakeQuads(Connected(0)._2, cableTexture, Some(Color.rgbValues(EnumDyeColor.SILVER)))
      faces ++= bakeQuads(Connected(1)._2, cableTexture, Some(Color.rgbValues(EnumDyeColor.SILVER)))
      faces ++= bakeQuads(Connected(0)._1, cableCapTexture, None)
      faces ++= bakeQuads(Connected(1)._1, cableCapTexture, None)

      bufferAsJavaList(faces)
    }
  }

  override def handleBlockState(state: IBlockState) = state match {
    case extended: IExtendedBlockState =>
      if (Mods.MCMultiPart.isAvailable) new PartCableModel.BlockModel(extended)
      else new BlockModel(extended)
    case _ => missingModel
  }

  override def handleItemState(stack: ItemStack) = ItemModel

  protected final val Middle = makeBox(new Vec3(6 / 16f, 6 / 16f, 6 / 16f), new Vec3(10 / 16f, 10 / 16f, 10 / 16f))

  // Per side, always plug + short cable + long cable (no plug).
  protected final val Connected = Array(
    (makeBox(new Vec3(5 / 16f, 0 / 16f, 5 / 16f), new Vec3(11 / 16f, 1 / 16f, 11 / 16f)),
      makeBox(new Vec3(6 / 16f, 1 / 16f, 6 / 16f), new Vec3(10 / 16f, 6 / 16f, 10 / 16f)),
      makeBox(new Vec3(6 / 16f, 0 / 16f, 6 / 16f), new Vec3(10 / 16f, 6 / 16f, 10 / 16f))),
    (makeBox(new Vec3(5 / 16f, 15 / 16f, 5 / 16f), new Vec3(11 / 16f, 16 / 16f, 11 / 16f)),
      makeBox(new Vec3(6 / 16f, 10 / 16f, 6 / 16f), new Vec3(10 / 16f, 15 / 16f, 10 / 16f)),
      makeBox(new Vec3(6 / 16f, 10 / 16f, 6 / 16f), new Vec3(10 / 16f, 16 / 16f, 10 / 16f))),
    (makeBox(new Vec3(5 / 16f, 5 / 16f, 0 / 16f), new Vec3(11 / 16f, 11 / 16f, 1 / 16f)),
      makeBox(new Vec3(6 / 16f, 6 / 16f, 1 / 16f), new Vec3(10 / 16f, 10 / 16f, 6 / 16f)),
      makeBox(new Vec3(6 / 16f, 6 / 16f, 0 / 16f), new Vec3(10 / 16f, 10 / 16f, 6 / 16f))),
    (makeBox(new Vec3(5 / 16f, 5 / 16f, 15 / 16f), new Vec3(11 / 16f, 11 / 16f, 16 / 16f)),
      makeBox(new Vec3(6 / 16f, 6 / 16f, 10 / 16f), new Vec3(10 / 16f, 10 / 16f, 15 / 16f)),
      makeBox(new Vec3(6 / 16f, 6 / 16f, 10 / 16f), new Vec3(10 / 16f, 10 / 16f, 16 / 16f))),
    (makeBox(new Vec3(0 / 16f, 5 / 16f, 5 / 16f), new Vec3(1 / 16f, 11 / 16f, 11 / 16f)),
      makeBox(new Vec3(1 / 16f, 6 / 16f, 6 / 16f), new Vec3(6 / 16f, 10 / 16f, 10 / 16f)),
      makeBox(new Vec3(0 / 16f, 6 / 16f, 6 / 16f), new Vec3(6 / 16f, 10 / 16f, 10 / 16f))),
    (makeBox(new Vec3(15 / 16f, 5 / 16f, 5 / 16f), new Vec3(16 / 16f, 11 / 16f, 11 / 16f)),
      makeBox(new Vec3(10 / 16f, 6 / 16f, 6 / 16f), new Vec3(15 / 16f, 10 / 16f, 10 / 16f)),
      makeBox(new Vec3(10 / 16f, 6 / 16f, 6 / 16f), new Vec3(16 / 16f, 10 / 16f, 10 / 16f)))
  )

  // Per side, cap only.
  protected final val Disconnected = Array(
    makeBox(new Vec3(6 / 16f, 5 / 16f, 6 / 16f), new Vec3(10 / 16f, 6 / 16f, 10 / 16f)),
    makeBox(new Vec3(6 / 16f, 10 / 16f, 6 / 16f), new Vec3(10 / 16f, 11 / 16f, 10 / 16f)),
    makeBox(new Vec3(6 / 16f, 6 / 16f, 5 / 16f), new Vec3(10 / 16f, 10 / 16f, 6 / 16f)),
    makeBox(new Vec3(6 / 16f, 6 / 16f, 10 / 16f), new Vec3(10 / 16f, 10 / 16f, 11 / 16f)),
    makeBox(new Vec3(5 / 16f, 6 / 16f, 6 / 16f), new Vec3(6 / 16f, 10 / 16f, 10 / 16f)),
    makeBox(new Vec3(10 / 16f, 6 / 16f, 6 / 16f), new Vec3(11 / 16f, 10 / 16f, 10 / 16f))
  )

  protected def cableTexture = Array.fill(6)(Textures.getSprite(Textures.Block.Cable))

  protected def cableCapTexture = Array.fill(6)(Textures.getSprite(Textures.Block.CableCap))

  class BlockModel(val state: IExtendedBlockState) extends SmartBlockModelBase {
    override def getGeneralQuads =
      state.getValue(block.property.PropertyTile.Tile) match {
        case t: tileentity.Cable =>
          val faces = mutable.ArrayBuffer.empty[BakedQuad]

          val color = Some(t.getColor)
          val mask = Cable.neighbors(t.world, t.getPos)
          faces ++= bakeQuads(Middle, cableTexture, color)
          for (side <- EnumFacing.values) {
            val connected = (mask & (1 << side.getIndex)) != 0
            val (plug, shortBody, longBody) = Connected(side.getIndex)
            if (connected) {
              if (isCable(t.position.offset(side))) {
                faces ++= bakeQuads(longBody, cableTexture, color)
              }
              else {
                faces ++= bakeQuads(shortBody, cableTexture, color)
                faces ++= bakeQuads(plug, cableCapTexture, None)
              }
            }
            else if (((1 << side.getOpposite.getIndex) & mask) == mask || mask == 0) {
              faces ++= bakeQuads(Disconnected(side.getIndex), cableCapTexture, None)
            }
          }

          bufferAsJavaList(faces)
        case _ => super.getGeneralQuads
      }

    protected def isCable(pos: BlockPosition) = {
      pos.world match {
        case Some(world) =>
          world.getTileEntity(pos).isInstanceOf[tileentity.Cable]
        case _ => false
      }
    }
  }

}
