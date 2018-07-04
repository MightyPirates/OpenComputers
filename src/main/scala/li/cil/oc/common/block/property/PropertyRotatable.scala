package li.cil.oc.common.block.property

import com.google.common.base.Predicate
import com.google.common.base.Predicates
import net.minecraft.block.properties.PropertyDirection
import net.minecraft.util.EnumFacing

import scala.collection.convert.WrapAsJava._

object PropertyRotatable {
  final val Facing = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL.asInstanceOf[Predicate[EnumFacing]])
  final val Pitch = PropertyDirection.create("pitch", Predicates.in(Set(EnumFacing.DOWN, EnumFacing.UP, EnumFacing.NORTH)))
  final val Yaw = PropertyDirection.create("yaw", EnumFacing.Plane.HORIZONTAL.asInstanceOf[Predicate[EnumFacing]])
}
