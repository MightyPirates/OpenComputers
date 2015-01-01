package li.cil.oc.client.renderer.block

import li.cil.oc.api
import li.cil.oc.client.Textures
import li.cil.oc.common.Tier
import li.cil.oc.common.block
import li.cil.oc.common.tileentity
import li.cil.oc.util.Color
import net.minecraft.block.state.IBlockState
import net.minecraft.client.renderer.block.model.BakedQuad
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
      state.getValue(block.Screen.Tile) match {
        case screen: tileentity.Screen =>
          val facing = screen.toLocal(side)

          val (x, y) = screen.localPosition
          val pitch = if (screen.pitch == EnumFacing.NORTH) 0 else 1
          var px = xy2part(x, screen.width - 1)
          var py = xy2part(y, screen.height - 1)
          var rotation = 0

          if (screen.pitch == EnumFacing.DOWN)
            py = 2 - py
          if (side == EnumFacing.UP) {
            rotation += screen.yaw.getHorizontalIndex
            py = 2 - py
          }
          else {
            if (side == EnumFacing.DOWN) {
              if (screen.yaw.getAxis == EnumFacing.Axis.X) {
                rotation += 1
                px = 2 - px
              }
              if (screen.yaw.getAxisDirection.getOffset < 0)
                py = 2 - py
            }
            else if (screen.yaw.getAxisDirection.getOffset > 0 && pitch == 1)
              py = 2 - py
            if (screen.yaw == EnumFacing.NORTH || screen.yaw == EnumFacing.EAST)
              px = 2 - px
          }

          val textures =
            if (screen.width == 1 && screen.height == 1) {
              val result = Textures.Block.Screen.Single.clone()
              if (facing == EnumFacing.SOUTH)
                result(3) = Textures.Block.Screen.SingleFront(pitch)
              result
            }
            else if (screen.width == 1) {
              val result = Textures.Block.Screen.Vertical(pitch)(py).clone()
              if (facing == EnumFacing.SOUTH)
                result(3) = Textures.Block.Screen.VerticalFront(pitch)(py)
              result
            }
            else if (screen.height == 1) {
              val result = Textures.Block.Screen.Horizontal(pitch)(px).clone()
              if (facing == EnumFacing.SOUTH)
                result(3) = Textures.Block.Screen.HorizontalFront(pitch)(px)
              result
            }
            else {
              val result = Textures.Block.Screen.Multi(pitch)(py)(px).clone()
              if (facing == EnumFacing.SOUTH)
                result(3) = Textures.Block.Screen.MultiFront(pitch)(py)(px)
              result
            }

          seqAsJavaList(Seq(new BakedQuad(makeQuad(side, Textures.Block.getSprite(textures(facing.ordinal())), screen.color, rotation), -1, side)))
        case _ => super.getFaceQuads(side)
      }

    private def xy2part(value: Int, high: Int) = if (value == 0) 2 else if (value == high) 0 else 1
  }

  class ItemModel(val stack: ItemStack) extends SmartBlockModelBase {
    val color = api.Items.get(stack).name() match {
      case "screen2" => Color.byTier(Tier.Two)
      case "screen3" => Color.byTier(Tier.Three)
      case _ => Color.byTier(Tier.One)
    }

    override def getFaceQuads(side: EnumFacing) = {
      val result =
        if (side == EnumFacing.NORTH)
          Textures.Block.Screen.SingleFront(0)
        else
          Textures.Block.Screen.Single(side.ordinal())
      seqAsJavaList(Seq(new BakedQuad(makeQuad(side, Textures.Block.getSprite(result), color, 0), -1, side)))
    }
  }

}
