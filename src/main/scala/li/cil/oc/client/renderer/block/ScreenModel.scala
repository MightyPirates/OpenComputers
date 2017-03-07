package li.cil.oc.client.renderer.block

import java.util
import java.util.Collections

import li.cil.oc.Constants
import li.cil.oc.api
import li.cil.oc.client.Textures
import li.cil.oc.common.Tier
import li.cil.oc.common.block
import li.cil.oc.common.tileentity
import li.cil.oc.util.DyeUtils
import net.minecraft.block.state.IBlockState
import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.client.renderer.block.model.IBakedModel
import net.minecraft.client.renderer.block.model.ItemOverrideList
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing
import net.minecraft.world.World
import net.minecraftforge.common.property.IExtendedBlockState

import scala.collection.convert.WrapAsJava._

object ScreenModel extends SmartBlockModelBase {
  override def getOverrides: ItemOverrideList = ItemOverride

  override def getQuads(state: IBlockState, side: EnumFacing, rand: Long): util.List[BakedQuad] = {
    val safeSide = if (side != null) side else EnumFacing.SOUTH
    state match {
      case extended: IExtendedBlockState =>
        extended.getValue(block.property.PropertyTile.Tile) match {
          case screen: tileentity.Screen =>
            val facing = screen.toLocal(safeSide)

            val (x, y) = screen.localPosition
            var px = xy2part(x, screen.width - 1)
            var py = xy2part(y, screen.height - 1)
            if ((safeSide == EnumFacing.DOWN || screen.facing == EnumFacing.DOWN) && safeSide != screen.facing) {
              px = 2 - px
              py = 2 - py
            }
            val rotation =
              if (safeSide == EnumFacing.UP) screen.yaw.getHorizontalIndex
              else if (safeSide == EnumFacing.DOWN) -screen.yaw.getHorizontalIndex
              else 0

            def pitch = if (screen.pitch == EnumFacing.NORTH) 0 else 1
            val texture =
              if (screen.width == 1 && screen.height == 1) {
                if (facing == EnumFacing.SOUTH)
                  Textures.Block.Screen.SingleFront(pitch)
                else
                  Textures.Block.Screen.Single(safeSide.getIndex)
              }
              else if (screen.width == 1) {
                if (facing == EnumFacing.SOUTH)
                  Textures.Block.Screen.VerticalFront(pitch)(py)
                else
                  Textures.Block.Screen.Vertical(pitch)(py)(facing.getIndex)
              }
              else if (screen.height == 1) {
                if (facing == EnumFacing.SOUTH)
                  Textures.Block.Screen.HorizontalFront(pitch)(px)
                else
                  Textures.Block.Screen.Horizontal(pitch)(px)(facing.getIndex)
              }
              else {
                if (facing == EnumFacing.SOUTH)
                  Textures.Block.Screen.MultiFront(pitch)(py)(px)
                else
                  Textures.Block.Screen.Multi(pitch)(py)(px)(facing.getIndex)
              }

            seqAsJavaList(Seq(bakeQuad(safeSide, Textures.getSprite(texture), Some(screen.getColor), rotation)))
          case _ => super.getQuads(state, safeSide, rand)
        }
      case _ => super.getQuads(state, safeSide, rand)
    }
  }

  private def xy2part(value: Int, high: Int) = if (value == 0) 2 else if (value == high) 0 else 1

  class ItemModel(val stack: ItemStack) extends SmartBlockModelBase {
    val color = api.Items.get(stack).name() match {
      case Constants.BlockName.ScreenTier2 => DyeUtils.byTier(Tier.Two)
      case Constants.BlockName.ScreenTier3 => DyeUtils.byTier(Tier.Three)
      case _ => DyeUtils.byTier(Tier.One)
    }

    override def getQuads(state: IBlockState, side: EnumFacing, rand: Long): util.List[BakedQuad] = {
      val result =
        if (side == EnumFacing.NORTH || side == null)
          Textures.Block.Screen.SingleFront(0)
        else
          Textures.Block.Screen.Single(side.ordinal())
      seqAsJavaList(Seq(bakeQuad(if (side != null) side else EnumFacing.SOUTH, Textures.getSprite(result), Some(DyeUtils.rgbValues(color)), 0)))
    }
  }

  object ItemOverride extends ItemOverrideList(Collections.emptyList()) {
    override def handleItemState(originalModel: IBakedModel, stack: ItemStack, world: World, entity: EntityLivingBase): IBakedModel = new ItemModel(stack)
  }

}
