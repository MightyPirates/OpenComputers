package li.cil.oc.common.block.traits

import com.google.common.base.Predicate
import net.minecraft.block.Block
import net.minecraft.block.properties.IProperty
import net.minecraft.block.properties.PropertyDirection
import net.minecraft.block.state.IBlockState
import net.minecraft.util.EnumFacing
import net.minecraftforge.common.property.IUnlistedProperty

import scala.collection.mutable

// Provides 4-way rotation for blocks using metadata to store the rotation.
trait Rotatable extends Block with Extended {
  def getFacing(state: IBlockState) = state.getValue(Rotatable.Facing).asInstanceOf[EnumFacing]

  def withFacing(state: IBlockState, facing: EnumFacing) = state.
    withProperty(Rotatable.Facing, facing)

  override protected def createProperties(listed: mutable.ArrayBuffer[IProperty], unlisted: mutable.ArrayBuffer[IUnlistedProperty[_]]): Unit = {
    super.createProperties(listed, unlisted)
    listed += Rotatable.Facing
  }
}

object Rotatable {
  final val Facing = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL.asInstanceOf[Predicate[EnumFacing]])
}
