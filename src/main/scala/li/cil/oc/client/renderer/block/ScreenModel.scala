package li.cil.oc.client.renderer.block

import java.util
import java.util.Collections

import li.cil.oc.Constants
import li.cil.oc.api
import li.cil.oc.client.Textures
import li.cil.oc.common.Tier
import li.cil.oc.common.block
import li.cil.oc.common.tileentity
import li.cil.oc.util.Color
import net.minecraft.block.BlockState
import net.minecraft.client.world.ClientWorld
import net.minecraft.client.renderer.model.BakedQuad
import net.minecraft.client.renderer.model.IBakedModel
import net.minecraft.client.renderer.model.ItemOverrideList
import net.minecraft.entity.LivingEntity
import net.minecraft.item.ItemStack
import net.minecraft.util.Direction
import net.minecraftforge.client.model.data.IModelData

import scala.collection.convert.WrapAsJava._

object ScreenModel extends SmartBlockModelBase {
  override def getOverrides: ItemOverrideList = ItemOverride

  override def getQuads(state: BlockState, side: Direction, rand: util.Random, data: IModelData): util.List[BakedQuad] = {
    val safeSide = if (side != null) side else Direction.SOUTH
    data match {
      case screen: tileentity.Screen =>
        val facing = screen.toLocal(safeSide)

        val (x, y) = screen.localPosition
        var px = xy2part(x, screen.width - 1)
        var py = xy2part(y, screen.height - 1)
        if ((safeSide == Direction.DOWN || screen.facing == Direction.DOWN) && safeSide != screen.facing) {
          px = 2 - px
          py = 2 - py
        }
        val rotation =
          if (safeSide == Direction.UP) screen.yaw.get2DDataValue
          else if (safeSide == Direction.DOWN) -screen.yaw.get2DDataValue
          else 0

        def pitch = if (screen.pitch == Direction.NORTH) 0 else 1
        val texture =
          if (screen.width == 1 && screen.height == 1) {
            if (facing == Direction.SOUTH)
              Textures.Block.Screen.SingleFront(pitch)
            else
              Textures.Block.Screen.Single(safeSide.get3DDataValue)
          }
          else if (screen.width == 1) {
            if (facing == Direction.SOUTH)
              Textures.Block.Screen.VerticalFront(pitch)(py)
            else
              Textures.Block.Screen.Vertical(pitch)(py)(facing.get3DDataValue)
          }
          else if (screen.height == 1) {
            if (facing == Direction.SOUTH)
              Textures.Block.Screen.HorizontalFront(pitch)(px)
            else
              Textures.Block.Screen.Horizontal(pitch)(px)(facing.get3DDataValue)
          }
          else {
            if (facing == Direction.SOUTH)
              Textures.Block.Screen.MultiFront(pitch)(py)(px)
            else
              Textures.Block.Screen.Multi(pitch)(py)(px)(facing.get3DDataValue)
          }

        seqAsJavaList(Seq(bakeQuad(safeSide, Textures.getSprite(texture), Some(screen.getColor), rotation)))
      case _ => super.getQuads(state, safeSide, rand)
    }
  }

  private def xy2part(value: Int, high: Int) = if (value == 0) 2 else if (value == high) 0 else 1

  class ItemModel(val stack: ItemStack) extends SmartBlockModelBase {
    val color = api.Items.get(stack).name() match {
      case Constants.BlockName.ScreenTier2 => Color.byTier(Tier.Two)
      case Constants.BlockName.ScreenTier3 => Color.byTier(Tier.Three)
      case _ => Color.byTier(Tier.One)
    }

    override def getQuads(state: BlockState, side: Direction, rand: util.Random): util.List[BakedQuad] = {
      val result =
        if (side == Direction.NORTH || side == null)
          Textures.Block.Screen.SingleFront(0)
        else
          Textures.Block.Screen.Single(side.ordinal())
      seqAsJavaList(Seq(bakeQuad(if (side != null) side else Direction.SOUTH, Textures.getSprite(result), Some(Color.rgbValues(color)), 0)))
    }
  }

  object ItemOverride extends ItemOverrideList {
    override def resolve(originalModel: IBakedModel, stack: ItemStack, world: ClientWorld, entity: LivingEntity): IBakedModel = new ItemModel(stack)
  }

}
