package li.cil.oc.common.block.traits

import com.google.common.base.Predicate
import net.minecraft.block.Block
import net.minecraft.block.properties.PropertyDirection
import net.minecraft.block.state.BlockState
import net.minecraft.block.state.IBlockState
import net.minecraft.util.EnumFacing
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

trait Rotatable extends Block {
  final lazy val Facing = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL.asInstanceOf[Predicate[EnumFacing]])

  protected def buildDefaultState() = getBlockState.getBaseState.withProperty(Facing, EnumFacing.NORTH)

  @SideOnly(Side.CLIENT)
  override def getStateForEntityRender(state: IBlockState) = getDefaultState.withProperty(Facing, EnumFacing.SOUTH)

  override def getStateFromMeta(meta: Int) = {
    val facing = EnumFacing.getFront(meta)
    if (facing.getAxis == EnumFacing.Axis.Y)
      getDefaultState.withProperty(Facing, EnumFacing.NORTH)
    else
      getDefaultState.withProperty(Facing, facing)
  }

  override def getMetaFromState(state: IBlockState) = state.getValue(Facing).asInstanceOf[EnumFacing].getIndex

  override def createBlockState() = new BlockState(this, Facing)
}
