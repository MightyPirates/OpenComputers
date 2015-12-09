package li.cil.oc.common.block.traits

import com.google.common.base.Predicate
import com.google.common.base.Predicates
import net.minecraft.block.Block
import net.minecraft.block.properties.IProperty
import net.minecraft.block.properties.PropertyDirection
import net.minecraft.block.state.IBlockState
import net.minecraft.util.EnumFacing
import net.minecraftforge.common.property.IUnlistedProperty

import scala.collection.convert.WrapAsJava._
import scala.collection.mutable

// Provides 2-axis rotation for blocks (pitch and yaw) using metadata to store the rotation.
trait OmniRotatable extends Block with Extended {
  def getPitch(state: IBlockState) =
    if (state.getBlock == this)
      state.getValue(OmniRotatable.Pitch)
    else
      EnumFacing.NORTH

  def getYaw(state: IBlockState) =
    if (state.getBlock == this)
      state.getValue(OmniRotatable.Yaw)
    else
      EnumFacing.SOUTH

  def withPitchAndYaw(state: IBlockState, pitch: EnumFacing, yaw: EnumFacing) =
    (if (state.getBlock == this) state else getDefaultState).
      withProperty(OmniRotatable.Pitch, pitch).
      withProperty(OmniRotatable.Yaw, yaw)


  override protected def createProperties(listed: mutable.ArrayBuffer[IProperty[_ <: Comparable[AnyRef]]], unlisted: mutable.ArrayBuffer[IUnlistedProperty[_ <: Comparable[AnyRef]]]): Unit = {
    super.createProperties(listed, unlisted)
    listed += OmniRotatable.Pitch.asInstanceOf[IProperty[_ <: Comparable[AnyRef]]]
    listed += OmniRotatable.Yaw.asInstanceOf[IProperty[_ <: Comparable[AnyRef]]]
  }
}

object OmniRotatable {
  final val Pitch = PropertyDirection.create("pitch", Predicates.in(Set(EnumFacing.DOWN, EnumFacing.UP, EnumFacing.NORTH)))
  final val Yaw = PropertyDirection.create("yaw", EnumFacing.Plane.HORIZONTAL.asInstanceOf[Predicate[EnumFacing]])
}
