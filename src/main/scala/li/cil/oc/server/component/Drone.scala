package li.cil.oc.server.component

import li.cil.oc.api.Network
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.Visibility
import li.cil.oc.api.prefab
import li.cil.oc.common.entity

class Drone(val host: entity.Drone) extends prefab.ManagedEnvironment {
  override val node = Network.newNode(this, Visibility.Network).
    withComponent("drone").
    create()

  @Callback(doc = "function(dx:number, dy:number, dz:number) -- Change the target position by the specified offset.")
  def move(context: Context, args: Arguments): Array[AnyRef] = {
    val dx = args.checkDouble(0).toFloat
    val dy = args.checkDouble(1).toFloat
    val dz = args.checkDouble(2).toFloat
    host.targetX += dx
    host.targetY += dy
    host.targetZ += dz
    null
  }

  @Callback(doc = "function():number -- Get the current distance to the target position.")
  def getOffset(context: Context, args: Arguments): Array[AnyRef] =
    result(host.getDistance(host.targetX, host.targetY, host.targetZ))

  @Callback(doc = "function():number -- Get the current velocity.")
  def getVelocity(context: Context, args: Arguments): Array[AnyRef] =
    result(math.sqrt(host.motionX * host.motionX + host.motionY * host.motionY + host.motionZ * host.motionZ) * 20) // per second

  @Callback(doc = "function():number -- Get the currently set acceleration.")
  def getAcceleration(context: Context, args: Arguments): Array[AnyRef] = {
    result(host.targetAcceleration * 20) // per second
  }

  @Callback(doc = "function(value:number):number -- Try to set the acceleration to the specified value and return the new acceleration.")
  def setAcceleration(context: Context, args: Arguments): Array[AnyRef] = {
    host.targetAcceleration = (args.checkDouble(0) / 20.0).toFloat
    result(host.targetAcceleration * 20)
  }
}
