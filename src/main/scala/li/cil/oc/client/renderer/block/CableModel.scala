package li.cil.oc.client.renderer.block

import java.util
import java.util.Collections

import li.cil.oc.client.Textures
import li.cil.oc.common.block
import li.cil.oc.common.tileentity
import li.cil.oc.integration.Mods
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.Color
import li.cil.oc.util.ExtendedWorld._
import li.cil.oc.util.ItemColorizer
import net.minecraft.block.state.IBlockState
import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.client.renderer.block.model.IBakedModel
import net.minecraft.client.renderer.block.model.ItemOverrideList
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.EnumDyeColor
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import net.minecraftforge.common.property.IExtendedBlockState

import scala.collection.convert.WrapAsJava._
import scala.collection.mutable

object CableModel extends CableModel

class CableModel extends SmartBlockModelBase {
  override def getOverrides: ItemOverrideList = ItemOverride

  override def getQuads(state: IBlockState, side: EnumFacing, rand: Long): util.List[BakedQuad] =
    state match {
      case extended: IExtendedBlockState =>
        extended.getValue(block.property.PropertyTile.Tile) match {
          case t: tileentity.Cable =>
            val faces = mutable.ArrayBuffer.empty[BakedQuad]

            val color = Some(t.getColor)
            val mask = block.Cable.neighbors(t.world, t.getPos)
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
          case _ => super.getQuads(state, side, rand)
        }
      case _ => super.getQuads(state, side, rand)
    }

  protected def isCable(pos: BlockPosition) = {
    pos.world match {
      case Some(world) =>
        world.getTileEntity(pos).isInstanceOf[tileentity.Cable]
      case _ => false
    }
  }

  protected final val Middle = makeBox(new Vec3d(6 / 16f, 6 / 16f, 6 / 16f), new Vec3d(10 / 16f, 10 / 16f, 10 / 16f))

  // Per side, always plug + short cable + long cable (no plug).
  protected final val Connected = Array(
    (makeBox(new Vec3d(5 / 16f, 0 / 16f, 5 / 16f), new Vec3d(11 / 16f, 1 / 16f, 11 / 16f)),
      makeBox(new Vec3d(6 / 16f, 1 / 16f, 6 / 16f), new Vec3d(10 / 16f, 6 / 16f, 10 / 16f)),
      makeBox(new Vec3d(6 / 16f, 0 / 16f, 6 / 16f), new Vec3d(10 / 16f, 6 / 16f, 10 / 16f))),
    (makeBox(new Vec3d(5 / 16f, 15 / 16f, 5 / 16f), new Vec3d(11 / 16f, 16 / 16f, 11 / 16f)),
      makeBox(new Vec3d(6 / 16f, 10 / 16f, 6 / 16f), new Vec3d(10 / 16f, 15 / 16f, 10 / 16f)),
      makeBox(new Vec3d(6 / 16f, 10 / 16f, 6 / 16f), new Vec3d(10 / 16f, 16 / 16f, 10 / 16f))),
    (makeBox(new Vec3d(5 / 16f, 5 / 16f, 0 / 16f), new Vec3d(11 / 16f, 11 / 16f, 1 / 16f)),
      makeBox(new Vec3d(6 / 16f, 6 / 16f, 1 / 16f), new Vec3d(10 / 16f, 10 / 16f, 6 / 16f)),
      makeBox(new Vec3d(6 / 16f, 6 / 16f, 0 / 16f), new Vec3d(10 / 16f, 10 / 16f, 6 / 16f))),
    (makeBox(new Vec3d(5 / 16f, 5 / 16f, 15 / 16f), new Vec3d(11 / 16f, 11 / 16f, 16 / 16f)),
      makeBox(new Vec3d(6 / 16f, 6 / 16f, 10 / 16f), new Vec3d(10 / 16f, 10 / 16f, 15 / 16f)),
      makeBox(new Vec3d(6 / 16f, 6 / 16f, 10 / 16f), new Vec3d(10 / 16f, 10 / 16f, 16 / 16f))),
    (makeBox(new Vec3d(0 / 16f, 5 / 16f, 5 / 16f), new Vec3d(1 / 16f, 11 / 16f, 11 / 16f)),
      makeBox(new Vec3d(1 / 16f, 6 / 16f, 6 / 16f), new Vec3d(6 / 16f, 10 / 16f, 10 / 16f)),
      makeBox(new Vec3d(0 / 16f, 6 / 16f, 6 / 16f), new Vec3d(6 / 16f, 10 / 16f, 10 / 16f))),
    (makeBox(new Vec3d(15 / 16f, 5 / 16f, 5 / 16f), new Vec3d(16 / 16f, 11 / 16f, 11 / 16f)),
      makeBox(new Vec3d(10 / 16f, 6 / 16f, 6 / 16f), new Vec3d(15 / 16f, 10 / 16f, 10 / 16f)),
      makeBox(new Vec3d(10 / 16f, 6 / 16f, 6 / 16f), new Vec3d(16 / 16f, 10 / 16f, 10 / 16f)))
  )

  // Per side, cap only.
  protected final val Disconnected = Array(
    makeBox(new Vec3d(6 / 16f, 5 / 16f, 6 / 16f), new Vec3d(10 / 16f, 6 / 16f, 10 / 16f)),
    makeBox(new Vec3d(6 / 16f, 10 / 16f, 6 / 16f), new Vec3d(10 / 16f, 11 / 16f, 10 / 16f)),
    makeBox(new Vec3d(6 / 16f, 6 / 16f, 5 / 16f), new Vec3d(10 / 16f, 10 / 16f, 6 / 16f)),
    makeBox(new Vec3d(6 / 16f, 6 / 16f, 10 / 16f), new Vec3d(10 / 16f, 10 / 16f, 11 / 16f)),
    makeBox(new Vec3d(5 / 16f, 6 / 16f, 6 / 16f), new Vec3d(6 / 16f, 10 / 16f, 10 / 16f)),
    makeBox(new Vec3d(10 / 16f, 6 / 16f, 6 / 16f), new Vec3d(11 / 16f, 10 / 16f, 10 / 16f))
  )

  protected def cableTexture = Array.fill(6)(Textures.getSprite(Textures.Block.Cable))

  protected def cableCapTexture = Array.fill(6)(Textures.getSprite(Textures.Block.CableCap))

  object ItemOverride extends ItemOverrideList(Collections.emptyList()) {
    class ItemModel(val stack: ItemStack) extends SmartBlockModelBase {
      override def getQuads(state: IBlockState, side: EnumFacing, rand: Long): util.List[BakedQuad] = {
        val faces = mutable.ArrayBuffer.empty[BakedQuad]

        val color = if (ItemColorizer.hasColor(stack)) ItemColorizer.getColor(stack) else Color.rgbValues(EnumDyeColor.SILVER)

        faces ++= bakeQuads(Middle, cableTexture, Some(color))
        faces ++= bakeQuads(Connected(0)._2, cableTexture, Some(color))
        faces ++= bakeQuads(Connected(1)._2, cableTexture, Some(color))
        faces ++= bakeQuads(Connected(0)._1, cableCapTexture, None)
        faces ++= bakeQuads(Connected(1)._1, cableCapTexture, None)

        bufferAsJavaList(faces)
      }
    }

    override def handleItemState(originalModel: IBakedModel, stack: ItemStack, world: World, entity: EntityLivingBase): IBakedModel = new ItemModel(stack)
  }

}
