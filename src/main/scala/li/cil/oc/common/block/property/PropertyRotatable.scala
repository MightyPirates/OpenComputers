package li.cil.oc.common.block.property

import com.google.common.base.Predicate
import com.google.common.base.Predicates
import net.minecraft.state.DirectionProperty
import net.minecraft.util.Direction

import scala.collection.convert.ImplicitConversionsToJava._

object PropertyRotatable {
  final val Facing = DirectionProperty.create("facing", Direction.Plane.HORIZONTAL.asInstanceOf[Predicate[Direction]])
  final val Pitch = DirectionProperty.create("pitch", Predicates.in(Set(Direction.DOWN, Direction.UP, Direction.NORTH)))
  final val Yaw = DirectionProperty.create("yaw", Direction.Plane.HORIZONTAL.asInstanceOf[Predicate[Direction]])
}
