package li.cil.oc.server.component

import li.cil.oc.api.Network
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network._
import li.cil.oc.api.prefab
import li.cil.oc.common.tileentity.traits.BundledRedstoneAware
import li.cil.oc.common.tileentity.traits.RedstoneAware
import li.cil.oc.server.component
import net.minecraft.util.EnumFacing

class Redstone[+Owner <: RedstoneAware](val owner: Owner) extends prefab.ManagedEnvironment {
  override val node = Network.newNode(this, Visibility.Network).
    withComponent("redstone", Visibility.Neighbors).
    create()

  // ----------------------------------------------------------------------- //

  @Callback(direct = true, doc = """function(side:number):number -- Get the redstone input on the specified side.""")
  def getInput(context: Context, args: Arguments): Array[AnyRef] = {
    val side = checkSide(args, 0)
    result(owner.input(side))
  }

  @Callback(direct = true, doc = """function(side:number):number -- Get the redstone output on the specified side.""")
  def getOutput(context: Context, args: Arguments): Array[AnyRef] = {
    val side = checkSide(args, 0)
    result(owner.output(side))
  }

  @Callback(doc = """function(side:number, value:number):number -- Set the redstone output on the specified side.""")
  def setOutput(context: Context, args: Arguments): Array[AnyRef] = {
    val side = checkSide(args, 0)
    val value = args.checkInteger(1)
    owner.output(side, value)
    context.pause(0.1)
    result(owner.output(side))
  }

  // ----------------------------------------------------------------------- //

  protected def checkSide(args: Arguments, index: Int) = {
    val side = args.checkInteger(index)
    if (side < 0 || side > 5)
      throw new IllegalArgumentException("invalid side")
    owner.toGlobal(EnumFacing.getFront(side))
  }
}

object Redstone {

  class Simple(redstone: RedstoneAware) extends component.Redstone[RedstoneAware](redstone)

  class Bundled(redstone: BundledRedstoneAware) extends component.Redstone[BundledRedstoneAware](redstone) with component.RedstoneBundled

  class Wireless(redstone: RedstoneAware) extends Simple(redstone) with component.RedstoneWireless

  class BundledWireless(redstone: BundledRedstoneAware) extends Bundled(redstone) with component.RedstoneWireless

}
