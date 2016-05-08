package li.cil.oc.client.renderer.block

import java.util
import java.util.Collections

import li.cil.oc.client.Textures
import net.minecraft.block.state.IBlockState
import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.client.renderer.block.model.IBakedModel
import net.minecraft.client.renderer.block.model.ItemOverrideList
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing
import net.minecraft.world.World

import scala.collection.convert.WrapAsJava.bufferAsJavaList
import scala.collection.mutable

object RobotModel extends SmartBlockModelBase {
  override def getOverrides: ItemOverrideList = ItemOverride

  override def getQuads(state: IBlockState, side: EnumFacing, rand: Long): util.List[BakedQuad] = {
    val faces = mutable.ArrayBuffer.empty[BakedQuad]

    faces += new BakedQuad(quad(top, top1, top2), tint, EnumFacing.NORTH, robotTexture, true, DefaultVertexFormats.ITEM)
    faces += new BakedQuad(quad(top, top2, top3), tint, EnumFacing.EAST, robotTexture, true, DefaultVertexFormats.ITEM)
    faces += new BakedQuad(quad(top, top3, top4), tint, EnumFacing.SOUTH, robotTexture, true, DefaultVertexFormats.ITEM)
    faces += new BakedQuad(quad(top, top4, top1), tint, EnumFacing.WEST, robotTexture, true, DefaultVertexFormats.ITEM)

    faces += new BakedQuad(quad(bottom, bottom1, bottom2), tint, EnumFacing.NORTH, robotTexture, true, DefaultVertexFormats.ITEM)
    faces += new BakedQuad(quad(bottom, bottom2, bottom3), tint, EnumFacing.EAST, robotTexture, true, DefaultVertexFormats.ITEM)
    faces += new BakedQuad(quad(bottom, bottom3, bottom4), tint, EnumFacing.SOUTH, robotTexture, true, DefaultVertexFormats.ITEM)
    faces += new BakedQuad(quad(bottom, bottom4, bottom1), tint, EnumFacing.WEST, robotTexture, true, DefaultVertexFormats.ITEM)

    bufferAsJavaList(faces)
  }

  private val size = 0.4f
  private val l = 0.5f - size
  private val h = 0.5f + size

  private val top = (0.5f, 1f, 0.5f, 0.25f, 0.25f)
  private val top1 = (l, 0.5f, h, 0f, 0f)
  private val top2 = (h, 0.5f, h, 0f, 0.5f)
  private val top3 = (h, 0.5f, l, 0.5f, 0.5f)
  private val top4 = (l, 0.5f, l, 0.5f, 0f)

  private val bottom = (0.5f, 0f, 0.5f, 0.75f, 0.25f)
  private val bottom1 = (l, 0.5f, l, 0.5f, 0.5f)
  private val bottom2 = (h, 0.5f, l, 0.5f, 0f)
  private val bottom3 = (h, 0.5f, h, 1f, 0f)
  private val bottom4 = (l, 0.5f, h, 1f, 0.5f)

  // I don't know why this is super-bright when using 0xFF888888 :/
  private val tint = 0xFF555555

  protected def robotTexture = Textures.getSprite(Textures.Item.Robot)

  private def interpolate(v0: (Float, Float, Float, Float, Float), v1: (Float, Float, Float, Float, Float)) =
    (v0._1 * 0.5f + v1._1 * 0.5f,
      v0._2 * 0.5f + v1._2 * 0.5f,
      v0._3 * 0.5f + v1._3 * 0.5f,
      v0._4 * 0.5f + v1._4 * 0.5f,
      v0._5 * 0.5f + v1._5 * 0.5f)

  private def quad(verts: (Float, Float, Float, Float, Float)*) = {
    val added = interpolate(verts.last, verts.head)
    (verts :+ added).flatMap {
      case ((x, y, z, u, v)) => rawData(
        (x - 0.5f) * 1.4f + 0.5f,
        (y - 0.5f) * 1.4f + 0.5f,
        (z - 0.5f) * 1.4f + 0.5f,
        EnumFacing.UP, robotTexture, robotTexture.getInterpolatedU(u * 16), robotTexture.getInterpolatedV(v * 16),
        White)
    }.toArray
  }

  object ItemOverride extends ItemOverrideList(Collections.emptyList()) {
    override def handleItemState(originalModel: IBakedModel, stack: ItemStack, world: World, entity: EntityLivingBase): IBakedModel = RobotModel
  }

}
