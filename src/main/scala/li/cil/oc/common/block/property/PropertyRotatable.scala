package li.cil.oc.common.block.property

import java.util.function.Predicate

import net.minecraft.state.DirectionProperty
import net.minecraft.util.Direction

import scala.collection.convert.ImplicitConversionsToJava._

object PropertyRotatable {
  final val Facing = DirectionProperty.create("facing", Direction.Plane.HORIZONTAL)
  final val Pitch = DirectionProperty.create("pitch", d => d.getAxis == Direction.Axis.Y || d == Direction.NORTH)
  final val Yaw = DirectionProperty.create("yaw", Direction.Plane.HORIZONTAL)
}
