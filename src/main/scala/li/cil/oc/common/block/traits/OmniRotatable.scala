package li.cil.oc.common.block.traits

import com.google.common.base.Predicate
import com.google.common.base.Predicates
import net.minecraft.block.Block
import net.minecraft.block.properties.PropertyDirection
import net.minecraft.block.state.BlockState
import net.minecraft.block.state.IBlockState
import net.minecraft.util.EnumFacing
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

trait OmniRotatable extends Block {
  final lazy val Facing = PropertyDirection.create("facing", Predicates.instanceOf(classOf[EnumFacing]))
  final lazy val Up = PropertyDirection.create("up", EnumFacing.Plane.HORIZONTAL.asInstanceOf[Predicate[EnumFacing]])

  protected def buildDefaultState() = getBlockState.getBaseState.withProperty(Facing, EnumFacing.NORTH).withProperty(Up, EnumFacing.NORTH)

  @SideOnly(Side.CLIENT)
  override def getStateForEntityRender(state: IBlockState) = getDefaultState.withProperty(Facing, EnumFacing.SOUTH)

  override def getStateFromMeta(meta: Int) = {
    val facing = EnumFacing.getFront(meta >>> 3)
    val up = EnumFacing.getHorizontal(meta & 7)
    if (up.getAxis == EnumFacing.Axis.Y)
      getDefaultState.withProperty(Facing, facing).withProperty(Up, EnumFacing.NORTH)
    else
      getDefaultState.withProperty(Facing, facing).withProperty(Up, up)
  }

  override def getMetaFromState(state: IBlockState) = {
    val facing = state.getValue(Facing).asInstanceOf[EnumFacing]
    val up = state.getValue(Up).asInstanceOf[EnumFacing]
    facing.getIndex << 3 | up.getHorizontalIndex
  }

  override def createBlockState() = new BlockState(this, Facing, Up)
}
