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
import net.minecraft.block.BlockState
import net.minecraft.client.renderer.model.BakedQuad
import net.minecraft.client.renderer.model.IBakedModel
import net.minecraft.client.renderer.model.ItemOverrideList
import net.minecraft.client.world.ClientWorld
import net.minecraft.entity.LivingEntity
import net.minecraft.item.DyeColor
import net.minecraft.item.ItemStack
import net.minecraft.util.Direction
import net.minecraft.util.math.vector.Vector3d
import net.minecraftforge.client.model.data.IModelData

import scala.collection.convert.WrapAsJava._
import scala.collection.mutable

object CableModel extends CableModel

class CableModel extends SmartBlockModelBase {
  override def getOverrides: ItemOverrideList = ItemOverride

  override def getQuads(state: BlockState, side: Direction, rand: util.Random, data: IModelData): util.List[BakedQuad] =
    data match {
      case cable: tileentity.Cable =>
        val world = cable.getLevel
        val neighbors = block.Cable.neighbors(world, cable.getBlockPos)
        val color = cable.getColor
        var isCableSide = 0
        for (side <- Direction.values) {
          if (world.getBlockEntity(cable.getBlockPos.relative(side)).isInstanceOf[tileentity.Cable]){
            isCableSide = block.Cable.mask(side, isCableSide)
          }
        }
        val faces = mutable.ArrayBuffer.empty[BakedQuad]

        faces ++= bakeQuads(Middle, cableTexture, color)
        for (side <- Direction.values) {
          val connected = (neighbors & (1 << side.get3DDataValue)) != 0
          val isCableOnSide = (isCableSide & (1 << side.get3DDataValue)) != 0
          val (plug, shortBody, longBody) = Connected(side.get3DDataValue)
          if (connected) {
            if (isCableOnSide) {
              faces ++= bakeQuads(longBody, cableTexture, color)
            }
            else {
              faces ++= bakeQuads(shortBody, cableTexture, color)
              faces ++= bakeQuads(plug, cableCapTexture, None)
            }
          }
          else if (((1 << side.getOpposite.get3DDataValue) & neighbors) == neighbors || neighbors == 0) {
            faces ++= bakeQuads(Disconnected(side.get3DDataValue), cableCapTexture, None)
          }
        }

        bufferAsJavaList(faces)
      case _ => super.getQuads(state, side, rand)
    }

  protected def isCable(pos: BlockPosition) = {
    pos.world match {
      case Some(world) =>
        world.getBlockEntity(pos).isInstanceOf[tileentity.Cable]
      case _ => false
    }
  }

  protected final val Middle = makeBox(new Vector3d(6 / 16f, 6 / 16f, 6 / 16f), new Vector3d(10 / 16f, 10 / 16f, 10 / 16f))

  // Per side, always plug + short cable + long cable (no plug).
  protected final val Connected = Array(
    (makeBox(new Vector3d(5 / 16f, 0 / 16f, 5 / 16f), new Vector3d(11 / 16f, 1 / 16f, 11 / 16f)),
      makeBox(new Vector3d(6 / 16f, 1 / 16f, 6 / 16f), new Vector3d(10 / 16f, 6 / 16f, 10 / 16f)),
      makeBox(new Vector3d(6 / 16f, 0 / 16f, 6 / 16f), new Vector3d(10 / 16f, 6 / 16f, 10 / 16f))),
    (makeBox(new Vector3d(5 / 16f, 15 / 16f, 5 / 16f), new Vector3d(11 / 16f, 16 / 16f, 11 / 16f)),
      makeBox(new Vector3d(6 / 16f, 10 / 16f, 6 / 16f), new Vector3d(10 / 16f, 15 / 16f, 10 / 16f)),
      makeBox(new Vector3d(6 / 16f, 10 / 16f, 6 / 16f), new Vector3d(10 / 16f, 16 / 16f, 10 / 16f))),
    (makeBox(new Vector3d(5 / 16f, 5 / 16f, 0 / 16f), new Vector3d(11 / 16f, 11 / 16f, 1 / 16f)),
      makeBox(new Vector3d(6 / 16f, 6 / 16f, 1 / 16f), new Vector3d(10 / 16f, 10 / 16f, 6 / 16f)),
      makeBox(new Vector3d(6 / 16f, 6 / 16f, 0 / 16f), new Vector3d(10 / 16f, 10 / 16f, 6 / 16f))),
    (makeBox(new Vector3d(5 / 16f, 5 / 16f, 15 / 16f), new Vector3d(11 / 16f, 11 / 16f, 16 / 16f)),
      makeBox(new Vector3d(6 / 16f, 6 / 16f, 10 / 16f), new Vector3d(10 / 16f, 10 / 16f, 15 / 16f)),
      makeBox(new Vector3d(6 / 16f, 6 / 16f, 10 / 16f), new Vector3d(10 / 16f, 10 / 16f, 16 / 16f))),
    (makeBox(new Vector3d(0 / 16f, 5 / 16f, 5 / 16f), new Vector3d(1 / 16f, 11 / 16f, 11 / 16f)),
      makeBox(new Vector3d(1 / 16f, 6 / 16f, 6 / 16f), new Vector3d(6 / 16f, 10 / 16f, 10 / 16f)),
      makeBox(new Vector3d(0 / 16f, 6 / 16f, 6 / 16f), new Vector3d(6 / 16f, 10 / 16f, 10 / 16f))),
    (makeBox(new Vector3d(15 / 16f, 5 / 16f, 5 / 16f), new Vector3d(16 / 16f, 11 / 16f, 11 / 16f)),
      makeBox(new Vector3d(10 / 16f, 6 / 16f, 6 / 16f), new Vector3d(15 / 16f, 10 / 16f, 10 / 16f)),
      makeBox(new Vector3d(10 / 16f, 6 / 16f, 6 / 16f), new Vector3d(16 / 16f, 10 / 16f, 10 / 16f)))
  )

  // Per side, cap only.
  protected final val Disconnected = Array(
    makeBox(new Vector3d(6 / 16f, 5 / 16f, 6 / 16f), new Vector3d(10 / 16f, 6 / 16f, 10 / 16f)),
    makeBox(new Vector3d(6 / 16f, 10 / 16f, 6 / 16f), new Vector3d(10 / 16f, 11 / 16f, 10 / 16f)),
    makeBox(new Vector3d(6 / 16f, 6 / 16f, 5 / 16f), new Vector3d(10 / 16f, 10 / 16f, 6 / 16f)),
    makeBox(new Vector3d(6 / 16f, 6 / 16f, 10 / 16f), new Vector3d(10 / 16f, 10 / 16f, 11 / 16f)),
    makeBox(new Vector3d(5 / 16f, 6 / 16f, 6 / 16f), new Vector3d(6 / 16f, 10 / 16f, 10 / 16f)),
    makeBox(new Vector3d(10 / 16f, 6 / 16f, 6 / 16f), new Vector3d(11 / 16f, 10 / 16f, 10 / 16f))
  )

  protected def cableTexture = Array.fill(6)(Textures.getSprite(Textures.Block.Cable))

  protected def cableCapTexture = Array.fill(6)(Textures.getSprite(Textures.Block.CableCap))

  object ItemOverride extends ItemOverrideList {
    class ItemModel(val stack: ItemStack) extends SmartBlockModelBase {
      override def getQuads(state: BlockState, side: Direction, rand: util.Random): util.List[BakedQuad] = {
        val faces = mutable.ArrayBuffer.empty[BakedQuad]

        val color = if (ItemColorizer.hasColor(stack)) ItemColorizer.getColor(stack) else Color.rgbValues(DyeColor.LIGHT_GRAY)

        faces ++= bakeQuads(Middle, cableTexture, Some(color))
        faces ++= bakeQuads(Connected(0)._2, cableTexture, Some(color))
        faces ++= bakeQuads(Connected(1)._2, cableTexture, Some(color))
        faces ++= bakeQuads(Connected(0)._1, cableCapTexture, None)
        faces ++= bakeQuads(Connected(1)._1, cableCapTexture, None)

        bufferAsJavaList(faces)
      }
    }

    override def resolve(originalModel: IBakedModel, stack: ItemStack, world: ClientWorld, entity: LivingEntity): IBakedModel = new ItemModel(stack)
  }

}
