package li.cil.oc.client.renderer.block

import li.cil.oc.Constants
import li.cil.oc.api
import li.cil.oc.client.Textures
import li.cil.oc.common.Tier
import li.cil.oc.common.block
import li.cil.oc.common.tileentity
import li.cil.oc.util.Color
import net.minecraft.block.state.IBlockState
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing
import net.minecraftforge.client.model.ISmartItemModel
import net.minecraftforge.common.property.IExtendedBlockState

import scala.collection.convert.WrapAsJava._

object ScreenModel extends SmartBlockModelBase with ISmartItemModel {
  override def handleBlockState(state: IBlockState) = state match {
    case extended: IExtendedBlockState => new BlockModel(extended)
    case _ => missingModel
  }

  override def handleItemState(stack: ItemStack) = new ItemModel(stack)

  class BlockModel(val state: IExtendedBlockState) extends SmartBlockModelBase {
    override def getFaceQuads(side: EnumFacing) =
      state.getValue(block.property.PropertyTile.Tile) match {
        case screen: tileentity.Screen =>
          val facing = screen.toLocal(side)

          val (x, y) = screen.localPosition
          var px = xy2part(x, screen.width - 1)
          var py = xy2part(y, screen.height - 1)
          if ((side == EnumFacing.DOWN || screen.facing == EnumFacing.DOWN) && side != screen.facing) {
            px = 2 - px
            py = 2 - py
          }
          val rotation =
            if (side == EnumFacing.UP) screen.yaw.getHorizontalIndex
            else if (side == EnumFacing.DOWN) -screen.yaw.getHorizontalIndex
            else 0

          def pitch = if (screen.pitch == EnumFacing.NORTH) 0 else 1
          val texture =
            if (screen.width == 1 && screen.height == 1) {
              if (facing == EnumFacing.SOUTH)
                Textures.Block.Screen.SingleFront(pitch)
              else
                Textures.Block.Screen.Single(side.getIndex)
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

          seqAsJavaList(Seq(bakeQuad(side, Textures.getSprite(texture), Some(screen.color), rotation)))
        case _ => super.getFaceQuads(side)
      }

    private def xy2part(value: Int, high: Int) = if (value == 0) 2 else if (value == high) 0 else 1
  }

  class ItemModel(val stack: ItemStack) extends SmartBlockModelBase {
    val color = api.Items.get(stack).name() match {
      case Constants.BlockName.ScreenTier2 => Color.byTier(Tier.Two)
      case Constants.BlockName.ScreenTier3 => Color.byTier(Tier.Three)
      case _ => Color.byTier(Tier.One)
    }

    override def getFaceQuads(side: EnumFacing) = {
      val result =
        if (side == EnumFacing.NORTH)
          Textures.Block.Screen.SingleFront(0)
        else
          Textures.Block.Screen.Single(side.ordinal())
      seqAsJavaList(Seq(bakeQuad(side, Textures.getSprite(result), Some(Color.rgbValues(color)), 0)))
    }
  }

}
