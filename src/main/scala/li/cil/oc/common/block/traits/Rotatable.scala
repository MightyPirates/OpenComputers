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
  def getFacing(state: IBlockState) =
    if (state.getBlock == this)
      state.getValue(Rotatable.Facing)
    else
      EnumFacing.SOUTH

  def withFacing(state: IBlockState, facing: EnumFacing) =
    (if (state.getBlock == this) state else getDefaultState).
      withProperty(Rotatable.Facing, facing)

  override protected def createProperties(listed: mutable.ArrayBuffer[IProperty[_ <: Comparable[AnyRef]]], unlisted: mutable.ArrayBuffer[IUnlistedProperty[_ <: Comparable[AnyRef]]]): Unit = {
    super.createProperties(listed, unlisted)
    listed += Rotatable.Facing.asInstanceOf[IProperty[_ <: Comparable[AnyRef]]]
  }
}

object Rotatable {
  final val Facing = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL.asInstanceOf[Predicate[EnumFacing]])
}
